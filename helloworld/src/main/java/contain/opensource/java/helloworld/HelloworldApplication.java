package contain.opensource.java.helloworld;
import org.fusesource.jansi.AnsiConsole;
import static org.fusesource.jansi.Ansi.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import contain.opensource.java.helloworld.classes.HelloWorld;

@SpringBootApplication
public class HelloworldApplication {
	
	public static void main(String[] args) {
		args = new String[] { "--from", "Ruben", "--to", "ChatGPT" };
		SpringApplication.run(HelloworldApplication.class, args);
		HelloWorld.main(args);
	}

}
