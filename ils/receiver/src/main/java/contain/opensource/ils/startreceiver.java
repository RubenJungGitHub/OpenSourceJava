package contain.opensource.ils;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

import contain.opensource.ils.bs.receiver.classes.Binding.TestCreateSelfGeneratedCertificate;
import contain.opensource.ils.bs.receiver.classes.Redis.RedisConfigProperties;
import contain.opensource.ils.bs.receiver.classes.Redis.RedisManager;
import contain.opensource.shared.configurationproperties.ActiveMQProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ILSRestProperties;

@SpringBootApplication(scanBasePackages = {
		"contain.opensource.ils.receiver.sharepoint", // only SP-related classes
		"contain.opensource.ils.receiver.services", // GraphService
		"contain.opensource.shared.configurationproperties",
		"contain.opensource.uuidutil"
})
@ComponentScan(basePackages = "contain.opensource.ils")
@EnableConfigurationProperties({ ActiveMQProperties.class, AlfrescoProperties.class, ILSRestProperties.class,
		RedisConfigProperties.class })
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
		// try {
		// PKCS12KeyLoader.loadPrivateKey();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
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