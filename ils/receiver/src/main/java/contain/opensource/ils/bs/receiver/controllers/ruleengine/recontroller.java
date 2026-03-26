package contain.opensource.ils.bs.receiver.controllers.ruleengine;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import contain.opensource.ils.bs.receiver.services.ruleengineservice;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.shared.constants.AlfrescoConstants;

@RestController
@RequestMapping("/api")
public class recontroller {

    ILSRestProperties ilsproperties;
    ruleengineservice reservice;

    @Autowired
    public recontroller(ILSRestProperties ilsProperties, ruleengineservice reservice) {
        this.ilsproperties = ilsProperties;
        this.reservice = reservice;
    }

    @GetMapping("/getREContainers")
    public String getREContainers() {
        try {
            return reservice.getRuleEnigineProjectContainerID();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error acquiring token: " + e.getMessage();
        }
    }

    @GetMapping("/rules")
    public ResponseEntity<?> getRules(
            @RequestParam AlfrescoConstants.ContainPlatforms platformfrom,
            @RequestParam String containerfrom,
            @RequestParam String classification,
            @RequestParam String marking) {

        RelocateInformationObject roObject = new RelocateInformationObject();
        // Set values
        roObject.platformfrom = platformfrom;
        roObject.containerfrom = containerfrom;
        roObject.classification = classification;
        roObject.marking = marking;

        // Execute logic
        reservice.executeDMN(roObject);

        // TEST: Stuur een simpele Map terug in plaats van het complexe object
        Map<String, String> Result = new HashMap<>();
        Result.put("containerto", roObject.getcontainerto());
        Result.put("platformto", (roObject.getplatformto() != null) ? roObject.getplatformto().name() : "UNKNOWN");
        return ResponseEntity.ok(Result);
    }
}