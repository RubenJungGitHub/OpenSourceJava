package contain.opensource.ils.bs.alfrescopoller;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import contain.opensource.ils.bs.receiver.classes.redis.RedisConfigProperties;
import contain.opensource.ils.bs.receiver.classes.redis.RedisManager;

@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class, 
    HibernateJpaAutoConfiguration.class 
})
@EnableMongoRepositories(basePackages = "contain.opensource.ils.bs.receiver.Interfaces")
@ComponentScan(basePackages = {
    "contain.opensource.uuidutil",
    "contain.opensource.ils.bs.receiver",
    "contain.opensource.ils.bs.alfrescopoller" 
})
public class StartAlfrescoPoller {

    // FORCEER DE BEAN HIER HANDMATIG
    @Bean
    public RedisConfigProperties redisConfigProperties() {
        return new RedisConfigProperties();
    }

    public static void main(String[] args) {

        ConfigurableApplicationContext context = SpringApplication.run(StartAlfrescoPoller.class, args);
        
        // Nu MOET hij hem vinden, want we hebben hem hierboven zelf gedefinieerd
        RedisConfigProperties redisConfig = context.getBean(RedisConfigProperties.class);
        RedisManager.init(redisConfig);
    }
}