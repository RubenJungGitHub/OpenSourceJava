package contain.opensource.ils.bs.receiver.classes.MessageQueueHandling;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import contain.opensource.ils.bs.receiver.classes.Binding.BindRequest;
import contain.opensource.ils.bs.receiver.classes.Binding.PKCS12KeyLoader;
import contain.opensource.shared.configurationproperties.ActiveMQProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.ils.bs.receiver.classes.Logger.IOLog;
import contain.opensource.ils.bs.receiver.classes.Redis.RedisManager;
import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoNodeController;
import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoQueMessage;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.ContainPlatforms;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.NodeType;

/**
 * MessageBrowserPoll is a Spring component responsible for polling messages
 * from an ActiveMQ queue,
 * processing them, and interacting with Alfresco and other external systems as
 * required.
 * p>
 * This class establishes a connection to an ActiveMQ broker, creates a consumer
 * for a specific queue,
 * and periodically polls for new messages using a scheduled executor. Each
 * message is processed according
 * to its type, with support for handling node removals, updating nodes, binding
 * content, and relocating
 * information objects between platforms.
 * p>
 * Key Features:
 *
 * Configurable ActiveMQ connection via Spring properties.
 * Scheduled polling of messages from a designated queue.
 * Integration with Alfresco for node operations via
 * AlfrescoNodeController.
 * Support for message acknowledgment and transaction management.
 * Logging and error handling for message processing and external service
 * interactions.
 * Interaction with Redis for caching and deduplication of processing.
 * REST calls to external services for binding and relocating information
 * objects.
 *
 * p>
 * Note: The class contains TODOs and comments regarding handling consumer
 * closure and Alfresco server downtime.
 * Proper resource cleanup and error handling are implemented to ensure
 * reliability.
 *
 * Dependencies:
 *
 * Spring Framework (for dependency injection and configuration)
 * ActiveMQ JMS client
 * Jackson (for JSON processing)
 * AlfrescoNodeController and related domain classes
 * RedisManager for caching
 * RestTemplate for REST API calls
 *
 *
 * Usage:
 * 
 * pre>
 * 
 * @Autowired
 *            private MessageBrowserPoll messageBrowserPoll;
 *            ...
 *            messageBrowserPoll.ReadMessages(args);
 *            pre>
 *
 *            Configuration:
 *            ul>
 *            activemq.brokerUrl - URL of the ActiveMQ broker
 *            activemq.user - Username for ActiveMQ
 *            activemq.password - Password for ActiveMQ
 *            ul>
 *
 *            Thread Safety: This class is designed to be used as a singleton
 *            Spring bean.
 *
 *            Author: [Your Name or Team]
 *            Since: [Version or Date]
 */
@Component
public class MessageBrowserPollAlfresco {

    @Value("${activemq.brokerUrl}")
    private String brokerUrl;

    @Value("${activemq.user}")
    private String user;

    @Value("${activemq.password}")
    private String password;

    private Integer PollInterval = 15;

    private final ActiveMQProperties activeMQProps;
    private final AlfrescoProperties alfrescoProps;
    private final ILSRestProperties ILSProperties;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    @Autowired
    private AlfrescoNodeController aController;

    @Autowired
    public MessageBrowserPollAlfresco(ActiveMQProperties activeMQProps, AlfrescoProperties alfrescoProps,
            ILSRestProperties ilsProperties) {
        this.activeMQProps = activeMQProps;
        this.alfrescoProps = alfrescoProps;
        this.ILSProperties = ilsProperties;
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
    public void ReadMessages(String[] args) {
        try {

            /// ================================================================================================================================
            /// TODO. HANGS ON CONSUMER CLOSED IF ALFRESCO SERVER IS BROUGHT DOWN. CHECK
            // MUST BE IMPLEMENTED AND CONSUMER REINITIATED IF SO!!!
            /// ================================================================================================================================

            ConnectionFactory factory = new ActiveMQConnectionFactory(user, password, brokerUrl);
            Connection connection = factory.createConnection();
            connection.start();

            // Create a session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

            // session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            // The queue you want to inspect
            Queue queue = session.createQueue(activeMQProps.getAlfrescoQueue());

            // Trial
            // Queue queue = session.createQueue("acs-repo-transform-request");
            // Queue queue =
            // session.createQueue("Consumer.cfd643ac-3ca4-35a9-9818-95efc887532a.VirtualTopic.alfresco.repo.events.nodes");
            MessageConsumer consumer = session.createConsumer(queue);

            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleWithFixedDelay(() -> {
                System.out.println("Polling ALFRESCO messages...");

                StartPoll(consumer, session);
            }, 0, PollInterval, TimeUnit.SECONDS);

            // Keep the main thread alive indefinitely

            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Cleanup

            try {
                if (session != null)
                    session.close();
            } catch (Exception ignored) {
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }

    /**
     * Starts polling messages from the provided JMS consumer and processes each
     * message.
     * p>
     * This method continuously receives messages from the given
     * {@link MessageConsumer}
     * without waiting, processes each message according to its type, and performs
     * actions such as logging, updating, binding, and relocating information
     * objects.
     * It handles both text and non-text messages, commits the session after
     * successful
     * processing, and rolls back the session in case of processing errors.
     * p>
     * For text messages, it parses the message content as JSON, maps it to an
     * {@code AlfrescoQueMessage}, and performs actions based on the node type,
     * including logging deletions, updating nodes, binding, and relocating objects
     * between platforms. It also interacts with Redis for caching and duplicate
     * prevention.
     * p>
     * Exceptions during message processing are caught and logged, and the session
     * is
     * rolled back to ensure message integrity.
     *
     * @param consumer the {@link MessageConsumer} to poll messages from
     * @param session  the JMS {@link Session} used for message acknowledgment and
     *                 transaction management
     */
    private String BindIO(String IOUUID, AlfrescoQueMessage QMessage, Object secondPath) {
        // First sign and log

        // IN the future store as actual byte in Redis and datastore. For POC store as
        try {
            System.out.println(contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.BG_CYAN
                    + contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.BRIGHT_RED
                    + ("Binding ALFRESCO NODE IO " + IOUUID)
                    + contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RESET);
            PrivateKey key = PKCS12KeyLoader.PK;
            String privateKeyBase64 = Base64.getEncoder().encodeToString(key.getEncoded());

            BindRequest request = new BindRequest(
                    aController.alfresconNodeResponse.ToSecuredDocument(), privateKeyBase64);
            String endPoint = ILSProperties.getBaseUrl() + "/api/Bind";
            System.out
                    .println(contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RED
                            + "Binding endpoint  : "
                            + endPoint
                            + contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RESET);

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
            System.out.println(contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RED
                    + "Error during binding: " + e.getMessage()
                    + contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RESET);
            e.printStackTrace();
            return "Binding failed" + e.getMessage();
        }
    }

    public void StartPoll(MessageConsumer consumer, Session session) {
        try {
            String timestamp = LocalDateTime.now().format(formatter);
            System.out.println(contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.BG_YELLOW
                    + timestamp + " -> New ALFRESCO poll loop Processing. Interval : " + PollInterval + " seconds"
                    + contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RESET);
            Message msg;
            while ((msg = consumer.receive(1000)) != null) {
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
                                        IOUUID = aController.UpdateNode(AlfrescoConstants.NodeTypeFields.UUID, ILSProperties.getuudiutilendpoint() ,Optional.ofNullable(secondPath.toString()), Optional.empty());
                                        //IOUUID = aController.UpdateNode(AlfrescoConstants.NodeTypeFields.UUID ,Optional.ofNullable(secondPath.toString()), Optional.empty());

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


                                    // Moveobject, binding in new environment.
                                    if (aController.alfresconNodeResponse.MustMove) {
                                        System.out.println(
                                                contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.CYAN
                                                        + "Alfresco  node must-move?"
                                                        + aController.alfresconNodeResponse.MustMove
                                                        + contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RESET);
                                        RedisManager.putHash("IOinRelocateProcess", redisentryInRelocation, "InProcess",
                                                120);

                                        // Create generic property mapping information object
                                        RelocateInformationObject IOobject = new RelocateInformationObject(
                                                aController.alfresconNodeResponse,
                                                "BOUND ON DESTINATION PLATFORM",
                                                AlfrescoConstants.ContainPlatforms.ALFRESCO,
                                                AlfrescoConstants.ContainPlatforms.SPO);
                                        // MOVE FOR NOW ONLY TOGGLE BETWEEN SPO and ALFRESCO
                                        // Could Be done from here but because it is not yet certain from where the
                                        // relocaiton is called we use a REST API
                                        // aController.RelocateIO(IOobject);
                                        RestTemplate restTemplate = new RestTemplate();        
                                        String endpoint = String.format(
                                                "%s/RelocateIO",
                                                this.ILSProperties.getBaseUrl());
                                        HttpHeaders headers = new HttpHeaders();
                                        headers.setContentType(MediaType.APPLICATION_JSON);
                                        headers.setBasicAuth(
                                                AlfrescoConstants.username,
                                                AlfrescoConstants.password,
                                                StandardCharsets.UTF_8);

                                        HttpEntity<RelocateInformationObject> entity2 = new HttpEntity<>(IOobject,
                                                headers);

                                        ResponseEntity<String> response2 = restTemplate.postForEntity(endpoint, entity2,
                                                String.class);

                                        System.out.println("Status: " + response2.getStatusCodeValue());
                                        System.out.println("Body: " + response2.getBody());

                                        Integer status = response2.getStatusCode().value();
                                        if (status != 200) {
                                            throw new IOException("HTTP error " + status);
                                        }
                                    } else {
                                        // BindObject
                                        BindIO(IOUUID, QMessage, secondPath);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        System.out.println(contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RED
                                + "Processing message: " + json
                                + contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RESET);
                    } else {
                        System.out.println(contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RED
                                + "Processing non-text message: " + msg
                                + contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RESET);
                    }

                    // msg.acknowledge(); // only removes message after successful processing

                    // Debug
                    session.commit();
                    // session.rollback();
                    System.out.println("Message acknowledged (removed from queue).");

                } catch (JMSException processingError) {
                    session.rollback();
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
