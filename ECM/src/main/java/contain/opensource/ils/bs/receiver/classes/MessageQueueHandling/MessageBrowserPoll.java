package contain.opensource.ils.bs.receiver.classes.MessageQueueHandling;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import contain.opensource.ils.bs.receiver.classes.Logger.IOLog;
import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import contain.opensource.ils.bs.receiver.classes.UUIDUtil;
import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoNodeController;
import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoQueMessage;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.NodeType;

@Component
public class MessageBrowserPoll {

    public void ReadMessages(String[] args) {
        try {

            /// ================================================================================================================================
            /// TODO. HANGS ON CONSUMER CLOSED IF ALFRESCO SERVER IS BROUGHT DOWN. CHECK
            // MUST BE IMPLEMENTED AND CONSUMER REINITIATED IF SO!!!
            /// ================================================================================================================================

            // Connect to ActiveMQ
            ConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
            Connection connection = factory.createConnection("admin", "admin");
            connection.start();

            // Create a session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

            // session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            // The queue you want to inspect
            Queue queue = session.createQueue("Consumer.MyJavaConsumer.VirtualTopic.alfresco.repo.events.nodes");
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

    public static void StartPoll(MessageConsumer consumer, Session session) {
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
                                AlfrescoNodeController aController = new AlfrescoNodeController(QMessage.getNodeId());
                                if (nodeType.equals(NodeType.NODEREMOVED)) {
                                    // Only for ballenbak
                                    String action = QMessage.getId() + " : " + QMessage.getName()
                                            + " deleted from Alfresco by user " + QMessage.getUsername();
                                    IOLog.log(
                                            QMessage.getId(),
                                            "",
                                            secondPath.toString(),
                                            action,
                                            AlfrescoConstants.ContainPlatforms.ALFRESCO.toString(),
                                            AlfrescoConstants.ContainPlatforms.ALFRESCO.toString(),
                                            UUIDUtil.getUUID(),
                                            QMessage.getName(),
                                            "",
                                            AlfrescoConstants.eActionPerformed.IODELETED,
                                            QMessage.getUsername());
                                } else {
                                    aController.GetNode();
                                    String IOUUID = "";
                                    if (!aController.alfresconNodeResponse.HasUUID) {
                                        // Set UUID
                                        IOUUID = aController.UpdateNode(AlfrescoConstants.NodeTypeFields.UUID,
                                                Optional.ofNullable(secondPath.toString()), Optional.empty());
                                    }
                                    else {
                                        IOUUID = aController.alfresconNodeResponse.UUID;
                                    }

                                    // First sign and log
                                    // Only for ballenbak
                                    // ==========================================================================================
                                    // ALL FUNCTIONS SHOULD BE SEPARATED
                                    // ==========================================================================================
                                    String action = "Content and-or metadata changed : REBIND IO " + IOUUID + " : " + QMessage.getName();
                                    IOLog.log(
                                            IOUUID,
                                            QMessage.getId(),
                                            secondPath.toString(),
                                            action,
                                            AlfrescoConstants.ContainPlatforms.ALFRESCO.toString(),
                                            AlfrescoConstants.ContainPlatforms.ALFRESCO.toString(),
                                            UUIDUtil.getUUID(),
                                            QMessage.getName(),
                                            "",
                                            AlfrescoConstants.eActionPerformed.IOBOUND,
                                            QMessage.getUsername());
                                    // ==========================================================================================

                                    if (aController.alfresconNodeResponse.MustMove) {
                                        // Create generic property mapping information object
                                        RelocateInformationObject IOobject = new RelocateInformationObject(
                                                aController.alfresconNodeResponse,
                                                AlfrescoConstants.ContainPlatforms.ALFRESCO,
                                                AlfrescoConstants.ContainPlatforms.SPO);
                                        // MOVE FOR NOW ONLY TOGGLE BETWEEN SPO and ALFRESCO
                                        // Could Be done from here but because it is not yet certain from where the
                                        // relocaiton is called we use a REST API
                                        // aController.RelocateIO(IOobject);
                                        String endpoint = "http://localhost:5000/RelocateIO";
                                        String auth = Base64.getEncoder().encodeToString(
                                                (AlfrescoConstants.username + ":" + AlfrescoConstants.password)
                                                        .getBytes(StandardCharsets.UTF_8));
                                        String jsonBody = mapper.writeValueAsString(IOobject);

                                        // Create HttpPost
                                        HttpPost post = new HttpPost(endpoint);
                                        post.setHeader("Authorization", "Basic " + auth);
                                        post.setHeader("Content-Type", "application/json");
                                        post.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
                                        try (CloseableHttpClient client = HttpClients.createDefault();
                                                CloseableHttpResponse response = client.execute(post)) {

                                            int statusCode = response.getCode();
                                            if (statusCode != 200) {
                                                throw new IOException("HTTP error " + statusCode);
                                            }
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
