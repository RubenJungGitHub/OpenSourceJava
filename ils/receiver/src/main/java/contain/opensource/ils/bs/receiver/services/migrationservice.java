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

    public void migrateSPObjectToAlfresco(MigrationQueueMessage msg) throws Exception {
        try {

            SharePointItemResponse SPItem = GraphService.getListItemsById(msg.getlistid(), msg.getID());
            // SPitem shoud get COnvertTorelocateObkject like it has for secureobject.
            RelocateInformationObject ROobject = new RelocateInformationObject(SPItem);
            ROobject.setplatformto(AlfrescoConstants.ContainPlatforms.valueOf(msg.getplatformto().toUpperCase()));
            ROobject.setcontainerto(msg.getcontainerto());
            this.AlfrescoNodeController.uploadSPItemToAlfresco(ROobject);
            this.graphservice.deleteSPItemById(ROobject);

        } catch (Exception e) {
            System.out.println("Failed to migrate SP item : " + e.getMessage());
            throw e;
        }
    }

    public void migrateSPObjectToSPO(MigrationQueueMessage msg) throws Exception {
        try {
            SharePointItemResponse SPItem = GraphService.getListItemsById(msg.getlistid(), msg.getID());
            RelocateInformationObject ROobject = new RelocateInformationObject(SPItem);
        } catch (Exception e) {
            System.out.println("Failed to migrate SP item : " + e.getMessage());
            throw e;
        }
    }

    public void migrateAlfrescoObjectToSP(MigrationQueueMessage msg) {
        // First get item from alfresco
        RelocateInformationObject ROobject = new RelocateInformationObject(AlfrescoNodeController.GetNode(msg.getID()));
        ROobject.setplatformto(AlfrescoConstants.ContainPlatforms.valueOf(msg.getplatformto().toUpperCase()));
        ROobject.setcontainerto(msg.getcontainerto());
        // this.graphservice.uploadAlfrescoNodeToSP(ROobject);
        /*
         * // ========================================================================
         * // To be moved to migration service
         * // ========================================================================
         * /*
         * // Moveobject, binding in new environment.
         * if (aController.alfresconNodeResponse.MustMove) {
         * System.out.println(
         * contain.opensource.shared.constants.AlfrescoConstants.CYAN
         * + "Alfresco  node must-move?"
         * + aController.alfresconNodeResponse.MustMove
         * + contain.opensource.shared.constants.AlfrescoConstants.RESET);
         * RedisManager.putHash("IOinRelocateProcess", redisentryInRelocation,
         * "InProcess",
         * 120);
         * 
         * // Create generic property mapping information object
         * RelocateInformationObject IOobject = new RelocateInformationObject(
         * aController.alfresconNodeResponse,
         * "BOUND ON DESTINATION PLATFORM",
         * AlfrescoConstants.ContainPlatforms.ALFRESCO,
         * AlfrescoConstants.ContainPlatforms.SPO);
         * // MOVE FOR NOW ONLY TOGGLE BETWEEN SPO and ALFRESCO
         * // Could Be done from here but because it is not yet certain from where the
         * // relocaiton is called we use a REST API
         * // aController.RelocateIO(IOobject);
         * RestTemplate restTemplate = new RestTemplate();
         * // String endpoint = String.format(
         * // "%s/RelocateIO",
         * // this.ilsproperties.getBaseUrl());
         * String endpoint = this.ilsproperties.getBaseUrl();
         * HttpHeaders headers = new HttpHeaders();
         * headers.setContentType(MediaType.APPLICATION_JSON);
         * headers.setBasicAuth(
         * AlfrescoConstants.username,
         * AlfrescoConstants.password,
         * StandardCharsets.UTF_8);
         * 
         * HttpEntity<RelocateInformationObject> entity2 = new HttpEntity<>(IOobject,
         * headers);
         * 
         * ResponseEntity<String> response2 = restTemplate.postForEntity(endpoint,
         * entity2,
         * String.class);
         * 
         * System.out.println("Status: " + response2.getStatusCodeValue());
         * System.out.println("Body: " + response2.getBody());
         * 
         * Integer status = response2.getStatusCode().value();
         * if (status != 200) {
         * throw new IOException("HTTP error " + status);
         * }
         * } else {
         * // BindObject
         * BindIO(IOUUID, QMessage, secondPath);
         * }
         */

        // BindIO(IOUUID, QMessage, secondPath);
        // boolean migrate =
        // this.graphService.ProcessChangedSharepointItem(item.getWebUrl(),
        // item.getId(), deltaLink);
        // if (migrate) {
        // SendMigrationMessage(item);
        // }

    }

    public void migrateAlfrescoObjectToAlfresco(MigrationQueueMessage msg) {
    }
}