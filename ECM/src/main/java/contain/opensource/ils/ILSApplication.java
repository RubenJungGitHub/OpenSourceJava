package contain.opensource.ils;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ILSApplication {

	public static void main(String[] args) {
		args = new String[] { "--from", "Ruben", "--to", "ChatGPT" };
		        SpringApplication.run(ILSApplication.class, args);
		        // Start Spring context and get the ApplicationContext
   /*     var context = SpringApplication.run(ILSApplication.class, args);

        // Retrieve beans from Spring context
        ILS ils = context.getBean(ILS.class);
		//SpringApplication.run(ILSApplication.class, args);
		ils.SayHello(args);
		//MessageBrowserPoll poll = context.getBean(MessageBrowserEvent.class);
		MessageBrowserPoll poll = context.getBean(MessageBrowserPoll.class);
		//MessageBrowserEvent.ReadMessages(args);
		MessageBrowserPoll.ReadMessages(args);
		poll.ReadMessages(args);
		*/

	}
}
