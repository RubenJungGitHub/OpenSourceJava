package contain.opensource.ils.bs.receiver.services;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNDecisionResult;
import org.kie.dmn.api.core.DMNResult;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.DMNServicesClient;
import org.kie.server.client.KieServicesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.shared.constants.AlfrescoConstants;

@Service
public class ruleengineservice {

    private final ILSRestProperties ilsProperties;
    private final KieServicesClient Kieserviceclient;

    public class RelocateInformationDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        // Use plain Strings to match your BC Data Types exactly
        public String containplatformfrom;
        public String containfromcontainer;
        public String classification;

        public String marking;

        // Standard empty constructor
        public RelocateInformationDTO() {
        }

        // Convenience constructor
        public RelocateInformationDTO(String platform, String classification, String marking,
                String containfromcontainer) {
            this.containplatformfrom = platform;
            this.classification = classification;
            this.marking = marking;
            this.containfromcontainer = containfromcontainer;
        }
    }

    @Autowired
    public ruleengineservice(ILSRestProperties ilsProperties, KieServicesClient kieserviceclient) {
        this.ilsProperties = ilsProperties;
        this.Kieserviceclient = kieserviceclient;
    }

    public String getRuleEnigineProjectContainerID() {
        String actualId = null;
        try {
            ServiceResponse<KieContainerResourceList> response = Kieserviceclient.listContainers();
            if (response.getType() == ServiceResponse.ResponseType.SUCCESS) {
                // 3. This is where the list actually lives
                KieContainerResourceList containerList = response.getResult();

                if (containerList != null && containerList.getContainers() != null) {
                    List<KieContainerResource> containers = containerList.getContainers();
                    String projectName = ilsProperties.getRuleengineprojectname();
                    // Print them out to verify
                    containers.forEach(c -> System.out.println("Found Container: " + c.getContainerId()));

                    // 2. Filter for the one that starts with your project name
                    actualId = response.getResult().getContainers().stream()
                            .map(KieContainerResource::getContainerId)
                            .filter(id -> id.startsWith(projectName))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException(
                                    "No active container found for project: " + projectName));
                }
            } else {
                System.err.println("Failed to list containers: " + response.getMsg());
            }
        } catch (Exception ex) {
            throw ex;
        }
        return actualId;
    }

    public void executeDMN(RelocateInformationObject ROobject) {
       // Convert to serializable type for Business central for fields must map
        RelocateInformationDTO RuleEngineDTO = new RelocateInformationDTO(
                ROobject.containplatformfrom != null ? ROobject.containplatformfrom.toString() : null,
                ROobject.classification,
                ROobject.marking,
                ROobject.getcontainfromcontainer());

        // 1. Get the DMN Client from your existing Kieserviceclient
        DMNServicesClient dmnClient = Kieserviceclient.getServicesClient(DMNServicesClient.class);

        // 2. Get the Container ID (using your existing method)
        String containerId = getRuleEnigineProjectContainerID();

        // 3. Create the DMN Context and "Source" must match the DMN Node Name
        DMNContext dmnContext = dmnClient.newContext();
        dmnContext.set("source", RuleEngineDTO); // "source" is the ID of the Input Node in your DMN

        // 4. Call the server
        // Replace "YourNamespace" and "YourModelName" with values from DMN 'Overview'
        // tab
        ServiceResponse<DMNResult> serverResponse = dmnClient.evaluateAll(containerId, dmnContext);

        if (serverResponse.getType() == ServiceResponse.ResponseType.SUCCESS) {
            DMNDecisionResult dr = serverResponse.getResult().getDecisionResultByName("destination");
            // 2. Access the Map inside the result
            if (dr != null && dr.getResult() instanceof Map) {
                Map<String, Object> resultMap = (Map<String, Object>) dr.getResult();

                // 3. Use the keys exactly as they appear in your debug output
                String containerto = (String) resultMap.get("containtocontainer");
                String platformto = (String) resultMap.get("containplatformto");

                System.out.println("Target Container: " + containerto);
                System.out.println("Target Platform: " + platformto);

                // Now you can map these back to your original ROobject if needed
                ROobject.setPlatformTo(AlfrescoConstants.ContainPlatforms.valueOf(platformto));
                ROobject.setcontainplatformcontainerto(containerto);
            }
        } else {
            System.err.println("DMN Error: " + serverResponse.getMsg());
        }
    }
}