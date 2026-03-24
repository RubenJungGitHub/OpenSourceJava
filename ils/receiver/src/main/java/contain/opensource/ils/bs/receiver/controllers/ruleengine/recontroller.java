package contain.opensource.ils.bs.receiver.controllers.ruleengine;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import contain.opensource.shared.constants.AlfrescoConstants;
import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import  contain.opensource.ils.bs.receiver.services.migrationservice;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.ils.bs.receiver.services.ruleengineservice;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
public class recontroller {

    ILSRestProperties ilsproperties;
    ruleengineservice reservice;

    @Autowired
    public recontroller(ILSRestProperties ilsProperties, ruleengineservice reservice)
    {
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
    public String getRules(
            @RequestParam AlfrescoConstants.ContainPlatforms platformfrom, 
            @RequestParam String containerfrom, 
            @RequestParam String classification, 
            @RequestParam String marking) {
        try {
             RelocateInformationObject ROobject = new RelocateInformationObject();
             ROobject.containplatformfrom = platformfrom;
             ROobject.setcontainfromcontainer(containerfrom);
             ROobject.classification= classification;
             ROobject.marking= marking;
             reservice.executeDMN(ROobject);
             return ROobject.getcontainplatformcontainerto();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error acquiring token: " + e.getMessage();
        }
    }
}