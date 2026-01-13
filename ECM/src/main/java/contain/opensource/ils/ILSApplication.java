package contain.opensource.ils;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import contain.opensource.ils.bs.receiver.classes.Redis.RedisManager;

@SpringBootApplication
public class ILSApplication {

	public static void main(String[] args) {
		 RedisManager.init();
		 SpringApplication.run(ILSApplication.class, args);
		 String a = "1";
	}
}
