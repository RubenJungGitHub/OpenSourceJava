package contain.opensource.ils.bs.receiver.classes.MessageQueueHandling;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.PrivateKey;
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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;

import contain.opensource.ils.bs.receiver.classes.Binding.PKCS12KeyLoader;
import contain.opensource.ils.bs.receiver.classes.ConfigurationProperties.ActiveMQProperties;
import contain.opensource.ils.bs.receiver.classes.ConfigurationProperties.AlfrescoProperties;
import contain.opensource.ils.bs.receiver.classes.ConfigurationProperties.ILSRestProperties;
import contain.opensource.ils.bs.receiver.classes.Logger.IOLog;
import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoNodeController;
import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoQueMessage;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.NodeType;
import contain.opensource.ils.bs.receiver.classes.Binding.BindRequest;
import contain.opensource.ils.bs.receiver.classes.Redis.RedisManager;

@Component
public class MessageBrowserPoll {

    @Value("${activemq.brokerUrl}")
    private String brokerUrl;

    @Value("${activemq.user}")
    private String user;

    @Value("${activemq.password}")
    private String password;

    private final ActiveMQProperties activeMQProps;
    private final AlfrescoProperties alfrescoProps;
    private final ILSRestProperties ILSProperties;

    @Autowired
    private AlfrescoNodeController aController;

    @Autowired
    public MessageBrowserPoll(ActiveMQProperties activeMQProps, AlfrescoProperties alfrescoProps,
            ILSRestProperties ilsProperties) {
        this.activeMQProps = activeMQProps;
        this.alfrescoProps = alfrescoProps;
        this.ILSProperties = ilsProperties;
    }

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
            Queue queue = session.createQueue("Consumer.MyJavaConsumer.VirtualTopic.alfresco.repo.events.nodes");

            // Trial
            // Queue queue = session.createQueue("acs-repo-transform-request");
            // Queue queue =
            // session.createQueue("Consumer.cfd643ac-3ca4-35a9-9818-95efc887532a.VirtualTopic.alfresco.repo.events.nodes");
            MessageConsumer consumer = session.createConsumer(queue);

            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleAtFixedRate(() -> {
                System.out.println("Polling messages...");

                StartPoll(consumer, session);
            }, 0, 5, TimeUnit.SECONDS);

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

    public void StartPoll(MessageConsumer consumer, Session session) {
        try {
            System.out.println(contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.YELLOW
                    + "New poll loop Processing"
                    + contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RESET);
            Message msg;
            while ((msg = consumer.receiveNoWait()) != null) {
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
                                // alfrescoNodeController.GetNode(QMessage.getNodeId());
                                // AlfrescoNodeController aController = new
                                // AlfrescoNodeController(QMessage.getNodeId());
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
                                            aController.alfresconNodeResponse.marking,
                                            aController.alfresconNodeResponse.label,
                                            aController.alfresconNodeResponse.version);
                                } else {
                                    aController.GetNode();
                                    String IOUUID = "";
                                    if (!aController.alfresconNodeResponse.HasUUID) {
                                        // Set UUID
                                        IOUUID = aController.UpdateNode(AlfrescoConstants.NodeTypeFields.UUID,
                                                Optional.ofNullable(secondPath.toString()), Optional.empty());
                                    } else {
                                        IOUUID = aController.alfresconNodeResponse.UUID;
                                    }

                                    // Add to Redis cache to avoid double binding.
                                    String redisentry = "IOinProcess-" + IOUUID;

                                    if (RedisManager.getHashField(redisentry) == null) {
                                        RedisManager.putHash("IOinProcess", redisentry, "InProcess", 120);
                                    } else {
                                        RedisManager.deleteEntry(redisentry);
                                        return;
                                    }

                                    // First sign and log
                                    // ==========================================================================================
                                    // ALL FUNCTIONS SHOULD BE SEPARATED
                                    // ==========================================================================================

                                    // IN the future store as actual byte in Redis and datastore. For POC store as
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
                                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                    conn.setRequestMethod("POST");

                                    int status = conn.getResponseCode();
                                    System.out.println(
                                            "Accessing uuid rest url on " + endPoint + " return code -> " + status);

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
                                                aController.alfresconNodeResponse.label,
                                                aController.alfresconNodeResponse.version);
                                        // ==========================================================================================
                                    }

                                    // Moveobject
                                    if (aController.alfresconNodeResponse.MustMove) {
                                        // Create generic property mapping information object
                                        RelocateInformationObject IOobject = new RelocateInformationObject(
                                                aController.alfresconNodeResponse,
                                                response.getBody(),
                                                AlfrescoConstants.ContainPlatforms.ALFRESCO,
                                                AlfrescoConstants.ContainPlatforms.SPO);
                                        // MOVE FOR NOW ONLY TOGGLE BETWEEN SPO and ALFRESCO
                                        // Could Be done from here but because it is not yet certain from where the
                                        // relocaiton is called we use a REST API
                                        // aController.RelocateIO(IOobject);

                                        String endpoint = String.format(
                                                "%s/RelocateIO",
                                                this.ILSProperties.getBaseUrl());
                                        headers = new HttpHeaders();
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

                                        status = response.getStatusCode().value();
                                        if (status != 200) {
                                            throw new IOException("HTTP error " + status);
                                        }
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
        System.out.println("No remaining messages on queue");
    }
}
