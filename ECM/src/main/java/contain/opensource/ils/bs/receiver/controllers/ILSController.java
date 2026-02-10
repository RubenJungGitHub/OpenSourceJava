package contain.opensource.ils.bs.receiver.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoNodeController;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants;
import contain.opensource.ils.bs.receiver.services.GraphService;

@RestController
public class ILSController {
    private final GraphService graphService;
    private final AlfrescoNodeController alfrescoNodeController;

    @Autowired
    public ILSController(GraphService graphService, AlfrescoNodeController alfrescoNodeController) {
        this.graphService = graphService;
        this.alfrescoNodeController = alfrescoNodeController;
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

    @PostMapping(value = "/RelocateIO", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String RelocateIO(@RequestBody RelocateInformationObject IOobject) {
        System.out.println("in relocateIO endpoint");
        switch (IOobject.getPlatfrom()) {
            case AlfrescoConstants.ContainPlatforms.ALFRESCO:
                if (IOobject.getPlatformTo() == AlfrescoConstants.ContainPlatforms.SPO) {
                    System.out.println(contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.BRIGHT_BLUE
                            + ("Relocate " + IOobject.getUuid() + " from " + IOobject.getPlatfrom().toString() + " to  "
                                    + IOobject.getPlatformTo().toString())
                            + contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RESET);
                    this.alfrescoNodeController.RelocateIO(IOobject);
                }
            case AlfrescoConstants.ContainPlatforms.SPO:
                if (IOobject.getPlatformTo() == AlfrescoConstants.ContainPlatforms.ALFRESCO) {
                    System.out.println(contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.BRIGHT_MAGENTA
                            + ("Relocate " + IOobject.getUuid() + " from " + IOobject.getPlatfrom().toString() + " to  "
                                    + IOobject.getPlatformTo().toString())
                            + contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RESET);
                    this.graphService.RelocateIO(IOobject);
                }
        }
        return "Success";
    }
}