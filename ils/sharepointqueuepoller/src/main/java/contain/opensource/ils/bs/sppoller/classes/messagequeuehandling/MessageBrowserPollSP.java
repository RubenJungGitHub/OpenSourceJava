package contain.opensource.ils.bs.sppoller.classes.messagequeuehandling;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.Hashtable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import contain.opensource.shared.classes.MessageBrowserPollParent;
import contain.opensource.shared.classes.SharepointQueMessage;
import contain.opensource.shared.configurationproperties.ActiveMQProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.shared.constants.AlfrescoConstants;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

@Component
public class MessageBrowserPollSP extends MessageBrowserPollParent {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    @Autowired
    public MessageBrowserPollSP(ActiveMQProperties activeMQProps,
            AlfrescoProperties alfrescoProps,
            ILSRestProperties ilsproperties,
            ObjectMapper objectMapper,
            JmsTemplate jmsTemplate) {
        super(
                activeMQProps,
                alfrescoProps,
                ilsproperties,
                objectMapper,
                jmsTemplate);
    }

    @Override
    public void StartPoll(QueueBrowser browser, Session session, Queue queue) {
        try {
            timestamp = ZonedDateTime.now(ZoneId.of("Europe/Amsterdam")).toLocalDateTime().format(formatter);
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_GREEN
                    + timestamp + " -> New SHAREPOINT poll loop on broker : " + activeMQProps.getBrokerUrl()
                    + " on queue " + activeMQProps.getSharepointQueue() + ". Interval : " + PollInterval + " seconds"
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
            String json = "";
            int count = 0;
            ObjectMapper mapper = new ObjectMapper();
            Enumeration<?> messages = browser.getEnumeration();
            while (messages.hasMoreElements()) {
                Message msg = (Message) messages.nextElement();
                count++;
                System.out.println("Processing " + msg + " message # " + count + " from queue");
                try {
                    if (msg instanceof TextMessage) {
                        json = ((TextMessage) msg).getText();
                        // TextMessage text = (TextMessage) msg;
                        try {
                            System.out.println("RAW JSON: " + json);
                            SharepointQueMessage message = mapper.readValue(json, SharepointQueMessage.class);

                            for (SharepointQueMessage.Item item : message.getItems()) {
                                System.out.println("Item ID: " + item.getId());
                                if (item.getParentReference() != null) {
                                    System.out.println("Site ID: " + item.getParentReference().getSiteId());
                                }
                                if (item.getFields() != null) {
                                    System.out.println("Fields: " + item.getFields());
                                }
                                String deltaLink = message.getDeltaLink().split("\\?")[0];
                                if (item.getDeleted() != null) {

                                    String action = "IO  " + item.getId() + " deleted from platform";
                                /*     IOLog.log(
                                            "DeletedFromPlatform",
                                            item.getId(),
                                            "DeletedFromPlatform",
                                            action,
                                            AlfrescoConstants.ContainPlatforms.SPO.toString(),
                                            AlfrescoConstants.ContainPlatforms.SPO.toString(),
                                            "DeletedFromPlatform",
                                            "DeletedFromPlatform",
                                            deltaLink,
                                            AlfrescoConstants.eActionPerformed.IODELETED,
                                            "<Unknown>",
                                            "DeletedFromPlatform",
                                            "DeletedFromPlatform",
                                            "DeletedFromPlatform");
                                            */
                                } else {

                                    // 1. Maak een RestTemplate aan (of @Autowired deze)
                                    RestTemplate restTemplate = new RestTemplate();

                                    // 2. De URL van je nieuwe endpoint (haal dit idealiter uit ILSRestProperties)
                                    String url = ilsproperties.getprocessspitemsendpoint();

                                    // 3. De parameters (als je ze als Query Params houdt zoals in je huidige
                                    // skeleton)
                                    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                                            .queryParam("ItemWebUrl", item.getWebUrl())
                                            .queryParam("ListItemID", item.getId())
                                            .queryParam("resourceValue", deltaLink);

                                    // 4. Verstuur het bericht (de 'message' is je SharepointQueMessage)
                                    try {
                                        ResponseEntity<Hashtable> response = restTemplate.postForEntity(
                                                builder.toUriString(),
                                                message,
                                                Hashtable.class);
                                        System.out.println("Receiver antwoordde met: " + response.getStatusCode());
                                        Hashtable<String, String> migrateinfo = response.getBody();
                                        // Check 'platformto' in plaats van 'containerto'
                                        if (migrateinfo.get("platformto") != null
                                                && !migrateinfo.get("platformto").equals("<NO MOVE>")) {
                                            SendMigrationMessage(item, deltaLink,
                                                    AlfrescoConstants.ContainPlatforms.SPO.name(), migrateinfo);
                                        }
                                    } catch (Exception e) {
                                        System.err.println("Fout bij aanroepen Receiver: " + e.getMessage());
                                    }
                                }
                            }
                            consumeMessageById(msg.getJMSMessageID(), activeMQProps.getSharepointQueue());
                        } catch (JMSException processingError) {
                            System.err.println("Error while processing message" + processingError);
                            processingError.printStackTrace();
                        }

                    }
                } catch (JMSException e) {
                    System.err.println("Error polling the queue:");
                    e.printStackTrace();
                }

            }
        } catch (Exception e) {
            System.err.println("Error in StartPoll:");
            e.printStackTrace();
        }
    }

}