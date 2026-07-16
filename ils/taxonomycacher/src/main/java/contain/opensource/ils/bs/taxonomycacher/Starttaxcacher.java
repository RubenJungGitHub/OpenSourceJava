package contain.opensource.ils.bs.taxonomycacher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties; // Import this
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import contain.opensource.ils.bs.receiver.services.TaxonomyServiceClient;
import contain.opensource.ils.bs.taxonomycacher.scheduler.CacheTaxonomies;
import contain.opensource.ils.bs.taxonomycacher.scheduler.TaxcacherScheduler;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.ils.bs.receiver.classes.redis.RedisManager;
import contain.opensource.ils.bs.receiver.classes.redis.RedisConfigProperties;

@SpringBootApplication
@EnableConfigurationProperties({ILSRestProperties.class, RedisConfigProperties.class})
@ComponentScan(basePackages = {
        "contain.opensource.ils.bs.taxonomycacher",
        "contain.opensource.ils.bs.receiver"
}, useDefaultFilters = false, includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        TaxonomyServiceClient.class,
        TaxcacherScheduler.class,
        CacheTaxonomies.class,
        RedisManager.class
}))
@EnableScheduling
public class Starttaxcacher {

    public static void main(String[] args) {
        SpringApplication.run(Starttaxcacher.class, args);
    }
}