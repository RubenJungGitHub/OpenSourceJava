package contain.opensource.ils.bs.migration.services;

import org.springframework.stereotype.Service;

import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;



@Service
public class migrationservice {

    //private final GraphService graphService;
  //  private final AlfrescoNodeController alfrescoController;

//   public migrationservice(GraphService graphService,
//                           AlfrescoNodeController alfrescoController) {
//        this.graphService = graphService;
//        this.alfrescoController = alfrescoController;
//    }

    public void migrateNodeToAlfresco(RelocateInformationObject robject) {

        //alfrescoController.fetchNode(object.getId());
        try
        {
    //        alfrescoController.uploadSPItemToAlfresco(robject);
        }
        catch(Exception ex)
        {
                    // to do
        }
        // additional coordination logic
    }

    public void migrateNodeToSP(RelocateInformationObject robject) {

        //alfrescoController.fetchNode(object.getId());
        try
        {
      //      graphService.uploadAlfrescoNodeToSP(robject);
        }
        catch(Exception ex)
        {
                    // to do
        }
        // additional coordination logic
    }

}