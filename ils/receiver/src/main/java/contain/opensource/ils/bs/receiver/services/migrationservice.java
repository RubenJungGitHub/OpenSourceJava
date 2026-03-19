package contain.opensource.ils.bs.receiver.services;

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

    @Autowired
    public migrationservice(ILSRestProperties ilsProperties, GraphService graphservice,
            AlfrescoNodeController alfresconodecontroller) {
        this.ilsProperties = ilsProperties;
        this.graphservice = graphservice;
        this.AlfrescoNodeController = alfresconodecontroller;
    }

    public void migrateio(MigrationQueueMessage msg) throws Exception {

        try {

            //First het the rules from the ruleengine

            
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
            this.graphservice.RelocateIO(ROobject);
        } catch (Exception e) {
            System.out.println("Failed to migrate SP item : " + e.getMessage());
            throw e;
        }
    }

    public void migrateAlfrescoObjectToSP(MigrationQueueMessage msg) {

        
        try {
            int a = 1;
            //to do. waiting for new Alfresco license
            // alfrescoController.fetchNode(object.getId());
            // graphService.uploadAlfrescoNodeToSP(robject);
        } catch (Exception ex) {
            // to do
        }
        // additional coordination logic
    }

}