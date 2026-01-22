package contain.opensource.ils.bs.receiver.classes.ConfigurationBeans;


import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "flyway")
public class FlywayConfig {

    @Bean
    public Flyway flyway(DataSource dataSource) {
        // Flyway will use the provided DataSource
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .connectRetries(5) // optional, helpful in Docker/slow startup
                .load();
    }
}
