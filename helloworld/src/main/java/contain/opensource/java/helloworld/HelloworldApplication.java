package contain.opensource.java.helloworld;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import contain.opensource.java.helloworld.classes.HelloWorld;
import contain.opensource.java.helloworld.classes.MessageBrowserEvent;
import contain.opensource.java.helloworld.classes.MessageBrowserPoll;

@SpringBootApplication
public class HelloworldApplication {
	
	public static void main(String[] args) {
		args = new String[] { "--from", "Ruben", "--to", "ChatGPT" };
		SpringApplication.run(HelloworldApplication.class, args);
		HelloWorld.SayHello(args);
		//MessageBrowserEvent.ReadMessages(args);
		MessageBrowserPoll.ReadMessages(args);
	}
}
