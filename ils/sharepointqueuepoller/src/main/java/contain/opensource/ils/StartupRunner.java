package contain.opensource.ils;
import  org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


import contain.opensource.ils.bs.receiver.classes.MessageQueueHandling.MessageBrowserPollSP;

@Component
public class StartupRunner implements CommandLineRunner {

    private final MessageBrowserPollSP SPPoll;

    public StartupRunner(MessageBrowserPollSP SPPoll) {
        this.SPPoll = SPPoll;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("StartupRunner: launching message polling...");

        // Run polling in a background thread to avoid blocking Spring
       new Thread(() -> SPPoll.ReadMessages(args)).start();
    }
}
