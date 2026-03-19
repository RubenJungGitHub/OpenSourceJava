package contain.opensource.ils.bs.receiver.classes.migration;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.shared.constants.AlfrescoConstants;
@Configuration
public class KieServerConfig {

    @Autowired
    private ILSRestProperties ilsProperties; // Your existing properties class

    @Bean
    public KieServicesClient kieServicesClient() {
        // 1. Base URL only (no /containers)
        String serverUrl = "http://localhost:8180/kie-server/services/rest/server";
        
            KieServicesConfiguration config = KieServicesFactory.newRestConfiguration(
                    this.ilsProperties.getRuleenginecontainerendpoint(),
                    AlfrescoConstants.rhpamusername,
                    AlfrescoConstants.rhpampassword);

        // 2. Optimization: Use JSON and set a reasonable timeout
        config.setMarshallingFormat(MarshallingFormat.JSON);
        config.setTimeout(5000L); // 5 seconds

        // 3. This is the expensive part that now only happens ONCE
        return KieServicesFactory.newKieServicesClient(config);
    }
}