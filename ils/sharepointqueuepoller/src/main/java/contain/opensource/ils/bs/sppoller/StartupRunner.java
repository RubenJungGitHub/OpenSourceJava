package contain.opensource.ils.bs.sppoller;
import  org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import contain.opensource.ils.bs.sppoller.classes.messagequeuehandling.MessageBrowserPollSP;

@Component
public class StartupRunner implements CommandLineRunner {

    private final MessageBrowserPollSP SPPoll;

    public StartupRunner(MessageBrowserPollSP SPPoll) {
        this.SPPoll = SPPoll;
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

    SPPoll.startPolling();
    }
}
