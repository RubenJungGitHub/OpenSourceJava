package contain.opensource.ils.bs.receiver.services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import contain.opensource.ils.bs.receiver.classes.migration.MigrationQueueMessage;
import contain.opensource.ils.bs.receiver.classes.sharepoint.SharePointItemResponse;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.shared.constants.AlfrescoConstants;

@Service
public class migrationservice {

    private ILSRestProperties ilsProperties;

    @Autowired
    public migrationservice(ILSRestProperties ilsProperties) {
        this.ilsProperties = ilsProperties;
    }

    public void migrateNodeToAlfresco(MigrationQueueMessage msg) {

        // Relocate item
        try {

            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.GREEN
                    + "Move SPItem -> " + msg.getKey()
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
            SharePointItemResponse SPItem = GraphService.getListItemsById(msg.getlistid(), msg.getID());
            RelocateInformationObject ROobject = new RelocateInformationObject(SPItem);
            String endpoint = this.ilsProperties.getRelocateendpoint();
            System.out.println("Relocate endpoint: " + endpoint);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(
                    AlfrescoConstants.username,
                    AlfrescoConstants.password,
                    StandardCharsets.UTF_8);
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<RelocateInformationObject> entitymove = new HttpEntity<>(ROobject,
                    headers);

            ResponseEntity<String> response = restTemplate.postForEntity(endpoint,
                    entitymove,
                    String.class);

            System.out.println("Status: " + response.getStatusCodeValue());
            System.out.println("Body: " + response.getBody());

            int status = response.getStatusCode().value();
            if (status != 200) {
                throw new IOException("HTTP error " + status);
            }

        } catch (Exception e) {
            System.out.println("Failed to delete SP item after move: " + e.getMessage());
        }
    }

    public void migrateNodeToSP(MigrationQueueMessage msg) {

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