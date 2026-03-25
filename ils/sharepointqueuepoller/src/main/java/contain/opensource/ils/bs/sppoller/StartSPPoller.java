package contain.opensource.ils.bs.sppoller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ActiveMQProperties;

@SpringBootApplication(scanBasePackages = {
    "contain.opensource.ils.bs.sppoller", // De poller logica zelf
    "contain.opensource.shared"           // Voor de shared config/properties
})
@EnableConfigurationProperties({ ActiveMQProperties.class, ILSRestProperties.class, AlfrescoProperties.class})
public class StartSPPoller {

    public static void main(String[] args) {
        // Dit is alles wat je nodig hebt om Spring te starten
        SpringApplication.run(StartSPPoller.class, args);
        
        System.out.println("SP Poller is nu actief en luistert naar de queue...");
    }
}