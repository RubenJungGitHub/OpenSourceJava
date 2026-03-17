package contain.opensource.ils.bs.migrationpoller.classes.messagequeuehandling;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;

import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import contain.opensource.ils.bs.receiver.classes.migration.MessageBrowserPollParentMigration;
import contain.opensource.ils.bs.receiver.classes.migration.MigrationQueueMessage;
import contain.opensource.shared.configurationproperties.ActiveMQProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.shared.constants.AlfrescoConstants;

@Component
public class MessageBrowserPollMigration extends MessageBrowserPollParentMigration {

    public MessageBrowserPollMigration(
            ActiveMQProperties activeMQProps,
            AlfrescoProperties alfrescoProps,
            ILSRestProperties ilsProperties,
            ObjectMapper objectMapper,
            JmsTemplate jmsTemplate) {
        super(
                activeMQProps,
                alfrescoProps,
                ilsProperties,
                objectMapper,
                jmsTemplate);

    }

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    @Override
    public void StartPoll(QueueBrowser browser, Session session, Queue queue) {
        try {
            timestamp = ZonedDateTime.now(ZoneId.of("Europe/Amsterdam")).toLocalDateTime().format(formatter);
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_BRIGHT_CYAN
                    + timestamp + " -> New MIGRATION poll loop on broker : " + browser
                    + " on queue " + queue + ". Interval : " + PollInterval + " seconds"
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
                        String endpoint = ILSProperties.getRelocateendpoint();
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
                        ResponseEntity<String> response = restTemplate.postForEntity(endpoint, queueMessage, String.class);
                        System.out.println("Status: " + response.getStatusCodeValue());
                        System.out.println("Body: " + response.getBody());
                        int status = response.getStatusCode().value();
                        if (status != 200) {
                            throw new IOException("HTTP error " + status);
                        }
                    }
                    consumeMessageById(msg.getJMSMessageID(), activeMQProps.getMigrationqueue());
                }
            } catch (Exception e) {
                System.err.println("Error in StartPoll:");
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Error in StartPoll:");
            e.printStackTrace();
        }
        System.out.println("Message process completed");
    }
}