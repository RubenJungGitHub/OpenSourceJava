package contain.opensource.ils.bs.sppoller.classes.messagequeuehandling;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.Hashtable;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
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
                jmsTemplate,
                activeMQProps.getMigrationHub().getSharepointQueue().toString(),
                activeMQProps.getMigrationHub());
    }

    @Override
    public void StartPoll(QueueBrowser browser, Session session, Queue queue) {
        try {
            String timestamp = ZonedDateTime.now(ZoneId.of("Europe/Amsterdam")).toLocalDateTime().format(formatter);
            String feedback = timestamp + " -> New SHAREPOINT poll loop on broker : " + this.currentSource.getBrokerUrl()
                    + " on queue " + this.queuetopoll + ". Interval : " + PollInterval + " seconds";
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_GREEN
                    + feedback
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
                                    logalfrescoiodeletedendpoint(item, "DeletedFromPlatform");
                                } else {
                                    try {
                                        Hashtable<String, String> migrateinfo = processharepointitem(message, item,
                                                deltaLink);
                                        // Check 'platformto' in plaats van 'containerto'
                                        // To do source <> destination
                                        if (migrateinfo.get("platformto") != null
                                                && !migrateinfo.get("platformto").equals("<NO MOVE>")) {
                                            SendMigrationMessage(item.getWebUrl(),
                                                    item.getFields().get("id").toString(), deltaLink,
                                                    AlfrescoConstants.ContainPlatforms.SPO.name(), migrateinfo);
                                        }
                                    } catch (Exception e) {
                                        System.err.println("Fout bij aanroepen Receiver: " + e.getMessage());
                                    }
                                }
                            }
                            consumeMessageById(msg.getJMSMessageID());
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

    private Hashtable<String, String> processharepointitem(SharepointQueMessage message, SharepointQueMessage.Item item,
            String deltaLink) {
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
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Fout bij aanroepen Receiver: " + e.getMessage());
            return null;
        }
    }

    private boolean logalfrescoiodeletedendpoint(SharepointQueMessage.Item item, String secondpath) {
        RestTemplate restTemplate = new RestTemplate();
        String url = ilsproperties.getlogiodeletedfromplatformendpoint();
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("platform", AlfrescoConstants.ContainPlatforms.SPO.name())
                .queryParam("id", item.getId())
                .queryParam("filename", "<Unknown>")
                .queryParam("deletedby", "<Unknown>")
                .queryParam("secondpath", secondpath);

        try {
            // 1. Create headers and set Content-Type to JSON
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 2. Create an HttpEntity with a null body but including the headers
            HttpEntity<String> entity = new HttpEntity<>(null, headers);

            // 3. Use the entity in the postForEntity call
            ResponseEntity<Boolean> response = restTemplate.postForEntity(
                    builder.toUriString(),
                    entity,
                    Boolean.class);

            System.out.println("Endpoint aangeroepen. Status: " + response.getStatusCode());

            // Return the actual body from the response
            return response.getBody() != null && response.getBody();

        } catch (Exception ex) {
            System.err.println("Fout bij aanroepen Alfresco endpoint: " + ex.getMessage());
            return false;
        }
    }
}