package contain.opensource.ils.bs.receiver.services;

import java.io.Serializable;
//import java.util.List;
//import java.util.Map;
//import org.kie.dmn.api.core.DMNContext;
//import org.kie.dmn.api.core.DMNDecisionResult;
//import org.kie.dmn.api.core.DMNResult;
//import org.kie.server.api.model.KieContainerResource;
//import org.kie.server.api.model.KieContainerResourceList;
//import org.kie.server.api.model.ServiceResponse;
//import org.kie.server.client.DMNServicesClient;
//import org.kie.server.client.KieServicesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoNodeController;
import contain.opensource.ils.bs.receiver.classes.sharepoint.SharePointItemResponse;
import contain.opensource.shared.classes.MigrationQueueMessage;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.shared.constants.AlfrescoConstants;

@Service
public class migrationservice {

    private final ILSRestProperties ilsProperties;
    private final GraphService graphservice;
    private final AlfrescoNodeController AlfrescoNodeController;
    // private final KieServicesClient Kieserviceclient;

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
    public migrationservice(ILSRestProperties ilsProperties, GraphService graphservice,
            AlfrescoNodeController alfresconodecontroller) {
        this.ilsProperties = ilsProperties;
        this.graphservice = graphservice;
        this.AlfrescoNodeController = alfresconodecontroller;
        // this.Kieserviceclient = kieserviceclient;
    }

    public void migrateio(MigrationQueueMessage msg) throws Exception {

        try {
            // First get the rules from the ruleengine based on
            // String containerid = getRuleEnigineProjectContainerID();
            // Integer a = 1;
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.GREEN
                    + "Migrate information object -> " + msg.getKey() + " : Source  -> " + msg.getSource()
                    + " destination  -> "
                    + msg.getplatformto()
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
            String source = msg.getSource().toUpperCase();
            String destination = msg.getplatformto().toUpperCase();
            String migrate = source + "->" + destination;
            switch (migrate) {
                case "SPO->ALFRESCO": {
                    // if
                    // (AlfrescoConstants.ContainPlatforms.SPO.name().equalsIgnoreCase(msg.getSource())
                    // &&
                    // AlfrescoConstants.ContainPlatforms.ALFRESCO.name().equalsIgnoreCase(msg.getplatformto()))
                    // {
                    migrateSPObjectToAlfresco(msg);
                }
                case "ALFRESCO->SPO": {
                    migrateAlfrescoObjectToSP(msg);
                }
                case "SPO->SPO": {
                    migrateSPObjectToSPO(msg);
                }
                case "ALFRESCO->ALFRESCO": {
                    migrateAlfrescoObjectToAlfresco(msg);
                }
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
            // executeDMN(ROobject);

            this.graphservice.RelocateIO(ROobject);
        } catch (Exception e) {
            System.out.println("Failed to migrate SP item : " + e.getMessage());
            throw e;
        }
    }

    public void migrateSPObjectToSPO(MigrationQueueMessage msg) throws Exception {
        try {

            SharePointItemResponse SPItem = GraphService.getListItemsById(msg.getlistid(), msg.getID());
            // SPitem shoud get COnvertTorelocateObkject like it has for secureobject.
            RelocateInformationObject ROobject = new RelocateInformationObject(SPItem);

            // What to do with the relocation object
            // executeDMN(ROobject);

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

    public void migrateAlfrescoObjectToAlfresco(MigrationQueueMessage msg) {

        try {
            int a = 1;
            // to do. waiting for new Alfresco license
            // alfrescoController.fetchNode(object.getId());
            // graphService.uploadAlfrescoNodeToSP(robject);
        } catch (Exception ex) {
            // to do
        }
    }

}