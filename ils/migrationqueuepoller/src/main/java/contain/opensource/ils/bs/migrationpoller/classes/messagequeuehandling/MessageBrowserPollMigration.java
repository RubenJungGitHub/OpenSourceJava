package contain.opensource.ils.bs.migrationpoller.classes.messagequeuehandling;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import contain.opensource.shared.classes.MessageBrowserPollParentMigration;
import contain.opensource.shared.classes.MigrationQueueMessage;
import contain.opensource.shared.configurationproperties.ActiveMQProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.shared.constants.AlfrescoConstants;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

@Component
public class MessageBrowserPollMigration extends MessageBrowserPollParentMigration {

    public MessageBrowserPollMigration(
            ActiveMQProperties activeMQProps,
            AlfrescoProperties alfrescoProps,
            ILSRestProperties ilsproperties,
            ObjectMapper objectMapper,
            JmsTemplate jmsTemplate) {
        super(
                activeMQProps,
                alfrescoProps,
                ilsproperties,
                objectMapper,
                jmsTemplate,
                activeMQProps.getMigrationHub().getMigrationQueue().toString(),
                activeMQProps.getMigrationHub());
    }

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    @Override
    public void StartPoll(QueueBrowser browser, Session session, Queue queue) {

        timestamp = ZonedDateTime.now(ZoneId.of("Europe/Amsterdam")).toLocalDateTime().format(formatter);
        String feedback = timestamp + " -> New MIGRATION poll loop on broker : " + this.currentSource.getBrokerUrl()
                + " on queue " + this.queuetopoll + ". Interval : " + PollInterval + " seconds";
        System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_BRIGHT_CYAN
                + feedback
                + contain.opensource.shared.constants.AlfrescoConstants.RESET);
        try {
            String json = "";
            int count = 0;
            ObjectMapper mapper = new ObjectMapper();
            Enumeration<?> messages = browser.getEnumeration();

            while (messages.hasMoreElements()) {
                count++;
                Message msg = (Message) messages.nextElement();
                System.out.println(contain.opensource.shared.constants.AlfrescoConstants.YELLOW
                        + timestamp + " -> Processing  message # " + count + " " + msg + "from queue"
                        + contain.opensource.shared.constants.AlfrescoConstants.RESET);
                if (msg instanceof TextMessage) {
                    // Process migration
                    json = ((TextMessage) msg).getText();
                    MigrationQueueMessage queueMessage = mapper.readValue(json, MigrationQueueMessage.class);
                    String endpoint = ilsproperties.getrelocateendpoint();
                    System.out.println("Relocate endpoint: " + endpoint);
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setBasicAuth(
                            AlfrescoConstants.username,
                            AlfrescoConstants.password,
                            StandardCharsets.UTF_8);
                    RestTemplate restTemplate = new RestTemplate();
                    // HttpEntity<RelocateInformationObject> entitymove = new HttpEntity<>(ROobject,
                    // headers);
                    int status = 0;
                    try {
                        // This must be checked if connection is broken a restart is required
                        ResponseEntity<String> response = restTemplate.postForEntity(endpoint, queueMessage,
                                String.class);
                        System.out.println("Status: " + response.getStatusCodeValue());
                        System.out.println("Body: " + response.getBody());
                        status = response.getStatusCode().value();
                        // For now hardcoded but this should depend on exceptions, persistance ansd
                        // transactions
                    } catch (Exception e) {
                        System.err.println("Error in StartPoll:");
                        e.printStackTrace();
                    }

                    if (status != 200) {
                        throw new IOException("HTTP error " + status);
                    } else {
                       // consumeMessageById(msg.getJMSMessageID());
                    }
                }
            }
        } catch (

        Exception e) {
            System.err.println("Error in StartPoll:");
            e.printStackTrace();
        }
        System.out.println("Message process completed");
    }
}