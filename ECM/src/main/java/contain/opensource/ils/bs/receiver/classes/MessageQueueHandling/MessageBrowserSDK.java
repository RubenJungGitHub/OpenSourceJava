package contain.opensource.ils.bs.receiver.classes.MessageQueueHandling;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

public class MessageBrowserSDK {
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

            // QueueBrowser browser = session.createBrowser(queue);
            // java.util.Enumeration msgs = browser.getEnumeration();

            /*
             * while (msgs.hasMoreElements()) {
             * Message msg = (Message) msgs.nextElement();
             * if (msg instanceof TextMessage) {
             * TextMessage textMsg = (TextMessage) msg;
             * System.out.println("Message: " + textMsg.getText());
             * } else {
             * System.out.println("Non-text message: " + msg);
             * }
             * }
             * 
             * browser.close();
             */
            session.close();
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
