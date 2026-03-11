package contain.opensource.ils.bs.migrationpoller;
import org.springframework.beans.factory.annotation.Autowired;
import  org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import contain.opensource.ils.bs.migrationpoller.classes.messagequeuehandling.MessageBrowserPollMigration;
import contain.opensource.shared.configurationproperties.ActiveMQProperties;

@Component
public class StartupRunner implements CommandLineRunner {

    private final MessageBrowserPollMigration MigrationPoll;

    @Autowired
    private ActiveMQProperties MQProps;

    public StartupRunner(MessageBrowserPollMigration MigrationPoll) {
        this.MigrationPoll = MigrationPoll;
    }


    @Override
    public void run(String... args) throws Exception {
    MigrationPoll.startPolling(MQProps.getMigrationqueue());
    }
}
