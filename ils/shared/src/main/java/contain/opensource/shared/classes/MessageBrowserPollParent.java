package contain.opensource.shared.classes;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Hashtable;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.MDC;
import org.springframework.jms.core.JmsTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import contain.opensource.shared.configurationproperties.ActiveMQProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import jakarta.jms.DeliveryMode;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.Connection;
import jakarta.jms.TextMessage;
import jakarta.jms.Queue;

public abstract class MessageBrowserPollParent extends MessageBrowserPollParentMigration {

    public MessageBrowserPollParent(
            ActiveMQProperties activeMQProps,
            AlfrescoProperties alfrescoProps,
            ILSRestProperties ilsproperties,
            ObjectMapper mapper, // Named 'mapper' here
            JmsTemplate jmsTemplate,
            String queuetopoll,
            ActiveMQProperties.BrokerConfig specificSource) {

        super(activeMQProps, alfrescoProps, ilsproperties, mapper, jmsTemplate, queuetopoll, specificSource);

    }

    public void SendMigrationMessage(String weburl, String id, String deltalink, String platformfrom,
            Hashtable<String, String> migrateinfo) {
        // Use the 'migconnection' and 'migsession' you created locally
        try (Connection migconnection = createConnectionFromConfig(activeMQProps.getMigrationHub())) {
            migconnection.start();
            try (Session migsession = migconnection.createSession(true, Session.SESSION_TRANSACTED)) {

                Queue migqueue = migsession.createQueue(activeMQProps.getMigrationHub().getMigrationQueue());
                MessageProducer producer = migsession.createProducer(migqueue);
                producer.setDeliveryMode(DeliveryMode.PERSISTENT);

                MigrationQueueMessage payload = new MigrationQueueMessage(weburl, "Migrate", deltalink, platformfrom,
                        id, migrateinfo.get("platformto"), migrateinfo.get("containerto"));

                // Ensure this variable name matches what's in the Grandparent
                String json = this.objectMapper.writeValueAsString(payload);

                // Use 'migsession' here, NOT 'session'
                TextMessage message = migsession.createTextMessage(json);

                producer.send(message);

                producer.close();
                migsession.commit();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                String timestamp = ZonedDateTime.now(ZoneId.of("Europe/Amsterdam")).toLocalDateTime().format(formatter);
                String feedback = timestamp + " -> Information object queued for migration";
                System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_BRIGHT_CYAN
                        + feedback
                        + contain.opensource.shared.constants.AlfrescoConstants.RESET);
            }
            // Add your logging here
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to send migration message to ActiveMQ", ex);
        }
    }

    // Helper to keep the try-with-resources clean
    private Connection createConnectionFromConfig(ActiveMQProperties.BrokerConfig config) throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(
                config.getUser(), config.getPassword(), config.getBrokerUrl());
        return factory.createConnection();
    }
}