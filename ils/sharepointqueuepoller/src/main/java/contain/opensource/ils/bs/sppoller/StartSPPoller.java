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
import org.springframework.context.annotation.FilterType;


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

@ComponentScan(
    basePackages = "contain.opensource.ils",
    excludeFilters = {
        // 1. Weg met de controllers (om poort-cloning te voorkomen)
        @ComponentScan.Filter(
            type = FilterType.ANNOTATION, 
            classes = org.springframework.web.bind.annotation.RestController.class
        ),
        // 2. Weg met de KIE configuratie
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE, 
            classes = contain.opensource.ils.bs.receiver.classes.migration.KieServerConfig.class
        ),
        // 3. NIEUW: Weg met de service die de KIE client nodig heeft
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE, 
            classes = contain.opensource.ils.bs.receiver.services.ruleengineservice.class
        )
    }
)
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