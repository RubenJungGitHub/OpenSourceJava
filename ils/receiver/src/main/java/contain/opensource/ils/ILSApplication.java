package contain.opensource.ils;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

import contain.opensource.ils.bs.receiver.classes.Binding.PKCS12KeyLoader;
import contain.opensource.ils.bs.receiver.classes.Binding.TestCreateSelfGeneratedCertificate;
import contain.opensource.ils.bs.receiver.classes.Redis.RedisManager;
import contain.opensource.shared.configurationproperties.ActiveMQProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ILSRestProperties;


@SpringBootApplication(scanBasePackages = {
        "contain.opensource.uuidutil",           // main app
        "contain.opensource.uuidutil.controllers",   // your controllers package
        "contain.opensource.ils.shared",
        "contain.opensource.shared.configurationproperties"
})
	@ComponentScan(basePackages = "contain.opensource.ils") 
	@EnableConfigurationProperties({ActiveMQProperties.class, AlfrescoProperties.class,ILSRestProperties.class})
	@ConfigurationPropertiesScan
	public class ILSApplication {
	//private static final Logger log = LoggerFactory.getLogger(ILSApplication.class);

	public static void main(String[] args) {
      //log.info("Main started");

		//GenerateKeyPair();
		RedisManager.init();
		try {
			PKCS12KeyLoader.loadPrivateKey();
		} catch (Exception e) {
			e.printStackTrace();
		}
		SpringApplication.run(ILSApplication.class, args);
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