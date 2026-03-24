package contain.opensource.uuidutil;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {
        "contain.opensource.uuidutil",
        "contain.opensource.uuidutil.controllers",
        "contain.opensource.ils.shared"
})
// This is the missing link! 
// It finds ILSRestProperties and any other config beans.
@ConfigurationPropertiesScan(basePackages = {
        "contain.opensource.shared.configurationproperties",
        "contain.opensource.uuidutil" 
})
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}