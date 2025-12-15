package contain.opensource.java.ils.bs.receiver.services;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.nio.charset.StandardCharsets;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;

import contain.opensource.java.ils.bs.receiver.classes.AlfrescoNodeResponse;
import io.swagger.v3.oas.annotations.Parameter;

@Service
public class GraphService {

    // Use your own tenant, clientId, clientSecret
    // ====================================================================
    // ====================================================================
    // This obviously should be stored secure somewhere in the future!!!!
    // ====================================================================
    // ====================================================================
    private final String tenantId = "9a1b5f77-1f1a-40ac-b1a1-38617300f02a";
    private final String tenantDomain = "lls6.Sharepoint.com";
    private final String clientId = "f590b477-5bd7-47d6-8bda-36f77fa10afd";
    private final String clientSecret = "pE.8Q~ZQRGngJ1YliTP4EDC5bejaEl72LlBAzb50";
    private final Set<String> scopes = Collections.singleton("https://graph.microsoft.com/.default");
    private final String SiteID = "d155b09d-c4de-4d04-8b37-198f35e78232";
    private final String SiteName = "SP-EventReceivers-Test";
    private final String ListId = "9358df3d-0b30-4f09-a063-d1d8dcaeccd3";
    private final String ListName = "Shared Documents";

    private final ClientCredentialParameters parameters = ClientCredentialParameters.builder(scopes).build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String accessToken;

    public String getGraphToken() throws MalformedURLException, ExecutionException, InterruptedException {

        // Build confidential client application
        ConfidentialClientApplication app = ConfidentialClientApplication.builder(
                clientId,
                ClientCredentialFactory.createFromSecret(clientSecret))
                .authority("https://login.microsoftonline.com/" + tenantId)
                .build();

        // Scopes for client credentials flow
        // Set<String> scopes =
        // Collections.singleton("https://graph.microsoft.com/.default");

        // Acquire token
        IAuthenticationResult result = app.acquireToken(parameters).get();
        this.accessToken = result.accessToken();
        return this.accessToken;
    }

    public String updateSharepointItemGraphAPI(
            @Parameter(description = "List Item ID") @RequestParam String listItemId) {
        try {
            // First obtain new UUID and accesstoken
            String AccessToken = getGraphToken();
            String uuid = GetUUID();
            HttpClient client = HttpClient.newHttpClient();

            // Build payload to update Title and ObjectClassificationText
            Map<String, Object> body = new HashMap<>();
            body.put("ContAInUUID", uuid);
            body.put("Title", "Test Updated Document Title Ruben from JaVa");
            body.put("ObjectClassificationText", "Changed from Java");

            String json = objectMapper.writeValueAsString(body);

            String endpoint = String.format(
                    "https://graph.microsoft.com/v1.0/sites/%s/lists/%s/items/%s/fields",
                    SiteID, ListId, listItemId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", "Bearer " + AccessToken)
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();

            // Send request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("Field updated successfully!");
                return uuid;
            } else {
                System.out.println("Failed to update field: " + response.body());
                return "Failed";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Failed";
        }
    }

    public String updateSharepointItemGraphAPI(AlfrescoNodeResponse node, String listItemId) {
        try {
            // First obtain new UUID and accesstoken
            String AccessToken = getGraphToken();
            String uuid = GetUUID();
            HttpClient client = HttpClient.newHttpClient();

            // Build payload to update Title and ObjectClassificationText
            Map<String, Object> body = new HashMap<>();
            body.put("ContAInUUID", node.UUID);
            body.put("Title", node.Title);
            body.put("ObjectClassificationText", "Changed from Java after move");

            String json = objectMapper.writeValueAsString(body);

            String endpoint = String.format(
                    "https://graph.microsoft.com/v1.0/sites/%s/lists/%s/items/%s/fields",
                    SiteID, ListId, listItemId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", "Bearer " + AccessToken)
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();

            // Send request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("Field updated successfully!");
                return uuid;
            } else {
                System.out.println("Failed to update field: " + response.body());
                return "Failed";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Failed";
        }
    }

    private static String GetUUID() {
        // Generate a random UUID
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
        // Print the UUID
        // System.out.println("Generated UUID: " + uuid.toString());
    }

    public void uploadAlfrescoNodeToSP(AlfrescoNodeResponse node) {
        try {
            String accessToken = getGraphToken();
            byte[] fileBytes = node.file;
            String fileName = node.entry.name;
            String driveItemId = "";
            // to do check null
            String driveId = getDriveID();
            String endPoint = String.format(
                    "https://graph.microsoft.com/v1.0/drives/%s/root:/%s:/content",
                    driveId, fileName);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endPoint))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/octet-stream")
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response.body());
                driveItemId = rootNode.path("id").asText(); // DriveItem ID
                System.out.println("Uploaded file DiveItemID: " + driveItemId);
            } else {
                System.err.println("Failed to upload file: " + response.statusCode());
                System.err.println(response.body());
            }

            // Step 1 : Get new listitemid
            String listItemId = getListItemId(driveId, driveItemId);
            String retval = updateSharepointItemGraphAPI(node, listItemId);
            // Function should return something in the future for transaction purposes

        } catch (Exception e) {
            System.err.println("Failed to move Alfresco node: " + e);
            e.printStackTrace();
        }
    }



    private String getDriveID() {
        try {
            String ListNameCorrected = this.ListName.replace(" ", "%20");
            String ListWebUrl = "https://" + this.tenantDomain + "/sites/" + this.SiteName + "/" + ListNameCorrected;
            String endpoint = String.format("https://graph.microsoft.com/v1.0/sites/%s/drives", this.SiteID);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                    HttpResponse.BodyHandlers.ofString());
            // Suppose 'response' is your HttpResponse<String>
            String responseBody = response.body(); // this is your JSON string

            // Create ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();

            // Parse JSON string into JsonNode
            JsonNode rootNode = objectMapper.readTree(responseBody);
            // "value" array contains all drives
            JsonNode drivesArray = rootNode.path("value");
            for (JsonNode driveNode : drivesArray) {
                String driveNameListIWebUrl = driveNode.path("webUrl").asText();
                if (ListWebUrl.equalsIgnoreCase(driveNameListIWebUrl)) {
                    return driveNode.path("id").asText(); // Return Drive ID
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to move obtain driveID for " + this.ListName + "->" + e);
            e.printStackTrace();
        }
        return null;
    }

    private String getListItemId(String driveId, String driveItemId) {

        try {
            // Give SP time to process
            String listItemId = "";
            int retryCount  = 10;
            int retryCounter = 0;
            // Now get lisitemID to also update metadatafields To do check null
            String endPoint = String.format("https://graph.microsoft.com/v1.0/drives/%s/items/%s/listItem", driveId,
                    driveItemId);
            HttpRequest lirequest = HttpRequest.newBuilder()
                    .uri(URI.create(endPoint))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            while (listItemId.equals("") && retryCounter <= retryCount) {
                retryCounter++;
                Thread.sleep(2000);
                System.out.println(contain.opensource.java.ils.bs.receiver.constants.AlfrescoConstants.MAGENTA
                        + "Try "+ retryCounter + " ->  Get new listitemId for driveitemID " + driveItemId
                        + contain.opensource.java.ils.bs.receiver.constants.AlfrescoConstants.RESET);

                HttpResponse<String> response = HttpClient.newHttpClient().send(lirequest,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode rootNode = mapper.readTree(response.body());
                    listItemId = rootNode.path("id").asText();
                    if (!listItemId.equals("")) {
                        System.out.println("new listitemID : " + listItemId);
                        break;
                    }
                    System.out.println("ListitemID not (yet) detected");
                } else {
                    System.err.println("Failed to upload file: " + response.statusCode());
                    System.err.println(response.body());
                }
            }
            return listItemId;
        } catch (Exception e) {
            System.err.println("Failed to move Alfresco node: " + e);
            e.printStackTrace();
        }
        return null;
    }
}
