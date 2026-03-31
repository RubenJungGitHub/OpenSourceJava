package contain.opensource.ils.bs.sppoller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import contain.opensource.ils.bs.sppoller.classes.messagequeuehandling.MessageBrowserPollSP;
import contain.opensource.shared.configurationproperties.ActiveMQProperties;

@Component
public class StartupRunner implements CommandLineRunner {

    private MessageBrowserPollSP SPPoll;

    @Autowired
    private ActiveMQProperties MQProps;

    public StartupRunner(MessageBrowserPollSP SPPoll) {
        this.SPPoll = SPPoll;
    }

    @Override
    public void run(String... args) throws Exception {
        SPPoll.startPolling(MQProps.getMigrationHub().getSharepointQueue());
    }
}
