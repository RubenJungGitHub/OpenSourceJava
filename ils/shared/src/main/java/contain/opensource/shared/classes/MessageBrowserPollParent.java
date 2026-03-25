package contain.opensource.shared.classes;

import jakarta.jms.DeliveryMode;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import java.util.Hashtable;
// ... and any other javax.jms imports;

import org.slf4j.MDC;
import org.springframework.jms.core.JmsTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

//import contain.opensource.ils.bs.receiver.classes.migration.MessageBrowserPollParentMigration;
//import contain.opensource.ils.bs.receiver.classes.migration.MigrationQueueMessage;/
import contain.opensource.shared.configurationproperties.ActiveMQProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ILSRestProperties;

public abstract class MessageBrowserPollParent extends MessageBrowserPollParentMigration {

    
    public MessageBrowserPollParent(
            ActiveMQProperties activeMQProps,
            AlfrescoProperties alfrescoProps,
            ILSRestProperties ilsProperties,
            ObjectMapper mapper,
            JmsTemplate jmsTemplate
            ) {

        super(activeMQProps, alfrescoProps, ilsProperties, mapper, jmsTemplate);
    }
    public void SendMigrationMessage(SharepointQueMessage.Item item, String deltalink,  String platformfrom,  Hashtable<String, String> migrateinfo ) {
        try {

            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            // Create a message producer
            queue = session.createQueue(activeMQProps.getMigrationqueue());
            MessageProducer producer = session.createProducer(queue);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            MigrationQueueMessage payload = new MigrationQueueMessage(item.getWebUrl(), "Migrate", platformfrom, 
                    item.getFields().get("id").toString(), deltalink, migrateinfo.get("platformto"), migrateinfo.get("containerto"));
            String json = objectMapper.writeValueAsString(payload);
            String correlationId = MDC.get("correlationId");
            TextMessage message = session.createTextMessage(json);

            // Send the message
            producer.send(message);

            // Clean up
            producer.close();
            session.commit();
            session.close();
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BRIGHT_GREEN
                    + timestamp + "-> Information object " + item + " sent to migrationqueue"
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);

        } catch (Exception ex) {
            // log.error("Failed to send delta message to ActiveMQ", ex);
            throw new IllegalStateException("Failed to send migration message to ActiveMQ", ex);
        }
    }
}