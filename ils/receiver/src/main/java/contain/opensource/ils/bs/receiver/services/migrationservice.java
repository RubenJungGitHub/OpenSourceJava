package contain.opensource.ils.bs.receiver.services;

import java.util.List;

import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoNodeController;
import contain.opensource.ils.bs.receiver.classes.migration.MigrationQueueMessage;
import contain.opensource.ils.bs.receiver.classes.sharepoint.SharePointItemResponse;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.shared.constants.AlfrescoConstants;

@Service
public class migrationservice {

    private ILSRestProperties ilsProperties;
    private GraphService graphservice;
    private AlfrescoNodeController AlfrescoNodeController;
    private KieServicesClient Kieserviceclient;

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
            String containerid = getRuleEnigineProjectContainerID();
            Integer a = 1;
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
        } 
        catch (Exception ex) {
            throw ex;
        }
        return actualId;
    }

    public void migrateSPObjectToAlfresco(MigrationQueueMessage msg) throws Exception {
        try {

            SharePointItemResponse SPItem = GraphService.getListItemsById(msg.getlistid(), msg.getID());
            // SPitem shoud get COnvertTorelocateObkject like it has for secureobject.
            RelocateInformationObject ROobject = new RelocateInformationObject(SPItem);
            //What to do with  the relocation object

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
        // additional coordination logic
    }

/*
    public Object executeMigrationRules(Object mySourceObject) {
    // 1. Get the specialized DMN Client from your Bean
    DMNServicesClient dmnClient = kieServicesClient.getServicesClient(DMNServicesClient.class);

    // 2. Create the "Envelope" for your data
    DMNContext dmnContext = dmnClient.newContext();
    
    // CRITICAL: "MigrationInput" must match the NAME of the 
    // Input Data node in your DMN Diagram exactly.
    dmnContext.set("MigrationInput", mySourceObject);

    // 3. Send to the Container ID we found earlier
    String containerId = "contAlnMigrationRuleset_1.0.0-SNAPSHOT";
    
    ServiceResponse<DMNResult> response = dmnClient.evaluateAll(containerId, dmnContext);

    if (response.getType() == ServiceResponse.ResponseType.SUCCESS) {
        DMNResult result = response.getResult();
        
        // 4. Get the specific Decision outcome
        // Replace "FinalDecision" with the name of your Decision node
        return result.getDecisionResultByName("FinalDecision").getResult();
    } else {
        throw new RuntimeException("DMN Error: " + response.getMsg());
}
        */
}