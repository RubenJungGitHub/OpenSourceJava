package contain.opensource.ils.bs.receiver.classes;
import  org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import contain.opensource.ils.bs.receiver.classes.MessageQueueHandling.MessageBrowserPoll;

@Component
public class StartupRunner implements CommandLineRunner {

    private final MessageBrowserPoll poll;

    public StartupRunner(MessageBrowserPoll poll) {
        this.poll = poll;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("StartupRunner: launching message polling...");

        // Run polling in a background thread to avoid blocking Spring
        new Thread(() -> poll.ReadMessages(args)).start();
    }
}
