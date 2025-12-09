package contain.opensource.java.helloworld.classes;

import javax.jms.*;
/*import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.Enumeration;
import javax.jms.QueueBrowser;
*/
import org.apache.activemq.ActiveMQConnectionFactory;

public class MessageBrowserPoll {

    public static void ReadMessages(String[] args) {
        Connection connection = null;
        Session session = null;
        MessageConsumer consumer = null;
        try {
            // Connect to ActiveMQ
            ConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
            connection = factory.createConnection("admin", "admin");
            connection.start();

            // Create a session
            // session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

            // The queue you want to inspect
            Queue queue = session.createQueue("Consumer.MyJavaConsumer.VirtualTopic.alfresco.repo.events.nodes");
            consumer = session.createConsumer(queue); // ✅ Create a consumer
            // Create a QueueBrowser (does NOT consume)
            // QueueBrowser browser = session.createBrowser(queue);

   
            while (true) {
                try {
                    //To be replaced with logger
                    System.out.println("Sleeping 5 seconds...");
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    System.err.println("Sleep was interrupted");
                    e.printStackTrace();
                    // optional: break the loop if interrupted
                    break;
                }
                // Message msg = consumer.receiveNoWait();
                System.out.println("Browsing messages in queue: " + queue.getQueueName());
                StartPoll(consumer);
            }

            // Cleanup
            // browser.close();
            session.close();
            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (consumer != null)
                    consumer.close();
            } catch (Exception ignored) {
            }
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
        }
    }

    public static void StartPoll(MessageConsumer consumer) {
        try {
            Message msg;
            while ((msg = consumer.receiveNoWait()) != null) {
                try {
                    final String RED = "\u001B[31m";
                    final String RESET = "\u001B[0m";
                    if (msg instanceof TextMessage) {
                        TextMessage text = (TextMessage) msg;
                        System.out.println(RED + "Processing message: " + text.getText() + RESET);
                    } else {
                        System.out.println(RED + "Processing non-text message: " + msg + RESET);
                    }

                    msg.acknowledge(); // only removes message after successful processing
                    System.out.println("Message acknowledged (removed from queue).");

                } catch (JMSException processingError) {
                    System.err.println("Error while processing message, NOT acknowledging.");
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
