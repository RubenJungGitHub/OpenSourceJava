package contain.opensource.ils.bs.receiver.classes;
import  org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import contain.opensource.ils.bs.receiver.classes.MessageQueueHandling.MessageBrowserPollAlfresco;
import contain.opensource.ils.bs.receiver.classes.MessageQueueHandling.MessageBrowserPollSP;

@Component
public class StartupRunner implements CommandLineRunner {

    private final MessageBrowserPollAlfresco AlfrescoPoll;
    private final MessageBrowserPollSP SPPoll;

    public StartupRunner(MessageBrowserPollAlfresco AlfrescoPoll,MessageBrowserPollSP SPPoll) {
        this.AlfrescoPoll = AlfrescoPoll;
        this.SPPoll = SPPoll;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("StartupRunner: launching message polling...");

        // Run polling in a background thread to avoid blocking Spring
       new Thread(() -> AlfrescoPoll.ReadMessages(args)).start();
       new Thread(() -> SPPoll.ReadMessages(args)).start();
    }
}
