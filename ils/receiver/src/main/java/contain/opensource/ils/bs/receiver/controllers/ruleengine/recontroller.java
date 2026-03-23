package contain.opensource.ils.bs.receiver.controllers.ruleengine;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import contain.opensource.shared.constants.AlfrescoConstants;
import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import  contain.opensource.ils.bs.receiver.services.migrationservice;
import contain.opensource.shared.configurationproperties.ILSRestProperties;

@RestController
public class recontroller {

    ILSRestProperties ilsproperties;
     migrationservice migservice;

    @Autowired
    public recontroller(ILSRestProperties ilsProperties, migrationservice migservice)
    {
        this.ilsproperties = ilsProperties;
        this.migservice = migservice;
    }

    @GetMapping("/getREContainers")
    public String getREContainers() {
        try {
            return migservice.getRuleEnigineProjectContainerID();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error acquiring token: " + e.getMessage();
        }
    }

    @GetMapping("/TestDMNResult")
    public String TestDMNResult() {
        try {
             RelocateInformationObject ROobject = new RelocateInformationObject();
             ROobject.containplatformfrom = AlfrescoConstants.ContainPlatforms.SPO;
             ROobject.classification= "Secret";
             ROobject.marking= "HR confidential";
             return migservice.executeDMN(ROobject).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error acquiring token: " + e.getMessage();
        }
    }
}