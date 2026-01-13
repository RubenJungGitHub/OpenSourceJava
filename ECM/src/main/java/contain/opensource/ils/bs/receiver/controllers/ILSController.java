package contain.opensource.ils.bs.receiver.controllers;

import java.util.Optional;
//import contain.opensource.ils.bs.receiver.services.GraphTokenService;
import java.util.UUID;

import static org.fusesource.jansi.Ansi.ansi;
import org.fusesource.jansi.AnsiConsole;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import contain.opensource.ils.bs.receiver.classes.Logger.IOLog;
import contain.opensource.ils.bs.receiver.classes.Logger.IOLogBallenbak;
import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoNodeController;
import contain.opensource.ils.bs.receiver.services.GraphService;
import contain.opensource.ils.bs.receiver.classes.Redis.RedisManager;

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
    public String RelocateIO(@RequestBody RelocateInformationObject IOobject) {
        AlfrescoNodeController Acontroller = new AlfrescoNodeController();
        Acontroller.RelocateIO(IOobject);
        return "Success";
    }

    @GetMapping(value = "/GetUUID")
    public String GetUUID() {
        // Generate a random UUID
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    @GetMapping(value = "/GetIOUUIDLatestHash")
    ResponseEntity<String> GetIOUUIDLatestHash(@RequestParam String uuid) {
        long startTime = System.currentTimeMillis();
        if (RedisManager.getHashField("IOLogs", uuid) != null) {
            long endTime = System.currentTimeMillis();
            long durationMs = endTime - startTime; // duration in milliseconds
            return ResponseEntity.ok("Hash from for IO : " + uuid + " -> " + RedisManager.getHashField("IOLogs", uuid) + " timespan in MS : " + durationMs);
        } else {
            // retrieve UUID from Datastore and cache in redis
            Optional<IOLogBallenbak> Logentry = IOLog.GetLog(uuid);
            if (Logentry.isPresent()) {
                String pkiHash = Logentry.get().getPkiHash();
                RedisManager.putHash("IOLogs", uuid, pkiHash);
                pkiHash = RedisManager.getHashField("IOLogs", uuid);
                long endTime = System.currentTimeMillis();
                long durationMs = endTime - startTime; // duration in milliseconds
                return ResponseEntity.ok("Lates hash from for IO : " + uuid + " -> " + RedisManager.getHashField("IOLogs", uuid) + " timespan in MS : " + durationMs);
            } else {
                return ResponseEntity.ok("No log entry found for UUID: " + uuid);
            }
        }
    }
}