package contain.opensource.ils.bs.sppoller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import contain.opensource.ils.bs.receiver.classes.redis.RedisConfigProperties;
import contain.opensource.ils.bs.receiver.classes.redis.RedisManager;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ActiveMQProperties;


@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class, 
    HibernateJpaAutoConfiguration.class 
})
@EnableMongoRepositories(basePackages = "contain.opensource.ils.bs.receiver.Interfaces")
// HIER horen de properties thuis:
@EnableConfigurationProperties({ 
    ILSRestProperties.class, 
    AlfrescoProperties.class,
    RedisConfigProperties.class,
    ActiveMQProperties.class 
})

@ComponentScan(basePackages = {
    "contain.opensource.uuidutil",
    "contain.opensource.ils.bs.receiver",
    "contain.opensource.ils.bs.sppoller" ,
    "contain.opensource.shared"
})
public class StartSPPoller {

        // FORCEER DE BEAN HIER HANDMATIG
 //   @Bean
 //   public RedisConfigProperties redisConfigProperties() {
  //      return new RedisConfigProperties();
  //  }

    public static void main(String[] args) {
        // Run THIS class, not the startreceiver class from the other module
        ConfigurableApplicationContext context = SpringApplication.run(StartSPPoller.class, args);
        
        // Now that the context is started, initialize Redis
        RedisConfigProperties redisConfig = context.getBean(RedisConfigProperties.class);
        RedisManager.init(redisConfig);
    }
}