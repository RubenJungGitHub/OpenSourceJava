package contain.opensource.ils.bs.receiver.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import contain.opensource.ils.bs.receiver.classes.migration.MigrationQueueMessage;
import contain.opensource.ils.bs.receiver.classes.sharepoint.SharePointItemResponse;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.shared.constants.AlfrescoConstants;
import contain.opensource.ils.bs.receiver.services.GraphService;
import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoNodeController;

@Service
public class migrationservice {

    private ILSRestProperties ilsProperties;
    private GraphService graphservice;
    private AlfrescoNodeController AlfrescoNodeController;

    @Autowired
    public migrationservice(ILSRestProperties ilsProperties, GraphService graphservice, AlfrescoNodeController alfresconodecontroller ) {
        this.ilsProperties = ilsProperties;
        this.graphservice = graphservice;
        this.AlfrescoNodeController = alfresconodecontroller;
    }

    public void migrateio(MigrationQueueMessage msg) {

        if (AlfrescoConstants.ContainPlatforms.SPO.toString().equalsIgnoreCase(msg.getSource()) &&
                AlfrescoConstants.ContainPlatforms.ALFRESCO.toString().equalsIgnoreCase(msg.getDestination())) {
                migrateSPObjectToAlfresco(msg);
        }
        if (AlfrescoConstants.ContainPlatforms.ALFRESCO.toString().equalsIgnoreCase(msg.getSource()) &&
                AlfrescoConstants.ContainPlatforms.SPO.toString().equalsIgnoreCase(msg.getDestination())) {
                migrateAlfrescoObjectToSP(msg);
        }
    }

    public boolean migrateSPObjectToAlfresco(MigrationQueueMessage msg) {
        try {

            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.GREEN
                    + "Move SPItem -> " + msg.getKey()
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);

            SharePointItemResponse SPItem = GraphService.getListItemsById(msg.getlistid(), msg.getID());
            //SPitem shoud get COnvertTorelocateObkject like it has for secureobject.
            RelocateInformationObject ROobject = new RelocateInformationObject(SPItem);
            this.graphservice.RelocateIO(ROobject);
            return true;
        } catch (Exception e) {
            System.out.println("Failed to delete SP item after move: " + e.getMessage());
            return false;
        }
    }

    public void migrateAlfrescoObjectToSP(MigrationQueueMessage msg) {

        // alfrescoController.fetchNode(object.getId());
        try {
            int a = 1;
            // graphService.uploadAlfrescoNodeToSP(robject);
        } catch (Exception ex) {
            // to do
        }
        // additional coordination logic
    }

}