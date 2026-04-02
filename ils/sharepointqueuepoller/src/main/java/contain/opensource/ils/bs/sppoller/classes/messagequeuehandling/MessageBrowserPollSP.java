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
            String feedback = timestamp + " -> New SHAREPOINT poll loop on broker : "
                    + this.currentSource.getBrokerUrl()
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
                                logSharepointItemdeletedendpoint(message);
                            } else {
                                Hashtable<String, String> migrateinfo = processharepointitem(message, item,
                                        deltaLink);
                                // Check 'platformto' in plaats van 'containerto'
                                // To do source <> destination
                                if (migrateinfo != null) {
                                    if (migrateinfo.get("platformto") != null
                                            && !migrateinfo.get("platformto").equals("<NO MOVE>")) {
                                        SendMigrationMessage(item.getWebUrl(),
                                                item.getFields().get("id").toString(), deltaLink,
                                                AlfrescoConstants.ContainPlatforms.SPO.name(), migrateinfo);
                                    }
                                }
                            }
                        }
                    }
                    consumeMessageById(msg.getJMSMessageID());
                } catch (JMSException e) {
                    System.out.println(contain.opensource.shared.constants.AlfrescoConstants.RED
                            + "Error in sharepoint poller : " + e.getMessage()
                            + contain.opensource.shared.constants.AlfrescoConstants.RESET);
                    // e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.RED
                    + "Fout bij aanroepen processharepointitem in receiver: " +
                    e.getMessage()
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
            // e.printStackTrace();
        }
    }

    private Hashtable<String, String> processharepointitem(SharepointQueMessage message, SharepointQueMessage.Item item,
            String deltaLink) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        try {
            // 2. De URL van je nieuwe endpoint (haal dit idealiter uit ILSRestProperties)
            String url = ilsproperties.getprocessspitemsendpoint();
            // 3. De parameters (als je ze als Query Params houdt zoals in je huidige
            // skeleton)
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("ItemWebUrl", item.getWebUrl())
                    .queryParam("ListItemID", item.getId())
                    .queryParam("resourceValue", deltaLink);

            // 4. Verstuur het bericht (de 'message' is je SharepointQueMessage)

            ResponseEntity<Hashtable> response = restTemplate.postForEntity(
                    builder.toUriString(),
                    message,
                    Hashtable.class);
            return response.getBody();
        } catch (org.springframework.web.client.HttpStatusCodeException e) {

            // Now this will work perfectly:
            int code = e.getStatusCode().value();

            if (code == 404) {
                // Perform your "Different Action" for the missing SharePoint item
                System.out.println("Item not found (404). Handling accordingly.");
            }
        } catch (Exception e) {
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.RED
                    + "Fout bij aanroepen processharepointitem in receiver: " +
                    e.getMessage()
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
            // This catches everything else that isn't an HTTP error (like a Timeout)
            throw e;
        }
        return null;
    }

    private boolean logSharepointItemdeletedendpoint(SharepointQueMessage msg) throws Exception {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = ilsproperties.getlogiodeletedfromplatformendpoint();
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("platform", AlfrescoConstants.ContainPlatforms.SPO.name())
                    .queryParam("id", msg.getItems().get(0).getId())
                    .queryParam("filename", "<Unknown>")
                    .queryParam("deletedby", "<Unknown>")
                    .queryParam("secondpath", "<Unknown>")
                    .queryParam("additionalinfo", msg.getDeltaLink());

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

            // Return the actual body from the response
            return response.getBody() != null && response.getBody();

        } catch (Exception e) {
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.RED
                    + "Fout bij aanroepen logSharepointItemdeletedendpoint Receiver: " +
                    e.getMessage()
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
            throw e;
        }
    }
}