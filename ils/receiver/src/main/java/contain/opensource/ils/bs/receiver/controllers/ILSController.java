package contain.opensource.ils.bs.receiver.controllers;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import contain.opensource.ils.bs.receiver.classes.Logger.IOLog;
import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoNodeController;
import contain.opensource.ils.bs.receiver.services.GraphService;
import contain.opensource.ils.bs.receiver.services.migrationservice;
import contain.opensource.shared.classes.MigrationQueueMessage;
import contain.opensource.shared.constants.AlfrescoConstants;

@RestController
@RequestMapping("/api")
public class ILSController {
    private final GraphService graphService;
    private final AlfrescoNodeController alfrescoNodeController;
    private final migrationservice migrationservice;

    @Autowired
    public ILSController(GraphService graphService, AlfrescoNodeController alfrescoNodeController,
            migrationservice migrationservice) {
        this.graphService = graphService;
        this.alfrescoNodeController = alfrescoNodeController;
        this.migrationservice = migrationservice;
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
            String retval = graphService.updateSharepointItemGraphAPI(listItemId, "PLACEHOLDERFORFUTUREIMPLEMENTATION"); // now
                                                                                                                         // works
            return retval;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error acquiring token: " + e.getMessage();
        }
    }

    @PostMapping(value = "/MigrateIO", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void MIgrateIO(@RequestBody MigrationQueueMessage msg) throws Exception {
        System.out.println("in migrate IO endpoint");
        if (msg == null) {
            throw new IllegalArgumentException("MigrationQueueMessage is null!");
        }
        try {
            migrationservice.migrateio(msg);
        } catch (Exception ex) {
            throw ex;
        }
    }

    @PostMapping(value = "/logiodeletedfromplatform", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void logalfrescoiodeleted(
            @RequestParam String platform,
            @RequestParam String id,
            @RequestParam String filename,
            @RequestParam String deletedby,
            @RequestParam String secondpath,
            @RequestParam String additionalinfo) {
        try {
            String decodesecondpath = URLDecoder.decode(secondpath, StandardCharsets.UTF_8);
            String decodefilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
            String decodeeletedby = URLDecoder.decode(deletedby, StandardCharsets.UTF_8);
            String decodeadditionalinfo = URLDecoder.decode(additionalinfo, StandardCharsets.UTF_8);

            String action = id + " : " + filename + " deleted from " + platform + " by user " + deletedby;
            IOLog.log(
                    "DeletedFromPlatform",
                    id,
                    decodesecondpath,
                    action,
                    platform,
                    platform,
                    "DeletedFromPlatform",
                    decodefilename,
                    decodeadditionalinfo,
                    AlfrescoConstants.eActionPerformed.IODELETED,
                    decodeeletedby,
                    "DeletedFromPlatform",
                    "DeletedFromPlatform",
                    "DeletedFromPlatform");
        } catch (Exception ex) {
            throw ex;
        }
    }

    @PostMapping(value = "/ProcessChangedSharepointItem", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> ProcessChangedSharepointItem(@RequestParam String ItemWebUrl,
            @RequestParam String ListItemID,
            @RequestParam String resourceValue) {
        System.out.println("in ProcessChangedSharepointItem endpoint voor item: " + ItemWebUrl);

        try {
            var result = graphService.ProcessChangedSharepointItem(
                    ItemWebUrl,
                    ListItemID,
                    resourceValue);
            return ResponseEntity.ok(result);
        } catch (NullPointerException ex) {
            // Specifically handle the "Item missing/null" case
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("ITEM_NOT_FOUND");
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }

    @PostMapping(value = "/ProcessChangedAlfrescoNode", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> ProcessChangedAlfrescoNode(
            @RequestParam String nodeid,
            @RequestParam String secondpath) {
        System.out.println("in ProcessChangedAlfrescoNode endpoint voor item: " + nodeid);
        try {
            String decodesecondpath = URLDecoder.decode(secondpath, StandardCharsets.UTF_8);
            var result = alfrescoNodeController.processalfresconodepoint(nodeid, decodesecondpath);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }
}
