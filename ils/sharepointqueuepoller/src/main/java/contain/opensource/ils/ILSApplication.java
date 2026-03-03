package contain.opensource.ils;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

import contain.opensource.shared.configurationproperties.ActiveMQProperties;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.shared.configurationproperties.ILSRestProperties;


@SpringBootApplication(scanBasePackages = {
		"contain.opensource.uuidutil", // main app
		"contain.opensource.uuidutil.controllers", // your controllers package
		"contain.opensource.ils.shared",
		"contain.opensource.shared.configurationproperties",
		"contain.opensource.shared.receiver"
})
//@ComponentScan(basePackages = "contain.opensource.ilsreceiver", excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = contain.opensource.ils.bs.receiver.classes.Redis.RedisManager.class))
@ComponentScan(basePackages = "contain.opensource.ils")

@EnableConfigurationProperties({ ActiveMQProperties.class, AlfrescoProperties.class, ILSRestProperties.class })
@ConfigurationPropertiesScan
public class ILSApplication {

	public static void main(String[] args) {
		try {
		} catch (Exception e) {
			e.printStackTrace();
		}
		SpringApplication.run(ILSApplication.class, args);
	}
}