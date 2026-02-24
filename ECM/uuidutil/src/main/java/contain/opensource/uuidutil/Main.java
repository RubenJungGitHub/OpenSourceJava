package contain.opensource.uuidutil;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "contain.opensource.uuidutil",           // main app
        "contain.opensource.uuidutil.controllers",   // your controllers package
        "contain.opensource.ils.shared"         // shared configuration
})
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}