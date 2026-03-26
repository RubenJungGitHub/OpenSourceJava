package contain.opensource.ils.bs.migrationpoller;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

import contain.opensource.ils.bs.receiver.classes.redis.RedisConfigProperties;
import contain.opensource.ils.bs.receiver.classes.redis.RedisManager;
import contain.opensource.shared.configurationproperties.ActiveMQProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ILSRestProperties;

@SpringBootApplication(scanBasePackages = {
    "contain.opensource.ils.bs.migrationpoller", // De poller logica zelf
    "contain.opensource.shared"           // Voor de shared config/properties
})
@EnableConfigurationProperties({ ActiveMQProperties.class, ILSRestProperties.class, AlfrescoProperties.class})
public class StartMigrationPoller {

    public static void main(String[] args) {

        ConfigurableApplicationContext context = SpringApplication.run(StartMigrationPoller.class, args);
        
        // Nu MOET hij hem vinden, want we hebben hem hierboven zelf gedefinieerd
        RedisConfigProperties redisConfig = context.getBean(RedisConfigProperties.class);
        RedisManager.init(redisConfig);
    }
}