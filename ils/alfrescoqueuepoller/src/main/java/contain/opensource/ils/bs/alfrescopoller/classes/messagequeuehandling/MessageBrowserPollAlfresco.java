package contain.opensource.ils.bs.alfrescopoller.classes.messagequeuehandling;

import java.net.URL;
import java.time.ZonedDateTime;
import java.time.ZoneId;

import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.JMSException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import contain.opensource.ils.bs.receiver.classes.Binding.BindRequest;
//import contain.opensource.ils.bs.receiver.classes.Logger.IOLogPostgress;
import contain.opensource.ils.bs.receiver.classes.Logger.IOLog;
import contain.opensource.ils.bs.receiver.classes.Redis.RedisManager;
import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoNodeController;
import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoQueMessage;
import contain.opensource.shared.configurationproperties.ActiveMQProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.shared.constants.AlfrescoConstants;
import contain.opensource.shared.constants.AlfrescoConstants.ContainPlatforms;
import contain.opensource.shared.constants.AlfrescoConstants.NodeType;
import contain.opensource.ils.bs.receiver.classes.migration.MessageBrowserPollParent;

@Component
public class MessageBrowserPollAlfresco extends MessageBrowserPollParent {
    private Integer PollInterval = 15;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    @Autowired
    private AlfrescoNodeController aController;

    @Autowired
    public MessageBrowserPollAlfresco(ActiveMQProperties activeMQProps, AlfrescoProperties alfrescoProps,
            ILSRestProperties ilsProperties, ObjectMapper objectMapper) {
              super(
            activeMQProps,
            alfrescoProps,
            ilsProperties,
            objectMapper,
            null,
            null
        );
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

    private String BindIO(String IOUUID, AlfrescoQueMessage QMessage, Object secondPath) {
        // First sign and log

        // IN the future store as actual byte in Redis and datastore. For POC store as
        try {
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_CYAN
                    + contain.opensource.shared.constants.AlfrescoConstants.BRIGHT_RED
                    + ("Binding ALFRESCO NODE IO " + IOUUID)
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);

            // Create request WITHOUT key
            BindRequest request = new BindRequest(aController.alfresconNodeResponse.ToSecuredDocument());

            String endPoint = ILSProperties.getbindendpoint();
            System.out
                    .println(contain.opensource.shared.constants.AlfrescoConstants.RED
                            + "Binding endpoint  : "
                            + endPoint
                            + contain.opensource.shared.constants.AlfrescoConstants.RESET);

            URL url = new URL(endPoint);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<BindRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(endPoint, entity,
                    String.class);

            // Move log to binding function
            String action = "Content and-or metadata changed : REBIND IO " + IOUUID + " : "
                    + QMessage.getName();
            if (response.getStatusCode().value() == 200) {
                IOLog.log(
                        IOUUID,
                        QMessage.getId(),
                        secondPath.toString(),
                        action,
                        AlfrescoConstants.ContainPlatforms.ALFRESCO.toString(),
                        AlfrescoConstants.ContainPlatforms.ALFRESCO.toString(),
                        response.getBody(),
                        QMessage.getName(),
                        "",
                        AlfrescoConstants.eActionPerformed.IOBOUND,
                        QMessage.getUsername(),
                        aController.alfresconNodeResponse.marking,
                        aController.alfresconNodeResponse.classification,
                        aController.alfresconNodeResponse.version);
                // ==========================================================================================
            }
            return response.getBody();
        } catch (Exception e) {
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.RED
                    + "Error during binding: " + e.getMessage()
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
            e.printStackTrace();
            return "Binding failed" + e.getMessage();
        }
    }

    public void StartPoll(QueueBrowser browser, Session session, Queue queue) {
        try {
            String timestamp = ZonedDateTime.now(ZoneId.of("Europe/Amsterdam")).toLocalDateTime().format(formatter);
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_YELLOW
                    + timestamp + " -> New ALFREASCO poll loop on broker : " + activeMQProps.getBrokerUrl()
                    + " on queue " + activeMQProps.getAlfrescoQueue() + ". Interval : " + PollInterval + " seconds"
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
                        try {
                            AlfrescoQueMessage QMessage = mapper.readValue(json, AlfrescoQueMessage.class);
                            Object secondPath = "";
                            List<Object> paths = QMessage.getPaths();
                            if (paths != null && paths.size() > 1) {
                                secondPath = paths.get(1);
                            }
                            String type = QMessage.getType();
                            // ===========================================================================================
                            // TO BE MANAGED BY RULE-ENGINE AND GENERATOR
                            // ===========================================================================================
                            NodeType nodeType = NodeType.fromString(type);
                            if (nodeType != null) {
                                // Call Alfresco object controller
                                aController.nodeId = QMessage.getNodeId();
                                if (nodeType.equals(NodeType.NODEREMOVED)) {
                                    // Only for ballenbak
                                    String action = QMessage.getId() + " : " + QMessage.getName()
                                            + " deleted from Alfresco by user " + QMessage.getUsername();
                                    // Remove from Redis. For SPO this is going to be a challenge
                                    // RedisManager.deleteHashField("IOLogs", type);
                                    IOLog.log(
                                            "DeletedFromPlatform",
                                            "",
                                            secondPath.toString(),
                                            action,
                                            AlfrescoConstants.ContainPlatforms.ALFRESCO.toString(),
                                            AlfrescoConstants.ContainPlatforms.ALFRESCO.toString(),
                                            "DeletedFromPlatform",
                                            QMessage.getName(),
                                            "",
                                            AlfrescoConstants.eActionPerformed.IODELETED,
                                            QMessage.getUsername(),
                                            "DeletedFromPlatform",
                                            "DeletedFromPlatform",
                                            "DeletedFromPlatform");
                                } else {
                                    aController.GetNode();
                                    String IOUUID = "";
                                    if (!aController.alfresconNodeResponse.HasUUID) {
                                        // Set UUID
                                        IOUUID = aController.UpdateNode(AlfrescoConstants.NodeTypeFields.UUID,
                                                ILSProperties.getuudiutilendpoint(),
                                                Optional.ofNullable(secondPath.toString()), Optional.empty());
                                        // IOUUID = aController.UpdateNode(AlfrescoConstants.NodeTypeFields.UUID
                                        // ,Optional.ofNullable(secondPath.toString()), Optional.empty());

                                    } else {
                                        IOUUID = aController.alfresconNodeResponse.UUID;
                                    }

                                    // redis redindant. To memcollection?
                                    String redisLogId = IOUUID;
                                    // Add to Redis cache to avoid double binding.
                                    for (ContainPlatforms platform : ContainPlatforms.values()) {
                                        redisLogId = redisLogId.replace(platform.toString(), "");
                                    }
                                    String redisentryInRelocation = "IOinRelocateProcess" + redisLogId;
                                    String redisentryUUIDAssigned = "IOinUUIDAssigned" + IOUUID;

                                    if (RedisManager.getHashField(redisentryInRelocation) != null) {
                                        RedisManager.deleteEntry(redisentryInRelocation);
                                        return;
                                    }
                                    if (RedisManager.getHashField(redisentryUUIDAssigned) != null
                                            && aController.alfresconNodeResponse.HasUUID) {
                                        RedisManager.deleteEntry(redisentryUUIDAssigned);
                                        return;
                                    }

                                    // ========================================================================
                                    // To be moved to migration service
                                    // ========================================================================
                                    /*
                                     * // Moveobject, binding in new environment.
                                     * if (aController.alfresconNodeResponse.MustMove) {
                                     * System.out.println(
                                     * contain.opensource.shared.constants.AlfrescoConstants.CYAN
                                     * + "Alfresco  node must-move?"
                                     * + aController.alfresconNodeResponse.MustMove
                                     * + contain.opensource.shared.constants.AlfrescoConstants.RESET);
                                     * RedisManager.putHash("IOinRelocateProcess", redisentryInRelocation,
                                     * "InProcess",
                                     * 120);
                                     * 
                                     * // Create generic property mapping information object
                                     * RelocateInformationObject IOobject = new RelocateInformationObject(
                                     * aController.alfresconNodeResponse,
                                     * "BOUND ON DESTINATION PLATFORM",
                                     * AlfrescoConstants.ContainPlatforms.ALFRESCO,
                                     * AlfrescoConstants.ContainPlatforms.SPO);
                                     * // MOVE FOR NOW ONLY TOGGLE BETWEEN SPO and ALFRESCO
                                     * // Could Be done from here but because it is not yet certain from where the
                                     * // relocaiton is called we use a REST API
                                     * // aController.RelocateIO(IOobject);
                                     * RestTemplate restTemplate = new RestTemplate();
                                     * // String endpoint = String.format(
                                     * // "%s/RelocateIO",
                                     * // this.ILSProperties.getBaseUrl());
                                     * String endpoint = this.ILSProperties.getBaseUrl();
                                     * HttpHeaders headers = new HttpHeaders();
                                     * headers.setContentType(MediaType.APPLICATION_JSON);
                                     * headers.setBasicAuth(
                                     * AlfrescoConstants.username,
                                     * AlfrescoConstants.password,
                                     * StandardCharsets.UTF_8);
                                     * 
                                     * HttpEntity<RelocateInformationObject> entity2 = new HttpEntity<>(IOobject,
                                     * headers);
                                     * 
                                     * ResponseEntity<String> response2 = restTemplate.postForEntity(endpoint,
                                     * entity2,
                                     * String.class);
                                     * 
                                     * System.out.println("Status: " + response2.getStatusCodeValue());
                                     * System.out.println("Body: " + response2.getBody());
                                     * 
                                     * Integer status = response2.getStatusCode().value();
                                     * if (status != 200) {
                                     * throw new IOException("HTTP error " + status);
                                     * }
                                     * } else {
                                     * // BindObject
                                     * BindIO(IOUUID, QMessage, secondPath);
                                     * }
                                     */

                                    BindIO(IOUUID, QMessage, secondPath);
                                    // boolean migrate =
                                    // this.graphService.ProcessChangedSharepointItem(item.getWebUrl(),
                                    // item.getId(), deltaLink);
                                    // if (migrate) {
                                    // SendMigrationMessage(item);
                                    // }
                                }
                            }
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

                    //consumeMessageById(msg.getJMSMessageID());

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
        System.out.println("No remaining ALFRESCO messages on queue");
    }
}
