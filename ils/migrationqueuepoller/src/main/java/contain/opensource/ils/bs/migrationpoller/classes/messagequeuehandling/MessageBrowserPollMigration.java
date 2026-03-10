package contain.opensource.ils.bs.migrationpoller.classes.messagequeuehandling;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    private String timestamp;
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

    /* */
    public void ReadMessages() {
        try {

            /// ================================================================================================================================
            /// TODO. HANGS ON CONSUMER CLOSED IF ALFRESCO SERVER IS BROUGHT DOWN. CHECK
            // MUST BE IMPLEMENTED AND CONSUMER REINITIATED IF SO!!!
            // Method must be made more generic because there is duplicated code over all
            /// different queuepollers/
            /// several polling projects.
            /// ================================================================================================================================

            ConnectionFactory factory = new ActiveMQConnectionFactory(user, password, brokerUrl);
            Connection connection = factory.createConnection();
            connection.start();

            // Create a session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue queue = session.createQueue(activeMQProps.getMigrationqueue());
            QueueBrowser browser = session.createBrowser(queue);
            
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleWithFixedDelay(() -> {
                System.out.println("Polling MIGRATION messages...");

     
                StartPoll(browser, session, queue); 
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

     */

   public void StartPoll(QueueBrowser browser, Session session, Queue queue) {
        try {
            timestamp = LocalDateTime.now().format(formatter);
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_BRIGHT_CYAN
                    + timestamp + " -> New MIGRATION poll loop on broker : " + activeMQProps.getBrokerUrl()
                    + " on queue " + activeMQProps.getMigrationqueue() + ". Interval : " + PollInterval + " seconds"
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
            Enumeration<?> messages = browser.getEnumeration();
            int count = 0;
            while (messages.hasMoreElements()) {
                count++;
                Message msg = (Message) messages.nextElement();
                System.out.println("Processing " + msg + " message # " + count + " from queue");
                try {
                    if (msg instanceof TextMessage) {
                        // Process migration
                        consumeMessageById(session, queue, msg.getJMSMessageID());
                    }
                } catch (Exception e) {
                    System.err.println("Error in StartPoll:");
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("Error in StartPoll:");
            e.printStackTrace();
        }
        System.out.println("No remaining MIGRATION messages on queue");
    }

    private void consumeMessageById(Session session, Queue queue, String messageId) throws JMSException {
        String selector = "JMSMessageID = '" + messageId + "'";
        MessageConsumer consumer = session.createConsumer(queue, selector);
        try {
            Message msg = consumer.receive(1000);
            if (msg != null) {
                session.commit(); // remove the message
                System.out.println("Message acknowledged " + messageId + " (removed from queue.)");
            }
        } catch (Exception ex) {
        } finally {
            consumer.close();
        }
    }
}