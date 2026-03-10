package contain.opensource.ils.bs.migrationpoller.classes.messagequeuehandling;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import contain.opensource.ils.bs.receiver.classes.migration.MigrationQueueMessage;
import contain.opensource.ils.bs.receiver.classes.sharepoint.SharepointQueMessage;
import contain.opensource.ils.bs.receiver.services.GraphService;
import contain.opensource.shared.configurationproperties.ActiveMQProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ILSRestProperties;

@Component
public class MessageBrowserPollMigration {

    @Value("${activemq.brokerUrl}")
    private String brokerUrl;

    @Value("${activemq.user}")
    private String user;

    @Value("${activemq.password}")
    private String password;

    private Integer PollInterval = 15;
    private JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;
    private final ActiveMQProperties activeMQProps;
    private final AlfrescoProperties alfrescoProps;
    private final ILSRestProperties ILSProperties;
    private final GraphService graphService;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    // @Autowired
    // private AlfrescoNodeController aController;

    @Autowired
    public MessageBrowserPollMigration(ActiveMQProperties activeMQProps, AlfrescoProperties alfrescoProps,
            ILSRestProperties ilsProperties, GraphService graphService, ObjectMapper mapper, JmsTemplate jmsTemplate) {
        this.activeMQProps = activeMQProps;
        this.alfrescoProps = alfrescoProps;
        this.ILSProperties = ilsProperties;
        this.graphService = graphService;
        this.objectMapper = mapper;
        this.jmsTemplate = jmsTemplate;

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

    // Public method to start polling
    public void startPolling() {
        System.out.println("MessageBrowserPollSP: starting MIGRATION polling in background thread...");

        new Thread(() -> {
            try {
                ReadMessages();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "SPPoller-Thread").start();
    }

    public void ReadMessages() {
        try {

            /// ================================================================================================================================
            /// TODO. HANGS ON CONSUMER CLOSED IF ALFRESCO SERVER IS BROUGHT DOWN. CHECK
            // MUST BE IMPLEMENTED AND CONSUMER REINITIATED IF SO!!!
            // Method must be made more generic because there is duplicated code over
            /// several polling projects.
            /// ================================================================================================================================

            ConnectionFactory factory = new ActiveMQConnectionFactory(user, password, brokerUrl);
            Connection connection = factory.createConnection();
            connection.start();

            // Create a session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

            // session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            // The queue you want to inspect. THIS SHOULD BE CONFIGURATIONENTRY, NOT
            // HARDCODED
            Queue queue = session.createQueue(activeMQProps.getMigrationqueue());

            // Trial
            // Queue queue = session.createQueue("acs-repo-transform-request");
            // Queue queue =
            // session.createQueue("Consumer.cfd643ac-3ca4-35a9-9818-95efc887532a.VirtualTopic.alfresco.repo.events.nodes");
            MessageConsumer consumer = session.createConsumer(queue);

            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleWithFixedDelay(() -> {
                System.out.println("Polling MIGRATION messages...");

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

    public void StartPoll(MessageConsumer consumer, Session session) {
        try {
            String timestamp = LocalDateTime.now().format(formatter);
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_BRIGHT_CYAN
                    + timestamp + " -> New MIGRATION poll loop on broker : " + activeMQProps.getBrokerUrl()
                    + " on queue " + activeMQProps.getMigrationqueue() + ". Interval : " + PollInterval + " seconds"
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
            Message msg;
            String json = "";
            ObjectMapper mapper = new ObjectMapper();
            while ((msg = consumer.receive(1000)) != null) {
                try {
                    if (msg instanceof TextMessage) {
                        try {
                            json = ((TextMessage) msg).getText();
                            // TextMessage text = (TextMessage) msg;
                            // try {
                            System.out.println("RAW JSON: " + json);
                            MigrationQueueMessage message = mapper.readValue(json, MigrationQueueMessage.class);
                            // for (MigrationQueueMessage.Item item : message.Getkey()) {
                            System.out.println("Migrationmessage : " + message);
                            /*
                             * if (item.getParentReference() != null) {
                             * System.out.println("Site ID: " + item.getParentReference().getSiteId());
                             * }
                             * if (item.getFields() != null) {
                             * System.out.println("Fields: " + item.getFields());
                             * }
                             * String deltaLink = message.getDeltaLink().split("\\?")[0];
                             * if (item.getDeleted() != null) {
                             * 
                             * String action = "IO  " + item.getId() + " deleted from platform";
                             * IOLog.log(
                             * "DeletedFromPlatform",
                             * item.getId(),
                             * "DeletedFromPlatform",
                             * action,
                             * AlfrescoConstants.ContainPlatforms.SPO.toString(),
                             * AlfrescoConstants.ContainPlatforms.SPO.toString(),
                             * "DeletedFromPlatform",
                             * "DeletedFromPlatform",
                             * deltaLink,
                             * AlfrescoConstants.eActionPerformed.IODELETED,
                             * "<Unknown>",
                             * "DeletedFromPlatform",
                             * "DeletedFromPlatform",
                             * "DeletedFromPlatform");
                             * } else {
                             * boolean migrate =
                             * this.graphService.ProcessChangedSharepointItem(item.getWebUrl(),
                             * item.getId(), deltaLink);
                             * if (migrate) {
                             * SendMigrationMessage(item);
                             * }
                             * }
                             */
                            // }

                            //session.commit();
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

            }
        } catch (Exception e) {
            System.err.println("Error in StartPoll:");
            e.printStackTrace();
        }
        System.out.println("No remaining MIGRATION messages on queue");
    }

    // =======================================================================
    // As more functions this should be more generic and moved to central point.
    // =======================================================================
    private void SendMigrationMessage(SharepointQueMessage.Item item) {
        try {

            ConnectionFactory factory = new ActiveMQConnectionFactory(user, password, brokerUrl);

            // Create a connection
            Connection connection = factory.createConnection();
            connection.start();

            // Create a session (non-transacted, auto-acknowledge)
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

            // Create the queue (migration queue)
            Queue queue = session.createQueue(activeMQProps.getMigrationqueue());

            // Create a message producer
            MessageProducer producer = session.createProducer(queue);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            MigrationQueueMessage payload = new MigrationQueueMessage(item.getWebUrl(), "Migrate", "SPO",
                    item.getFields().get("Move").toString());
            String json = objectMapper.writeValueAsString(payload);
            String correlationId = MDC.get("correlationId");
            TextMessage message = session.createTextMessage(json);

            // Send the message
            producer.send(message);

            // Clean up
            producer.close();
            session.commit();
            session.close();
            connection.close();

        } catch (Exception ex) {
            // log.error("Failed to send delta message to ActiveMQ", ex);
            throw new IllegalStateException("Failed to send migration message to ActiveMQ", ex);
        }
    }
}
