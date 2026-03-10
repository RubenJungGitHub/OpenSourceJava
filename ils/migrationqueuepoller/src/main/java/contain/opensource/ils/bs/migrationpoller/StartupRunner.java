package contain.opensource.ils.bs.migrationpoller;
import  org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import contain.opensource.ils.bs.migrationpoller.classes.messagequeuehandling.MessageBrowserPollMigration;

@Component
public class StartupRunner implements CommandLineRunner {

    private final MessageBrowserPollMigration MigrationPoll;

    public StartupRunner(MessageBrowserPollMigration MigrationPoll) {
        this.MigrationPoll = MigrationPoll;
    }


    @Override
    public void run(String... args) throws Exception {
       
       /*
        System.out.println("✅ StartupRunner run() called!");
        new Thread(() -> {
            try {
                SPPoll.ReadMessages();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "SPPoller-Thread").start();
    }
    */

    MigrationPoll.startPolling();
    }
}
