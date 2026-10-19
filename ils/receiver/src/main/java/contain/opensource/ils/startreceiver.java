package contain.opensource.ils;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

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
@EnableConfigurationProperties({ 
	ActiveMQProperties.class,
    AlfrescoProperties.class, 
    ILSRestProperties.class, 
    RedisConfigProperties.class 
})
public class startreceiver {

    public static void main(String[] args) {
        var context = SpringApplication.run(startreceiver.class, args);
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