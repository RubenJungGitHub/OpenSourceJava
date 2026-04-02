package contain.opensource.ils.bs.receiver.services;

import java.io.Serializable;

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
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.GREEN
                    + "Migrate information object -> " + msg.getKey() + " : Source  -> " + msg.getSource()
                    + " destination  -> "
                    + msg.getplatformto() + " : " + msg.getcontainerto()
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
            String source = msg.getSource().toUpperCase();
            String destination = msg.getplatformto().toUpperCase();
            String migrate = source + "->" + destination;
            switch (migrate) {
                case "SPO->ALFRESCO": {
                    migrateSPObjectToAlfresco(msg);
                    break;
                }
                case "ALFRESCO->SPO": {
                    migrateAlfrescoObjectToSP(msg);
                    break;
                }
                case "SPO->SPO": {
                    migrateSPObjectToSPO(msg);
                    break;
                }
                case "ALFRESCO->ALFRESCO": {
                    migrateAlfrescoObjectToAlfresco(msg);
                    break;
                }
                default:
                    System.out.println("No migration path matched for: " + migrate);
                    // Do nothing or throw an error
                    break;
            }
        } catch (Exception ex) {
            throw ex;
        }
    }

    private void migrateSPObjectToAlfresco(MigrationQueueMessage msg) throws Exception {
        try {

            SharePointItemResponse SPItem = GraphService.getListItemsById(msg.getlistid(), msg.getID());
            RelocateInformationObject ROobject = new RelocateInformationObject(SPItem);
            setenvironments(msg, ROobject);
            // ROobject.setplatformto(AlfrescoConstants.ContainPlatforms.valueOf(msg.getplatformto().toUpperCase()));
            // ROobject.setcontainerto(msg.getcontainerto());
            this.AlfrescoNodeController.uploadSPItemToAlfresco(ROobject);
            this.graphservice.deleteSPItemById(ROobject);

        } catch (Exception e) {
            System.out.println("Failed to migrate SP item : " + e.getMessage());
            throw e;
        }
    }

    private void migrateSPObjectToSPO(MigrationQueueMessage msg) throws Exception {
        try {
            SharePointItemResponse SPItem = GraphService.getListItemsById(msg.getlistid(), msg.getID());
            RelocateInformationObject ROobject = new RelocateInformationObject(SPItem);
            setenvironments(msg, ROobject);
            // ROobject.setplatformto(AlfrescoConstants.ContainPlatforms.valueOf(msg.getplatformto().toUpperCase()));
            // ROobject.setcontainerto(msg.getcontainerto());
            // Store original ItemId for deletion after successful upload
            this.graphservice.uploadAlfrescoNodeToSP(ROobject);
            this.graphservice.deleteSPItemByFromDeltaLink(ROobject, msg.getdeltalink());
        } catch (Exception e) {
            System.out.println("Failed to migrate SP item : " + e.getMessage());
            throw e;
        }
    }

    private void migrateAlfrescoObjectToSP(MigrationQueueMessage msg) {
        // First get item from alfresco
        try {
            RelocateInformationObject ROobject = new RelocateInformationObject(
                    AlfrescoNodeController.GetNode(msg.getID()));
            setenvironments(msg, ROobject);
            this.graphservice.uploadAlfrescoNodeToSP(ROobject);
            this.AlfrescoNodeController.DeleteAlfrescoNode(ROobject.getId());
        } catch (Exception e) {
            System.out.println("Failed to delete Alfresco node after migration: " + e.getMessage());
        }
    }

    private void migrateAlfrescoObjectToAlfresco(MigrationQueueMessage msg) {
        try {
            RelocateInformationObject ROobject = new RelocateInformationObject(
                    AlfrescoNodeController.GetNode(msg.getID()));
            setenvironments(msg, ROobject);
            this.AlfrescoNodeController.uploadSPItemToAlfresco(ROobject);
            this.AlfrescoNodeController.DeleteAlfrescoNode(ROobject.getId());
        } catch (Exception e) {
            System.out.println("Failed to delete Alfresco node after migration: " + e.getMessage());
        }
    }

    private void setenvironments(MigrationQueueMessage msg, RelocateInformationObject ROobject) {
        ROobject.setplatformto(AlfrescoConstants.ContainPlatforms.valueOf(msg.getplatformto().toUpperCase()));
        ROobject.setcontainerto(msg.getcontainerto());

    }
}