package contain.opensource.java.ils.bs.receiver.classes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;

import com.fasterxml.jackson.databind.ObjectMapper;

import contain.opensource.java.ils.bs.receiver.constants.AlfrescoConstants;
import contain.opensource.java.ils.bs.receiver.constants.AlfrescoConstants.NodeType;

public class MessageBrowserPoll {

    public static void ReadMessages(String[] args) {
        try {

            /// ================================================================================================================================
            /// TODO. HANGS ON CINSUMER CLOSED IF ALFRESCO SSERVER IS BROUGHT DOWN. CHECK
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
            System.out.println(contain.opensource.java.ils.bs.receiver.constants.AlfrescoConstants.YELLOW
                    + "New poll loop Processing"
                    + contain.opensource.java.ils.bs.receiver.constants.AlfrescoConstants.RESET);
            Message msg;
            while ((msg = consumer.receiveNoWait()) != null) {
                try {
                    String json = "";
                    if (msg instanceof TextMessage) {
                        TextMessage text = (TextMessage) msg;
                        json = text.getText();
                        ObjectMapper mapper = new ObjectMapper();
                        try {
                            AlfresQueMessage QMessage = mapper.readValue(json, AlfresQueMessage.class);
                            String type = QMessage.getType();
                            // ===========================================================================================
                            // TO BE MANAGED BY RULE-ENGINE AND GENERATOR
                            // ===========================================================================================
                            NodeType nodeType = NodeType.fromString(type);
                            if (nodeType != null) {
                                System.out.println("ID: " + QMessage.getId());
                                System.out.println("Username: " + QMessage.getUsername());
                                System.out.println("Type: " + QMessage.getType());
                                System.out.println("Timestamp: " + QMessage.getTimestamp());
                                System.out.println("NodeType : " + QMessage.getType());
                                System.out.println("NodeId : " + QMessage.getNodeId());
                                // Call Alfresco object controller
                                AlfrescoNodeController aController = new AlfrescoNodeController(QMessage.getNodeId());
                                if (nodeType.equals(NodeType.NODEREMOVED)) {
                                    // Only for ballenbak

                                } else {
                                    aController.GetNode();
                                    if (!aController.alfresconNodeResponse.HasUUID) {
                                        // Set UUID
                                        aController.UpdateNode(AlfrescoConstants.NodeTypeFields.UUID, Optional.empty());
                                    }
                                    if (aController.alfresconNodeResponse.MustMove) {

                                        // Create generic property mapping information object
                                        InformationObject IOobject = new InformationObject(
                                                aController.alfresconNodeResponse,
                                                AlfrescoConstants.ContainPlatforms.ALFRESCO,
                                                AlfrescoConstants.ContainPlatforms.SPO);
                                        // MOVE FOR NOW ONLY TOGGLE BETWEEN SPO and ALFRESCO
                                        // Could Be done from here but because it is not yet crtain from where the
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
                        System.out.println(contain.opensource.java.ils.bs.receiver.constants.AlfrescoConstants.RED
                                + "Processing message: " + json
                                + contain.opensource.java.ils.bs.receiver.constants.AlfrescoConstants.RESET);
                    } else {
                        System.out.println(contain.opensource.java.ils.bs.receiver.constants.AlfrescoConstants.RED
                                + "Processing non-text message: " + msg
                                + contain.opensource.java.ils.bs.receiver.constants.AlfrescoConstants.RESET);
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
        } catch (JMSException e) {
            System.err.println("Error polling the queue:");
            e.printStackTrace();
        }
        System.out.println("No remaining messages on queue");
    }
}
