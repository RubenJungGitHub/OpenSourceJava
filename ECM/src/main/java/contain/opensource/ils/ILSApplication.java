package contain.opensource.ils;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import contain.opensource.ils.bs.receiver.classes.Binding.PKCS12KeyLoader;
import contain.opensource.ils.bs.receiver.classes.Binding.TestCreateSelfGeneratedCertificate;
import contain.opensource.ils.bs.receiver.classes.Redis.RedisManager;

@SpringBootApplication
@EnableConfigurationProperties(contain.opensource.ils.bs.receiver.classes.ConfigurationProperties.ActiveMQProperties.class)

public class ILSApplication {
	

	public static void main(String[] args) {
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