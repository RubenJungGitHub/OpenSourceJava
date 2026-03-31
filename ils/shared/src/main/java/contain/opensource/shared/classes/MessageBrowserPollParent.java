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
            ObjectMapper mapper,
            JmsTemplate jmsTemplate) {

        super(activeMQProps, alfrescoProps, ilsproperties, mapper, jmsTemplate);
    }

    public void SendMigrationMessage(String weburl, String id, String deltalink, String platformfrom,
            Hashtable<String, String> migrateinfo) {
        try {
            ActiveMQConnectionFactory migfactory = new ActiveMQConnectionFactory(
                    activeMQProps.getMigrationHub().getUser(), activeMQProps.getMigrationHub().getPassword(),
                    activeMQProps.getMigrationHub().getBrokerUrl());
            Connection migconnection = migfactory.createConnection();
            migconnection.start();
            Session migsession = migconnection.createSession(true, Session.SESSION_TRANSACTED);

            // Create a message producer
            Queue migqueue = migsession.createQueue(activeMQProps.getMigrationHub().getMigrationQueue());
            MessageProducer producer = migsession.createProducer(migqueue);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            MigrationQueueMessage payload = new MigrationQueueMessage(weburl, "Migrate", platformfrom,
                    id, deltalink, migrateinfo.get("platformto"), migrateinfo.get("containerto"));
            String json = objectMapper.writeValueAsString(payload);
            String correlationId = MDC.get("correlationId");
            TextMessage message = session.createTextMessage(json);

            // Send the message
            producer.send(message);

            // Clean up
            producer.close();
            migsession.commit();
            migsession.close();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            String timestamp = ZonedDateTime.now(ZoneId.of("Europe/Amsterdam")).toLocalDateTime().format(formatter);
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BRIGHT_GREEN
                    + timestamp + "-> Information object " + weburl + " sent to migrationqueue"
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);

        } catch (Exception ex) {
            // log.error("Failed to send delta message to ActiveMQ", ex);
            throw new IllegalStateException("Failed to send migration message to ActiveMQ", ex);
        }
    }
}