package contain.opensource.java.helloworld.controllers;

//import contain.opensource.java.helloworld.services.GraphTokenService;
import java.awt.print.Book;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.springframework.web.bind.annotation.RequestMapping;

import contain.opensource.java.helloworld.services.GraphService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
//import com.microsoft.aad.msal4j.ClientCredential;
import com.microsoft.aad.msal4j.IAuthenticationResult;

import org.fusesource.jansi.AnsiConsole;
import static org.fusesource.jansi.Ansi.ansi;

@RestController
public class HelloWorldController {
    private final GraphService graphService;

    public HelloWorldController(GraphService graphService) {
        this.graphService = graphService;
        AnsiConsole.systemInstall();
    }

    @GetMapping("/GetGraphToken")
    public String getGraphToken() {
        try {
            String token = graphService.getGraphToken(); // now works
            return token != null ? token : "Failed to acquire token";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error acquiring token: " + e.getMessage();
        }
    }

    @PostMapping("/UpdateItemUUIDGraphAPI/{listItemId}")
    public String UpdateItemUUIDGraphAPI(@PathVariable("listItemId") String listItemId) {
        try {
            String retval = graphService.updateItemUUIDGraphAPI(listItemId); // now works
            return retval;
        } catch (Exception e)
        {
            e.printStackTrace();
            return "Error acquiring token: " + e.getMessage();
        }
    }

    @GetMapping("/users/{id}")
    public String getUser(@PathVariable("id") String id) {
        return "User ID: " + id;
    }

    @GetMapping("/hello")
    public String SayHello(
            @RequestParam(defaultValue = "Ruben") String from,
            @RequestParam(defaultValue = "ChatGPT") String to) {

        System.out.println(ansi().fgRed().a("Hello world, ")
                .fgBlue().a(to)
                .fgGreen().a(" from " + from + "!")
                .reset());

        // Return a string to the REST client
        return "Hello worl staticd, " + to + " from " + from + "!";
    }
}
