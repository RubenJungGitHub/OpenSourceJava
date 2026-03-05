package contain.opensource.ils.bs.alfrescopoller;
import  org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import contain.opensource.ils.bs.alfrescopoller.classes.messagequeuehandling.MessageBrowserPollAlfresco;

@Component
public class StartupRunner implements CommandLineRunner {

    private final MessageBrowserPollAlfresco AlfrescoPoll;

    public StartupRunner(MessageBrowserPollAlfresco AlfrescoPoll) {
        this.AlfrescoPoll = AlfrescoPoll;
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

    AlfrescoPoll.startPolling();
    }
}
