package contain.opensource.ils.bs.alfrescopoller.classes.messagequeuehandling;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import contain.opensource.shared.classes.AlfrescoQueMessage;
import contain.opensource.shared.classes.MessageBrowserPollParent;
import contain.opensource.shared.configurationproperties.ActiveMQProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.shared.constants.AlfrescoConstants;
import contain.opensource.shared.constants.AlfrescoConstants.NodeType;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

@Component
public class MessageBrowserPollAlfresco extends MessageBrowserPollParent {
  //  private Integer PollInterval = 5;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    // @Autowired
    // private AlfrescoNodeController aController;
    @Autowired
    public MessageBrowserPollAlfresco(ActiveMQProperties activeMQProps, AlfrescoProperties alfrescoProps,
            ILSRestProperties ilsproperties, ObjectMapper objectMapper) {
        super(
                activeMQProps,
                alfrescoProps,
                ilsproperties,
                objectMapper,
                null,
                activeMQProps.getAlfrescoSource().getQueue().toString(),
                activeMQProps.getAlfrescoSource());
        ;
    }

    /**
     * Reads messages from a specified ActiveMQ queue and polls for new messages at
     * fixed intervals.
     * p>
     * This method establishes a connection to the ActiveMQ broker using the
     * provided credentials and broker URL.
     * It creates a session and a consumer for the specified queue, then schedules a
     * polling task that periodically
     * invokes the {@code StartPoll} method to process messages.
     * p>
     * The method keeps the main thread alive indefinitely to allow continuous
     * polling. It also handles cleanup of
     * resources such as the session and connection upon termination.
     * p>
     * b>Note:</b> There is a known issue where the consumer may hang if the
     * Alfresco server is brought down.
     * Proper handling and reinitialization of the consumer should be implemented to
     * address this.
     *
     * @param args Command-line arguments (currently unused).
     */

    public void StartPoll(QueueBrowser browser, Session session, Queue queue) {
        try {
            String timestamp = ZonedDateTime.now(ZoneId.of("Europe/Amsterdam")).toLocalDateTime().format(formatter);
            String feedback = timestamp + " -> New ALFRESCO poll loop on broker : " + this.currentSource.getBrokerUrl()
                    + " on queue " + this.queuetopoll + ". Interval : " + PollInterval + " seconds";
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_YELLOW
                    + feedback
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
            Enumeration<?> messages = browser.getEnumeration();
            int count = 0;
            while (messages.hasMoreElements()) {
                Message msg = (Message) messages.nextElement();
                count++;
                System.out.println("Processing " + msg + " message # " + count + " from queue");
                try {
                    String json = "";
                    if (msg instanceof TextMessage) {
                        TextMessage text = (TextMessage) msg;
                        json = text.getText();
                        ObjectMapper mapper = new ObjectMapper();
                        // To do additional checkslike contenttype
                        try {
                            // to do validation on content type
                            AlfrescoQueMessage QMessage = mapper.readValue(json, AlfrescoQueMessage.class);
                            Object secondpath = "";
                            List<Object> paths = QMessage.getPaths();
                            if (paths != null && paths.size() > 1) {
                                secondpath = paths.get(1);
                            }
                            String type = QMessage.getType();
                            // ===========================================================================================
                            // TO BE MANAGED BY RULE-ENGINE AND GENERATOR
                            // ===========================================================================================
                            NodeType nodeType = NodeType.fromString(type);
                            if (nodeType != null) {
                                // Call Alfresco object controller
                                // aController.nodeId = QMessage.getNodeId();
                                if (nodeType.equals(NodeType.NODEREMOVED)) {
                                    // Only for ballenbak
                                    String action = QMessage.getId() + " : " + QMessage.getName()
                                            + " deleted from Alfresco by user " + QMessage.getUsername();
                                    logalfrescoiodeletedendpoint(QMessage, secondpath.toString());

                                } else {
                                    Hashtable<String, String> migrateinfo = processalfresconode(QMessage.getNodeId(),
                                            secondpath.toString());
                                    // Check 'platformto' in plaats van 'containerto'
                                    // To do source <> destination
                                    if (migrateinfo != null) {
                                        if (migrateinfo.get("platformto") != null
                                                && !migrateinfo.get("platformto").equals("<NO MOVE>")) {

                                            List<Object> pathsList = QMessage.getPaths();
                                            String rawPath = "";

                                            if (pathsList != null && !pathsList.isEmpty()) {
                                                // If the list has at least 2 items, it's likely the nested format
                                                // [Class,
                                                // [Data]]
                                                if (pathsList.size() >= 2 && pathsList.get(1) instanceof List) {
                                                    List<?> internalList = (List<?>) pathsList.get(1);
                                                    rawPath = internalList.get(0).toString();
                                                }
                                                // If it only has 1 item, it's the simple format [/path/to/file]
                                                else {
                                                    rawPath = pathsList.get(0).toString();
                                                }
                                            }

                                            // 2. Now clean the string (Brackets and Filename)
                                            if (!rawPath.isEmpty()) {
                                                String cleanPath = rawPath.replace("[", "").replace("]", "");
                                                int lastSlash = cleanPath.lastIndexOf("/");

                                                // This is your weburl: "/Company Home/Sites/ontobind/documentLibrary"
                                                String folderOnly = (lastSlash != -1)
                                                        ? cleanPath.substring(0, lastSlash)
                                                        : cleanPath;

                                                // 3. Now it is safe to call your send function

                                                SendMigrationMessage(folderOnly,
                                                        QMessage.getNodeId().toString(), "-",
                                                        AlfrescoConstants.ContainPlatforms.ALFRESCO.name(),
                                                        migrateinfo);

                                            }
                                        }
                                    }
                                }
                            }
                            consumeMessageById(msg.getJMSMessageID());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        System.out.println(contain.opensource.shared.constants.AlfrescoConstants.RED
                                + "Processing message: " + json
                                + contain.opensource.shared.constants.AlfrescoConstants.RESET);
                    } else {
                        System.out.println(contain.opensource.shared.constants.AlfrescoConstants.RED
                                + "Processing non-text message: " + msg
                                + contain.opensource.shared.constants.AlfrescoConstants.RESET);
                    }
                    // To do transaction and persistance

                } catch (JMSException processingError) {
                    System.err.println("Error while processing message, ROLLBACK.");
                    processingError.printStackTrace();
                }
            }
        } catch (

        JMSException e) {
            System.err.println("Error polling the queue:");
            e.printStackTrace();
        }
        // System.out.println("No remaining ALFRESCO messages on queue");
    }

    private Hashtable<String, String> processalfresconode(String nodeid, String secondpath) throws Exception {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = ilsproperties.getprocessalfresconodepoint();

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("nodeid", nodeid)
                    .queryParam("secondpath", secondpath);

            // 1. Create headers and set Content-Type to JSON
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 2. Create an HttpEntity with a null body but including the headers
            HttpEntity<String> entity = new HttpEntity<>(null, headers);

            // 3. Use the entity in the postForEntity call
            ResponseEntity<Hashtable> response = restTemplate.postForEntity(
                    builder.toUriString(),
                    entity,
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
                    + "Fout bij aanroepen processalfresconode in receiver: " +
                    e.getMessage()
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
            // This catches everything else that isn't an HTTP error (like a Timeout)
            throw e;
        }
        return null;
    }

    private boolean logalfrescoiodeletedendpoint(AlfrescoQueMessage QMessage, String secondpath) throws Exception {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = ilsproperties.getlogiodeletedfromplatformendpoint();
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("platform", AlfrescoConstants.ContainPlatforms.ALFRESCO.name())
                    .queryParam("id", QMessage.getId())
                    .queryParam("filename", QMessage.getName())
                    .queryParam("deletedby", QMessage.getUsername())
                    .queryParam("secondpath", secondpath)
                    .queryParam("additionalinfo", "-");

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

        } catch (Exception e) {
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.RED
                    + "Fout bij aanroepen logalfrescoiodeletedendpoint Receiver: " +
                    e.getMessage()
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
            throw e;
        }
    }
}
