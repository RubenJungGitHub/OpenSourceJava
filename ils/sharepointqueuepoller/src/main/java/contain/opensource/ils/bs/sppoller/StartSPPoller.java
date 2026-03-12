package contain.opensource.ils.bs.sppoller;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import contain.opensource.ils.bs.receiver.classes.Redis.RedisConfigProperties;
import contain.opensource.ils.bs.receiver.classes.Redis.RedisManager;
import contain.opensource.ils.startreceiver;

@SpringBootApplication(scanBasePackages = {
        "contain.opensource.uuidutil",
        "contain.opensource.uuidutil.controllers",
        "contain.opensource.ils.bs.receiver",
        "contain.opensource.ils.bs.sppoller" 
})
@ConfigurationPropertiesScan(basePackages = "contain.opensource.shared.configurationproperties")
@EnableJpaRepositories(basePackages = "contain.opensource.ils.bs.receiver.Interfaces")
@EntityScan(basePackages = "contain.opensource.ils.bs.receiver.classes.Logger")

public class StartSPPoller {

    public static void main(String[] args) {

		var context = SpringApplication.run(startreceiver.class, args);
		RedisConfigProperties redisConfig = context.getBean(RedisConfigProperties.class);
		// GenerateKeyPair();
		RedisManager.init(redisConfig);
    }
}