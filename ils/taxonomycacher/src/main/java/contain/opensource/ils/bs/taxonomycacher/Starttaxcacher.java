package contain.opensource.ils.bs.taxonomycacher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;



@SpringBootApplication(scanBasePackages = {
    "contain.opensource.ils.bs.taxonomycacher" 
})

@EnableScheduling
public class Starttaxcacher {

	public static void main(String[] args) {
		SpringApplication.run(Starttaxcacher.class, args);
	}
}
