package contain.opensource.java.ils.bs.receiver.controllers;

//import contain.opensource.java.ils.bs.receiver.services.GraphTokenService;
import static org.fusesource.jansi.Ansi.ansi;
import org.fusesource.jansi.AnsiConsole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import contain.opensource.java.ils.bs.receiver.classes.AlfrescoNodeController;
import contain.opensource.java.ils.bs.receiver.classes.InformationObject;
import contain.opensource.java.ils.bs.receiver.classes.RelocateIORequest;
import contain.opensource.java.ils.bs.receiver.services.GraphService;
import org.springframework.http.MediaType;

@RestController
public class ILSController {
    private final GraphService graphService;

    public ILSController(GraphService graphService) {
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

    @PostMapping("/UpdateSharepointItemGraphAPI/{listItemId}")
    public String UpdateItemUUIDGraphAPI(@PathVariable("listItemId") String listItemId) {
        try {
            String retval = graphService.updateSharepointItemGraphAPI(listItemId); // now works
            return retval;
        } catch (Exception e) {
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
        return "Hello world static, " + to + " from " + from + "!";
    }

    @PostMapping(value = "/RelocateIO", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String RelocateIO(@RequestBody InformationObject IOobject) {
        AlfrescoNodeController Acontroller = new AlfrescoNodeController();
        Acontroller.RelocateIO(IOobject);
        return "Success";
    }
}