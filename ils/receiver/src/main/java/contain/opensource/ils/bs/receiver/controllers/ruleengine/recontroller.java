package contain.opensource.ils.bs.receiver.controllers.ruleengine;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
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
}