package contain.opensource.ils.bs.migrationpoller;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ActiveMQProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import contain.opensource.ils.bs.receiver.classes.redis.RedisConfigProperties;
import contain.opensource.ils.bs.receiver.classes.redis.RedisManager;

@SpringBootApplication(scanBasePackages = {
    "contain.opensource.ils.bs.sppoller", // De poller logica zelf
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