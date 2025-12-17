package contain.opensource.ils;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import contain.opensource.ils.bs.receiver.classes.ILS;
import contain.opensource.ils.bs.receiver.classes.MessageBrowserPoll;

@SpringBootApplication
public class ILSApplication {

	public static void main(String[] args) {
		args = new String[] { "--from", "Ruben", "--to", "ChatGPT" };
		SpringApplication.run(ILSApplication.class, args);
		ILS.SayHello(args);
		// MessageBrowserEvent.ReadMessages(args);
		//MessageBrowserPoll.ReadMessages(args);
	}
}
