package contain.opensource.java.helloworld.controllers;

//import contain.opensource.java.helloworld.services.GraphTokenService;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import contain.opensource.java.helloworld.services.GraphTokenService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import static org.fusesource.jansi.Ansi.ansi;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
//import com.microsoft.aad.msal4j.ClientCredential;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import org.fusesource.jansi.AnsiConsole;

@RestController
public class HelloWorldController {
        private final GraphTokenService graphTokenService;
    public HelloWorldController(GraphTokenService graphTokenService) {
        this.graphTokenService = graphTokenService;
        AnsiConsole.systemInstall();
    }

    @GetMapping("/users/{id}")
    public String getUser(@PathVariable("id") String id) {
        return "User ID: " + id;
    }

    @GetMapping("/hello")
    public String SayHello(
         @RequestParam(defaultValue = "Ruben") String from,
            @RequestParam(defaultValue = "ChatGPT") String to)
            {

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

    @GetMapping("/GetGraphToken")
    public String getGraphToken() {
        String token = graphTokenService.getClientToken();
        if (token == null) {
            return "Failed to acquire token.";
        }
        return token;
    }

/*
    @GetMapping("/GetSPClientToken")
    public String getSPClientToken(@RequestParam String tenantId) {
        try {
            // Build client credentials
            ClientCredential credential = ClientCredentialFactory.createFromSecret(clientSecret);

            ConfidentialClientApplication app = ConfidentialClientApplication.builder(clientId, credential)
                    .authority("https://login.microsoftonline.com/" + tenantId)
                    .build();

            // Scope for SharePoint app-only
            String scope = "https://" + domain + "/.default";

            // Acquire token
            CompletableFuture<IAuthenticationResult> future = app.acquireToken(
                    com.microsoft.aad.msal4j.ClientCredentialParameters.builder(
                            Collections.singleton(scope))
                            .build());

            IAuthenticationResult result = future.get(); // wait for completion

            if (result == null || result.accessToken() == null) {
                throw new IllegalStateException("Failed to acquire SharePoint token.");
            }

            // Optional: decode JWT payload to check audience
            String accessToken = result.accessToken();
            String payload = accessToken.split("\\.")[1];
            payload = padBase64(payload);
            String json = new String(java.util.Base64.getDecoder().decode(payload));
            System.out.println("Token payload: " + json);

            return accessToken;

        } catch (MalformedURLException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }
    */
}
