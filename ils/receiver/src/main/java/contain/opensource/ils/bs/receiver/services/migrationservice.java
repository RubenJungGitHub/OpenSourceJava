package contain.opensource.ils.bs.receiver.services;

import org.springframework.stereotype.Service;
import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;

@Service
public class migrationservice {

    public void migrateNodeToAlfresco() {

        // alfrescoController.fetchNode(object.getId());
        try {
            int a = 1;
            // alfrescoController.uploadSPItemToAlfresco(robject);
        } catch (Exception ex) {
            // to do
        }
        // additional coordination logic
    }

    public void migrateNodeToSP() {

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