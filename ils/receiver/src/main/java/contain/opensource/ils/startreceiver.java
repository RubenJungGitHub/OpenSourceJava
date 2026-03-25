package contain.opensource.ils;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import contain.opensource.ils.bs.receiver.classes.Binding.TestCreateSelfGeneratedCertificate;
import contain.opensource.ils.bs.receiver.classes.redis.RedisConfigProperties;
import contain.opensource.ils.bs.receiver.classes.redis.RedisManager;
import contain.opensource.shared.configurationproperties.ActiveMQProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ILSRestProperties;

@SpringBootApplication(
    scanBasePackages = {
        "contain.opensource.ils",
        "contain.opensource.shared",
        "contain.opensource.uuidutil"
    },
    exclude = { 
        DataSourceAutoConfiguration.class, 
        HibernateJpaAutoConfiguration.class 
    }
)
//@ComponentScan(basePackages = "contain.opensource.ils")
@EnableConfigurationProperties({ ActiveMQProperties.class, AlfrescoProperties.class, ILSRestProperties.class, RedisConfigProperties.class })
@ConfigurationPropertiesScan
public class startreceiver {
	// private static final Logger log =
	// LoggerFactory.getLogger(ILSApplication.class);

	public static void main(String[] args) {
		// log.info("Main started");
		var context = SpringApplication.run(startreceiver.class, args);
		RedisConfigProperties redisConfig = context.getBean(RedisConfigProperties.class);
		// GenerateKeyPair();
		RedisManager.init(redisConfig);
		System.out.println("Listening.....");
	}

	private static void GenerateKeyPair() {
		TestCreateSelfGeneratedCertificate createKeyPair = new TestCreateSelfGeneratedCertificate();
		try {
			createKeyPair.Generate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}