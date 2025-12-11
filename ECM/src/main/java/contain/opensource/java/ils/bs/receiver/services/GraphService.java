package contain.opensource.java.ils.bs.receiver.services;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
//import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import java.nio.charset.StandardCharsets;
import org.apache.hc.core5.http.ContentType;
import java.net.URLEncoder;

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
    // This obviously should be stored secure soewhere in the future!!!!
    // ====================================================================
    // ====================================================================
    private final String tenantId = "9a1b5f77-1f1a-40ac-b1a1-38617300f02a";
    private final String tenantDomain = "lls6.Sharepoint.com";
    private final String clientId = "f590b477-5bd7-47d6-8bda-36f77fa10afd";
    private final String clientSecret = "pE.8Q~ZQRGngJ1YliTP4EDC5bejaEl72LlBAzb50";
    private final Set<String> scopes = Collections.singleton("https://graph.microsoft.com/.default");
    private final String SiteID = "d155b09d-c4de-4d04-8b37-198f35e78232";
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

    /*
     * public String updateItemUUIDGraphAPI(
     * 
     * @Parameter(description = "Site ID") @RequestParam String siteId,
     * 
     * @Parameter(description = "List ID") @RequestParam String listId,
     * 
     * @Parameter(description = "List Item ID") @RequestParam String listItemId,
     * 
     * @Parameter(description = "Term Label") @RequestParam String termLabel
     * )
     */
    public String updateItemUUIDGraphAPI(
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

            // Build endpoint
            /*
             * String endpoint = String.format(
             * "https://graph.microsoft.com/v1.0/sites/%s/lists/%s/items/%s/fields",
             * siteId, listId, listItemId);
             */
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

            // Encode folder and filename
            //String folderUrl = URLEncoder.encode("Shared Documents", StandardCharsets.UTF_8.name());
            //String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name());

            // Step 1: Upload file content
            String encodedFolder = URLEncoder.encode(this.ListName, StandardCharsets.UTF_8.toString());
            String encodedFile = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());

            String endPoint = "https://" + this.tenantDomain + ".sharepoint.com/sites/" + this.SiteID
                    + "/_api/web/GetFolderByServerRelativeUrl('" + encodedFolder + "')/Files/add(url='" +encodedFile
                    + "',overwrite=true)";

            HttpPost uploadPost = new HttpPost(endPoint);
            uploadPost.setHeader("Authorization", "Bearer " + accessToken);
            uploadPost.setHeader("Accept", "application/json;odata=verbose");
             ByteArrayEntity fileEntity = new ByteArrayEntity(fileBytes, ContentType.create(node.entry.content.mimeType));
            uploadPost.setEntity(fileEntity);

            int itemId;
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                var uploadResponse = client.execute(uploadPost);
                String jsonResponse = EntityUtils.toString(uploadResponse.getEntity());

                // Parse response to get ListItem ID
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(jsonResponse);
                itemId = root.path("d").path("ListItemAllFields").path("Id").asInt();
            }

            /*/
            // Step 2: Update metadata (UUID, Title, etc.)
            String metaUrl = "https://" + tenantId +
                    "/sites/" + siteId +
                    "/_api/web/lists(guid'" + listId + "')/items(" + itemId + ")";
            HttpPost metaPost = new HttpPost(metaUrl);
            metaPost.setHeader("Authorization", "Bearer " + accessToken);
            metaPost.setHeader("Accept", "application/json;odata=verbose");
            metaPost.setHeader("Content-Type", "application/json;odata=verbose");
            metaPost.setHeader("IF-MATCH", "*");
            metaPost.setHeader("X-HTTP-Method", "MERGE");

            String uuid = (String) node.entry.properties.otherProperties.get("RJTM:UUID");
            String jsonBody = "{ \"UUID\": \"" + uuid + "\", " +
                    "\"Title\": \"" + fileName + "\" }";
            metaPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

            
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                var metaResponse = client.execute(metaPost);
                System.out.println("File uploaded and metadata set for item: " + itemId);
            }
                */

        } catch (Exception e) {
            System.err.println("Failed to move Alfresco node: " + e);
            e.printStackTrace();
        }
    }
/*
    public void uploadAlfrescoNodeToSPObsolete(AlfrescoNodeResponse node) {
        try {
            // First obtain new UUID and accesstoken
            String AccessToken = getGraphToken();
            // String endpoint =
            // "https://{tenant}.sharepoint.com/sites/{site}/_api/web/lists(guid'{listId}')/items({itemId})";
            String endPoint = "https://" + this.tenantDomain + ".sharepoint.com/sites/" + this.SiteID
                    + "/_api/web/GetFolderByServerRelativeUrl(" + this.ListId + ")/Files/add(url='" + node.entry.name
                    + "',overwrite=true)";

            HttpPost uploadPost = new HttpPost(endPoint);
            uploadPost.setHeader("Authorization", "Bearer " + accessToken);
            uploadPost.setHeader("Accept", "application/json;odata=verbose");
            uploadPost.setEntity(new ByteArrayEntity(node.file));

            String jsonBody = "{ \"UUID\": \"" + node.entry.properties.otherProperties.get("RJTM:UUID") + "\", " +
                    "\"Title\": \"" + node.entry.name + "\" }";

            post.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                var response = client.execute(post);
                System.out.println(EntityUtils.toString(response.getEntity()));
            }
        } catch (Exception e) {
            System.out.println("Failed tomove Alfresconode: " + e);
        }
    }
        */
}
