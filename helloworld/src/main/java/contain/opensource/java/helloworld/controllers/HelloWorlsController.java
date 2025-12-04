package contain.opensource.java.helloworld.controllers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import static org.fusesource.jansi.Ansi.ansi;
import org.fusesource.jansi.AnsiConsole;

@RestController
public class HelloWorlsController {
    @GetMapping("/users/{id}")
    public String getUser(@PathVariable("id") String id) {
        return "User ID: " + id;
    }

    @GetMapping("/hello")
    public String SayHello(String[] args) {
                     if (args == null || args.length == 0) {
                        args = new String[] { "--from", "Ruben", "--to", "ChatGPT" };
                }
                String from = "DefaultFrom";
                String to = "DefaultTo";

                for (int i = 0; i < args.length; i++) {
                        switch (args[i]) {
                                case "--from":
                                        if (i + 1 < args.length)
                                                from = args[i + 1];
                                        i++; // skip value
                                        break;
                                case "--to":
                                        if (i + 1 < args.length)
                                                to = args[i + 1];
                                        i++;
                                        break;
                        }
                }

                System.out.println(ansi().fgRed().a("Hello world, ")
                                .fgBlue().a(to)
                                .fgGreen().a(" from " + from + "!")
                                .reset());
                  
        // Return a string to the REST client
        return "Hello world, " + to + " from " + from + "!";              
    }

    // POST endpoint: /users
    @PostMapping("/users")
    public String createUser(@RequestBody String userJson) {
        // Normally, you'd parse userJson and save it
        return "Created user: " + userJson;
    }
}
