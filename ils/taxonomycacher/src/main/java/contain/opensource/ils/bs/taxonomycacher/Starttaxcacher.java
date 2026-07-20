package contain.opensource.ils.bs.taxonomycacher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties; // Import this
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import contain.opensource.ils.bs.taxonomycacher.redis.RedisConfigProperties;
import contain.opensource.ils.bs.taxonomycacher.scheduler.CacheTaxonomies;
import contain.opensource.ils.bs.taxonomycacher.scheduler.TaxcacherScheduler;
import contain.opensource.shared.configurationproperties.ILSRestProperties;

@SpringBootApplication(scanBasePackages = {
    "contain.opensource.ils.bs.taxonomycacher",
    "contain.opensource.shared"           // Ensure your shared properties/beans are scanned
})
@EnableConfigurationProperties({ILSRestProperties.class,RedisConfigProperties.class})
@EnableScheduling
public class Starttaxcacher {

    public static void main(String[] args) {
        SpringApplication.run(Starttaxcacher.class, args);
    }
}