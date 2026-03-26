package contain.opensource.ils.bs.receiver.controllers;

import java.util.Map;
import java.util.HashMap;

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

import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoNodeController;
import contain.opensource.ils.bs.receiver.services.GraphService;
import contain.opensource.ils.bs.receiver.services.migrationservice;
import contain.opensource.shared.classes.MigrationQueueMessage;
import contain.opensource.shared.classes.SharepointQueMessage;
import contain.opensource.shared.constants.AlfrescoConstants;
import contain.opensource.shared.classes.AlfrescoNodeResponse;

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

    @PostMapping(value = "/ProcessChangedSharepointItem", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> ProcessChangedSharepointItem(
            @RequestParam String ItemWebUrl,
            @RequestParam String ListItemID,
            @RequestParam String resourceValue) {
        System.out.println("in ProcessChangedSharepointItem endpoint voor item: " + ItemWebUrl);

        try {
            var result = graphService.ProcessChangedSharepointItem(
                    ItemWebUrl,
                    ListItemID,
                    resourceValue);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }

    @GetMapping(value = "/GetAfrescoIOUUID", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> GetAfrescoIOUUID(
            @RequestParam String nodeid) {
        System.out.println("in GetAfrescoIOUUID endpoint voor item: " + nodeid);
        try {
            AlfrescoNodeResponse nodeinfo = alfrescoNodeController.GetNode();
            return ResponseEntity.ok(true);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }
}
