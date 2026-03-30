package contain.opensource.ils.bs.migrationpoller;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import contain.opensource.ils.bs.receiver.classes.redis.RedisConfigProperties;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ActiveMQProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {
    "contain.opensource.ils.bs.migrationpoller", // De poller logica zelf
    "contain.opensource.shared"           // Voor de shared config/properties
})
@EnableConfigurationProperties({ ActiveMQProperties.class, ILSRestProperties.class, AlfrescoProperties.class})

public class StartMigrationPoller {

    // FORCEER DE BEAN HIER HANDMATIG
    @Bean
    public RedisConfigProperties redisConfigProperties() {
        return new RedisConfigProperties();
    }

    public static void main(String[] args) {
              ConfigurableApplicationContext context = SpringApplication.run(StartMigrationPoller.class, args);
    }
}