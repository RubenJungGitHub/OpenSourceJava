package contain.opensource.ils.bs.sppoller.classes.messagequeuehandling;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import contain.opensource.ils.bs.receiver.classes.Logger.IOLog;
import contain.opensource.ils.bs.receiver.classes.migration.MessageBrowserPollParent;
import contain.opensource.ils.bs.receiver.classes.sharepoint.SharepointQueMessage;
import contain.opensource.ils.bs.receiver.services.GraphService;
import contain.opensource.ils.bs.receiver.services.migrationservice;
import contain.opensource.shared.configurationproperties.ActiveMQProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.shared.constants.AlfrescoConstants;

@Component
public class MessageBrowserPollSP extends MessageBrowserPollParent{
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private GraphService graphservice;

    @Autowired
    public MessageBrowserPollSP(ActiveMQProperties activeMQProps,
            AlfrescoProperties alfrescoProps,
            ILSRestProperties ilsProperties,
            ObjectMapper objectMapper,
            JmsTemplate jmsTemplate,
            migrationservice migrationService,
            GraphService graphService) {
        super(
            activeMQProps,
            alfrescoProps,
            ilsProperties,
            objectMapper,
            jmsTemplate,
            migrationService
        );
        this.graphservice = graphService;
    }

    @Override
    public void StartPoll(QueueBrowser browser, Session session, Queue queue) {
        try {
            timestamp = LocalDateTime.now().format(formatter);
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_GREEN
                    + timestamp + " -> New SHAREPOINT poll loop on broker : " + activeMQProps.getBrokerUrl()
                    + " on queue " + activeMQProps.getSharepointQueue() + ". Interval : " + PollInterval + " seconds"
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
            String json = "";
            int count = 0;
            ObjectMapper mapper = new ObjectMapper();
            Enumeration<?> messages = browser.getEnumeration();
            while (messages.hasMoreElements()) {
                Message msg = (Message) messages.nextElement();
                count++;
                System.out.println("Processing " + msg + " message # " + count + " from queue");
                try {
                    if (msg instanceof TextMessage) {
                        json = ((TextMessage) msg).getText();
                        // TextMessage text = (TextMessage) msg;
                        try {
                            System.out.println("RAW JSON: " + json);
                            SharepointQueMessage message = mapper.readValue(json, SharepointQueMessage.class);

                            for (SharepointQueMessage.Item item : message.getItems()) {
                                System.out.println("Item ID: " + item.getId());
                                if (item.getParentReference() != null) {
                                    System.out.println("Site ID: " + item.getParentReference().getSiteId());
                                }
                                if (item.getFields() != null) {
                                    System.out.println("Fields: " + item.getFields());
                                }
                                String deltaLink = message.getDeltaLink().split("\\?")[0];
                                if (item.getDeleted() != null) {

                                    String action = "IO  " + item.getId() + " deleted from platform";
                                    IOLog.log(
                                            "DeletedFromPlatform",
                                            item.getId(),
                                            "DeletedFromPlatform",
                                            action,
                                            AlfrescoConstants.ContainPlatforms.SPO.toString(),
                                            AlfrescoConstants.ContainPlatforms.SPO.toString(),
                                            "DeletedFromPlatform",
                                            "DeletedFromPlatform",
                                            deltaLink,
                                            AlfrescoConstants.eActionPerformed.IODELETED,
                                            "<Unknown>",
                                            "DeletedFromPlatform",
                                            "DeletedFromPlatform",
                                            "DeletedFromPlatform");
                                } else {
                                    boolean migrate = this.graphservice.ProcessChangedSharepointItem(item.getWebUrl(),
                                            item.getId(), deltaLink);
                                    if (migrate) {
                                        SendMigrationMessage(item, deltaLink);
                                    }
                                }
                            }
                            consumeMessageById(msg.getJMSMessageID(), activeMQProps.getSharepointQueue());

                        } catch (JMSException processingError) {
                            System.err.println("Error while processing message" +processingError );
                            processingError.printStackTrace();
                        }

                    }
                } catch (JMSException e) {
                    System.err.println("Error polling the queue:");
                    e.printStackTrace();
                }

            }
        } catch (Exception e) {
            System.err.println("Error in StartPoll:");
            e.printStackTrace();
        }
    }
}