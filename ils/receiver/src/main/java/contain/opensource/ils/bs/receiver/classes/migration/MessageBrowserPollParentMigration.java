package contain.opensource.ils.bs.receiver.classes.migration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import contain.opensource.shared.configurationproperties.ActiveMQProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;

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
//    public final ILSRestProperties ILSProperties;
    public Session session;
    public Connection connection;
    public Queue queue;
    public ActiveMQConnectionFactory factory;
    public QueueBrowser browser;
    public String timestamp;
    private volatile boolean pollingActive = false;
    private final ExecutorService pollExecutor = Executors.newSingleThreadExecutor();
    protected ILSRestProperties ILSProperties;
    
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
        synchronized (this) {
            if (pollingActive) {
                System.out.println("Polling already active for queue: " + queueid);
                return; // don't start another poller
            }
            pollingActive = true;
        }

        Thread pollerThread = new Thread(() -> {
            try {
                pollLoop(queueid);
            } finally {
                pollingActive = false;
                pollExecutor.shutdown();
            }
        }, "SPPoller-Thread");

        pollerThread.setDaemon(true);
        pollerThread.start();
    }

    private void pollLoop(String queueid) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Ensure connection/session exist
                if (connection == null) {
                    connection = createConnectionWithRetry();
                    session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                }

                queue = session.createQueue(queueid);
                browser = session.createBrowser(queue);

                System.out.println("Polling queue messages...");

                // Submit StartPoll as a Future and block until finished
                Future<?> pollFuture = pollExecutor.submit(() -> StartPoll(browser, session, queue));
                try {
                    pollFuture.get(); // BLOCKS until StartPoll finishes
                } catch (ExecutionException e) {
                    System.err.println("Error during poll execution: " + e.getCause());
                }

            } catch (JMSException e) {
                System.err.println("Session/Connection error: " + e.getMessage());
                cleanupConnection();

                // short delay before retry
                sleepSilently(5000);

            } catch (Exception e) {
                e.printStackTrace();
            }

            // Wait PollInterval seconds AFTER poll completion
            sleepSilently(PollInterval * 1000L);
        }

        // Cleanup on thread exit
        cleanupConnection();
    }

    private void cleanupConnection() {
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
        session = null;
        connection = null;
    }

    private void sleepSilently(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Create a JMS connection. Implement your connection/retry logic here.
     */
    private Connection createConnectionWithRetry() throws JMSException {
        // Example: ActiveMQConnectionFactory connectionFactory = ...
        int retryCount = 0;
        int maxRetries = 10; // or Integer.MAX_VALUE for infinite
        int retryIntervalSec = 10;
        try {
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
        } catch (Exception ex) {
            // return connectionFactory.createConnection();
            throw new UnsupportedOperationException("Implement your connection creation here");
        }
    }

    /**
     * StartPoll must be implemented by child class.
     * It may be synchronous or asynchronous internally.
     * Future.get() in the polling loop will wait until all work is done.
     */
    protected abstract void StartPoll(QueueBrowser browser, Session session, Queue queue);
    // private void StartPoll(QueueBrowser browser, Session session, Queue queue) {
    // Your existing poll logic override in child
    // }

    public void ReadMessages(String queueid) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                System.out.println("Polling queue messages...");

                if (connection == null) {
                    connection = createConnectionWithRetry();
                    session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                }

                queue = session.createQueue(queueid);
                browser = session.createBrowser(queue);

                // Blocking poll — ensures next iteration only happens after completion
                StartPoll(browser, session, queue);

            } catch (JMSException e) {
                System.err.println("Session/Connection error: " + e.getMessage());
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
                session = null;
                connection = null;

                // short delay before retrying
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // wait interval before next poll
            try {
                Thread.sleep(PollInterval * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Cleanup on exit
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
        System.out.println("SHAREPOINT queue message processed");

    }


    public void consumeMessageById(String messageId, String queueid) throws JMSException {
        String selector = "JMSMessageID = '" + messageId + "'";
        // Check session is open
        MessageConsumer consumer = null;

        try {
            if (session instanceof ActiveMQSession amqSession) {
                if (amqSession.isClosed()) {
                    connection = createConnectionWithRetry();
                }
            }
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            queue = session.createQueue(queueid);
            consumer = session.createConsumer(queue, selector);
            Message msg = consumer.receive(1000);
            if (msg != null) {
                session.commit(); // remove the message
                System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_BRIGHT_GREEN
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