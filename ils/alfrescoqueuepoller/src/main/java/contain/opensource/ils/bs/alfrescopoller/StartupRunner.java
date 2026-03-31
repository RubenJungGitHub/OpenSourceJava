package contain.opensource.ils.bs.alfrescopoller;
import org.springframework.beans.factory.annotation.Autowired;
import  org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import contain.opensource.ils.bs.alfrescopoller.classes.messagequeuehandling.MessageBrowserPollAlfresco;
import contain.opensource.shared.configurationproperties.ActiveMQProperties;

@Component
public class StartupRunner implements CommandLineRunner {

    private final MessageBrowserPollAlfresco AlfrescoPoll;

    public StartupRunner(MessageBrowserPollAlfresco AlfrescoPoll) {
        this.AlfrescoPoll = AlfrescoPoll;
    }

    @Autowired
    private ActiveMQProperties MQProps;

    @Override
    public void run(String... args) throws Exception {
       
    AlfrescoPoll.startPolling();
    }
}
