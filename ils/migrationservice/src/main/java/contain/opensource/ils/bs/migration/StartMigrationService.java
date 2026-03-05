package contain.opensource.ils.bs.migration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "contain.opensource.uuidutil",
        "contain.opensource.uuidutil.controllers",
        "contain.opensource.ils.bs.receiver",
        "contain.opensource.ils.bs.sppoller" 
})
@ConfigurationPropertiesScan(basePackages = "contain.opensource.shared.configurationproperties")
//@ComponentScan(basePackages = "contain.opensource.ils")
//@EnableConfigurationProperties({ ActiveMQProperties.class, AlfrescoProperties.class, ILSRestProperties.class }) -> From shared


// **Add these two lines**
@EnableJpaRepositories(basePackages = "contain.opensource.ils.bs.receiver.Interfaces")
@EntityScan(basePackages = "contain.opensource.ils.bs.receiver.classes.Logger")

public class StartMigrationService {
    public static void main(String[] args) {
        // Create a single orchestrator for the whole app
        //MigrationService migrationService = new MigrationService(5); // 5 worker threads

        // Pass the orchestrator to controllers
        //AlfrescoController alfrescoController = new AlfrescoController(migrationService);
        //SPController spController = new SPController(migrationService);

        // Start Spring context if you still need it
        //SpringApplication.run(AppConfig.class, args);

    }
}