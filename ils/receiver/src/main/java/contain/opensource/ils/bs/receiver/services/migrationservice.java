package contain.opensource.ils.bs.receiver.services;

import java.io.Serializable;
import java.util.List;

import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.DMNServicesClient;
import org.kie.server.client.KieServicesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNResult;
import java.io.Serializable;
import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoNodeController;
import contain.opensource.ils.bs.receiver.classes.migration.MigrationQueueMessage;
import contain.opensource.ils.bs.receiver.classes.sharepoint.SharePointItemResponse;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.shared.constants.AlfrescoConstants;

@Service
public class migrationservice {

    private final ILSRestProperties ilsProperties;
    private final GraphService graphservice;
    private final AlfrescoNodeController AlfrescoNodeController;
    private final KieServicesClient Kieserviceclient;


    public class RelocateInformationDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        // Use plain Strings to match your BC Data Types exactly
        public String containplatformfrom;
        public String classification;
        public String marking;

        // Standard empty constructor
        public RelocateInformationDTO() {
        }

        // Convenience constructor
        public RelocateInformationDTO(String platform, String classification, String marking) {
            this.containplatformfrom = platform;
            this.classification = classification;
            this.marking = marking;
        }
    }

    @Autowired
    public migrationservice(ILSRestProperties ilsProperties, GraphService graphservice,
            AlfrescoNodeController alfresconodecontroller, KieServicesClient kieserviceclient) {
        this.ilsProperties = ilsProperties;
        this.graphservice = graphservice;
        this.AlfrescoNodeController = alfresconodecontroller;
        this.Kieserviceclient = kieserviceclient;
    }

    public void migrateio(MigrationQueueMessage msg) throws Exception {

        try {

            // First het the rules from the ruleengine
            // String containerid = getRuleEnigineProjectContainerID();
            // Integer a = 1;
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.GREEN
                    + "Migrate information object -> " + msg.getKey() + " : Source  -> " + msg.getSource()
                    + " destination  -> "
                    + msg.getDestination()
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
            if (AlfrescoConstants.ContainPlatforms.SPO.toString().equalsIgnoreCase(msg.getSource()) &&
                    AlfrescoConstants.ContainPlatforms.ALFRESCO.toString().equalsIgnoreCase(msg.getDestination())) {
                migrateSPObjectToAlfresco(msg);
            }
            if (AlfrescoConstants.ContainPlatforms.ALFRESCO.toString().equalsIgnoreCase(msg.getSource()) &&
                    AlfrescoConstants.ContainPlatforms.SPO.toString().equalsIgnoreCase(msg.getDestination())) {
                migrateAlfrescoObjectToSP(msg);
            }
        } catch (Exception ex) {
            throw ex;
        }
    }

    public void migrateSPObjectToAlfresco(MigrationQueueMessage msg) throws Exception {
        try {

            SharePointItemResponse SPItem = GraphService.getListItemsById(msg.getlistid(), msg.getID());
            // SPitem shoud get COnvertTorelocateObkject like it has for secureobject.
            RelocateInformationObject ROobject = new RelocateInformationObject(SPItem);

            // What to do with the relocation object
            executeDMN(ROobject);

            this.graphservice.RelocateIO(ROobject);
        } catch (Exception e) {
            System.out.println("Failed to migrate SP item : " + e.getMessage());
            throw e;
        }
    }

    public void migrateAlfrescoObjectToSP(MigrationQueueMessage msg) {

        try {
            int a = 1;
            // to do. waiting for new Alfresco license
            // alfrescoController.fetchNode(object.getId());
            // graphService.uploadAlfrescoNodeToSP(robject);
        } catch (Exception ex) {
            // to do
        }
    }
    // additional coordination logic}

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

    public Object executeDMN(RelocateInformationObject ROobject) {
        Object result = null;
        //Convert to serializable type for Business central for fields must map 
        RelocateInformationDTO RuleEngineDTO = new RelocateInformationDTO(
        ROobject.containplatformfrom != null ? ROobject.containplatformfrom.toString() : null,
        ROobject.classification,
        ROobject.marking);
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
            DMNResult dmnResult = serverResponse.getResult();

            // 5. Get the output of your decision node
            // Replace "dcsSource..." with the exact name of your Decision box
            result = dmnResult.getDecisionResultByName("dcsSource.containplatformto").getResult();

            System.out.println("DMN Output: " + result);

        } else {
            System.err.println("DMN Error: " + serverResponse.getMsg());
        }
        ROobject.setcontainplatformcontainerto(result);
        return result;
    }
}