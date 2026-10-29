package contain.opensource.ils.bs.alfrescopoller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ActiveMQProperties;

@SpringBootApplication(scanBasePackages = {
        "contain.opensource.ils.bs.alfrescopoller",
        "contain.opensource.shared"
})
@EnableConfigurationProperties({ ActiveMQProperties.class, ILSRestProperties.class, AlfrescoProperties.class })
public class StartAlfrescoPoller {

    private static final Logger log = LoggerFactory.getLogger(StartAlfrescoPoller.class);

    public static void main(String[] args) {
        System.out.println(">>> STARTING ALFRESCO QUEUE POLLER V1.0 <<<");                
        ConfigurableApplicationContext context = SpringApplication.run(StartAlfrescoPoller.class, args);
    }
}