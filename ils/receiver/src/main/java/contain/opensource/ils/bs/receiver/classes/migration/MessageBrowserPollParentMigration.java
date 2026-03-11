package contain.opensource.ils.bs.receiver.classes.migration;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import contain.opensource.shared.configurationproperties.ActiveMQProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ILSRestProperties;

public abstract class MessageBrowserPollParentMigration {

    @Value("${activemq.brokerUrl}")
    public String brokerUrl;

    @Value("${activemq.user}")
    public String user;

    @Value("${activemq.password}")
    public String password;

    public Integer PollInterval = 15;
    public JmsTemplate jmsTemplate;
    public final ObjectMapper objectMapper;
    public final ActiveMQProperties activeMQProps;
    public final AlfrescoProperties alfrescoProps;
    public final ILSRestProperties ILSProperties;
    // private final GraphService graphService;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    public Session session;
    public Connection connection;
    public Queue queue;
    public ActiveMQConnectionFactory factory;
    public QueueBrowser browser;
    public String timestamp;

    // @Autowired
    // private AlfrescoNodeController aController;

    @Autowired
    public MessageBrowserPollParentMigration(ActiveMQProperties activeMQProps, AlfrescoProperties alfrescoProps,
            ILSRestProperties ilsProperties, ObjectMapper mapper, JmsTemplate jmsTemplate) {
        this.activeMQProps = activeMQProps;
        this.alfrescoProps = alfrescoProps;
        this.ILSProperties = ilsProperties;
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
    public void startPolling(String queueid) {
        System.out.println("MessageBrowserPollSP: starting MIGRATION polling in background thread...");

        new Thread(() -> {
            try {
                ReadMessages(queueid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "SPPoller-Thread").start();
    }

    private Connection createConnectionWithRetry() {
        int retryCount = 0;
        int maxRetries = 10; // or Integer.MAX_VALUE for infinite
        int retryIntervalSec = 10;
        while (true) {
            try {
                factory = new ActiveMQConnectionFactory(user, password, brokerUrl);
                connection = factory.createConnection();
                connection.start();
                session = connection.createSession(true, Session.SESSION_TRANSACTED);
                System.out.println("Connected to ActiveMQ!");
                return connection;
            } catch (JMSException e) {
                retryCount++;
                System.err.println("Failed to connect to ActiveMQ (attempt " + retryCount + "): " + e.getMessage());
                try {
                    Thread.sleep(retryIntervalSec * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting to retry ActiveMQ connection", ie);
                }
            }
        }
    }

    public void ReadMessages(String queueid) {
        try {

            /// ================================================================================================================================
            /// TODO. HANGS ON CONSUMER CLOSED IF ALFRESCO SERVER IS BROUGHT DOWN. CHECK
            // MUST BE IMPLEMENTED AND CONSUMER REINITIATED IF SO!!!
            // Method must be made more generic because there is duplicated code over all
            /// different queuepollers/
            /// several polling projects.
            /// ================================================================================================================================
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleWithFixedDelay(() -> {
                System.out.println("Polling MIGRATION messages...");

                try {
                    connection = createConnectionWithRetry();

                    queue = session.createQueue(queueid);
                    browser = session.createBrowser(queue);
                    StartPoll(browser, session, queue);
                } catch (JMSException e) {
                    System.err.println("Session/Connection error: " + e.getMessage());
                    // will retry creating connection after outer while loop
                }
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

    public void StartPoll(QueueBrowser browser, Session session, Queue queue) {
    }

    public void consumeMessageById(String messageId) throws JMSException {
        String selector = "JMSMessageID = '" + messageId + "'";
        // Check session is open
        MessageConsumer consumer = null;

        try {
            if (session instanceof ActiveMQSession amqSession) {
                if (amqSession.isClosed()) {
                  connection = createConnectionWithRetry();
                }
            }
            consumer = session.createConsumer(queue, selector);
            Message msg = consumer.receive(1000);
            if (msg != null) {
                session.commit(); // remove the message
                System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BRIGHT_CYAN
                        + timestamp + "Message acknowledged " + messageId + " (removed from queue.)"
                        + contain.opensource.shared.constants.AlfrescoConstants.RESET);
            }
        } catch (Exception ex) {
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_BRIGHT_RED
                    + timestamp + "Error clearing message from queue " + ex
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
        } finally {
            if (consumer != null) {
                consumer.close();
            }
        }
    }
}