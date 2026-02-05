package contain.opensource.ils.bs.receiver.classes.MessageQueueHandling;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * MessageBrowser is a Spring component that handles browsing and consuming
 * messages
 * from an ActiveMQ message queue.
 * 
 * This class provides functionality to connect to an ActiveMQ broker, create a
 * session,
 * and listen for messages from a specified queue destination. It supports
 * message
 * consumption with client acknowledgment mode.
 * 
 * The class is designed to work with ActiveMQ's virtual topics and consumer
 * groups,
 * allowing it to subscribe to and process messages asynchronously using a
 * message listener.
 * 
 * @author [Your Name]
 * @version 1.0
 * @since [Date]
 */
/**
 * MessageBrowser is a Spring component responsible for consuming messages from
 * an ActiveMQ message queue.
 * It provides functionality to read and process messages from a specified queue
 * destination.
 * 
 * 
 * This class uses ActiveMQ as the messaging provider and supports connection
 * pooling through
 * the injected {@link ConnectionFactory}.
 *
 * 
 * 
 * b>Usage:</b>
 *
 * 
 * pre>
 * MessageBrowser.ReadMessages(new String[] {});
 * pre>
 * 
 * 
 * b>Features:</b>
 *
 *
 * Connects to ActiveMQ broker running on localhost:61616
 * Authenticates with provided credentials (admin/admin)
 * Listens to messages from a VirtualTopic consumer queue
 * Processes TextMessage instances with a message listener callback
 * Uses CLIENT_ACKNOWLEDGE mode for manual message acknowledgment
 *
 * 
 * 
 * b>Note:</b> The current implementation creates a static method which
 * bypasses
 * the injected {@link ConnectionFactory}. Consider refactoring to use the
 * instance
 * field for better dependency management and testability.
 *
 * 
 * @see javax.jms.ConnectionFactory
 * @see javax.jms.Connection
 * @see javax.jms.Session
 * @see javax.jms.MessageConsumer
 */
@Component
public class MessageBrowser {

    private final ConnectionFactory connectionFactory;

    public MessageBrowser(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Reads messages from an ActiveMQ message queue and processes them
     * asynchronously.
     * 
     * This method establishes a connection to an ActiveMQ broker running on
     * localhost:61616,
     * creates a session and message consumer for a specific queue, and sets up a
     * message listener
     * to handle incoming messages. The method keeps the application running
     * indefinitely until
     * interrupted by the user (Ctrl+C).
     * 
     * <b>Connection Details:</b>
     *
     * Broker URL: tcp://localhost:61616
     * Username: admin
     * Password: admin
     * Acknowledgment Mode: CLIENT_ACKNOWLEDGE
     * Queue:
     * Consumer.01212fbf-b2c3-3779-8bda-19738e300ada.VirtualTopic.alfresco.repo.events.nodes
     *
     * 
     * <b>Message Processing:</b>
     *
     * TextMessage instances are logged with their content
     * Non-text messages are logged as generic messages
     * Exceptions during message processing are caught and printed to stderr
     *
     * 
     * @param args command-line arguments (currently unused)
     * @throws Exception if connection, session creation, or message consumption
     *                   fails
     * 
     * @see javax.jms.Connection
     * @see javax.jms.Session
     * @see javax.jms.MessageConsumer
     * @see javax.jms.TextMessage
     */
    public static void ReadMessages(String[] args) {
        try {

            ConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
            Connection connection = factory.createConnection("admin", "admin");
            // Connection connection = factory.createConnection();
            connection.start();

            // Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            // Queue queue =
            // session.createQueue("http://localhost:8161/admin/browse.jsp?JMSDestination=Consumer.01212fbf-b2c3-3779-8bda-19738e300ada.VirtualTopic.alfresco.repo.events.nodes");
            Queue queue = session.createQueue(
                    "Consumer.01212fbf-b2c3-3779-8bda-19738e300ada.VirtualTopic.alfresco.repo.events.nodes");

            MessageConsumer consumer = session.createConsumer(queue);

            System.out.println("Waiting for messages...");
            consumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        TextMessage textMessage = (TextMessage) message;
                        System.out.println("Message received: " + textMessage.getText());
                    } else {
                        System.out.println("Non-text message: " + message);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // Keep program alive
            System.out.println("Press Ctrl+C to exit");
            Thread.sleep(Long.MAX_VALUE);

            session.close();
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
