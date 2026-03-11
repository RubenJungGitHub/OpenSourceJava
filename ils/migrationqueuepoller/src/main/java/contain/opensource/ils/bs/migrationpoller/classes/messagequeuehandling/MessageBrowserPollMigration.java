package contain.opensource.ils.bs.migrationpoller.classes.messagequeuehandling;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;

import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import contain.opensource.ils.bs.receiver.classes.migration.MessageBrowserPollParentMigration;
import contain.opensource.ils.bs.receiver.classes.migration.MigrationQueueMessage;
import contain.opensource.ils.bs.receiver.services.migrationservice;
import contain.opensource.shared.configurationproperties.ActiveMQProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ILSRestProperties;

@Component
public class MessageBrowserPollMigration extends MessageBrowserPollParentMigration {
    private migrationservice migrationservice;

    public MessageBrowserPollMigration(
            ActiveMQProperties activeMQProps,
            AlfrescoProperties alfrescoProps,
            ILSRestProperties ilsProperties,
            ObjectMapper objectMapper,
            JmsTemplate jmsTemplate,
            migrationservice migservice) {

        super(
                activeMQProps,
                alfrescoProps,
                ilsProperties,
                objectMapper,
                jmsTemplate);
        this.migrationservice = migservice;
    }

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    @Override
    public void StartPoll(QueueBrowser browser, Session session, Queue queue) {
        try {
            timestamp = LocalDateTime.now().format(formatter);
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_BRIGHT_CYAN
                    + timestamp + " -> New MIGRATION poll loop on broker : " + browser
                    + " on queue " + queue + ". Interval : " + PollInterval + " seconds"
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
            String json = "";
            int count = 0;
            ObjectMapper mapper = new ObjectMapper();
            Enumeration<?> messages = browser.getEnumeration();

            while (messages.hasMoreElements()) {
                count++;
                Message msg = (Message) messages.nextElement();
                System.out.println(contain.opensource.shared.constants.AlfrescoConstants.YELLOW
                        + timestamp + " -> Processing  message # " + count + " " + msg + "from queue"
                        + contain.opensource.shared.constants.AlfrescoConstants.RESET);
                try {
                    if (msg instanceof TextMessage) {
                        // Process migration
                        json = ((TextMessage) msg).getText();
                        MigrationQueueMessage queueMessage =mapper.readValue(json, MigrationQueueMessage.class);

                        //THE MOVETO NEEDS TO BE EVALUATED LATER. FOR NOW ONLY ALFRESCO!
                         migrationservice.migrateNodeToAlfresco(queueMessage);

                        consumeMessageById(msg.getJMSMessageID(), activeMQProps.getMigrationqueue());
                        
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
}