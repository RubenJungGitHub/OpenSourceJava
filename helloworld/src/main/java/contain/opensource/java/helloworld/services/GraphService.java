package contain.opensource.java.helloworld.services;

import com.microsoft.aad.msal4j.*;

import io.swagger.v3.oas.annotations.Parameter;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Service
public class GraphService {

    // Use your own tenant, clientId, clientSecret
    private final String tenantId = "9a1b5f77-1f1a-40ac-b1a1-38617300f02a";
    private final String clientId = "f590b477-5bd7-47d6-8bda-36f77fa10afd";
    private final String clientSecret = "pE.8Q~ZQRGngJ1YliTP4EDC5bejaEl72LlBAzb50";
    private final Set<String> scopes = Collections.singleton("https://graph.microsoft.com/.default");
    private final String SiteID = "d155b09d-c4de-4d04-8b37-198f35e78232";
    private final String ListId = "9358df3d-0b30-4f09-a063-d1d8dcaeccd3";

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

    /*public String updateItemUUIDGraphAPI(
            @Parameter(description = "Site ID") @RequestParam String siteId,
            @Parameter(description = "List ID") @RequestParam String listId,
            @Parameter(description = "List Item ID") @RequestParam String listItemId,
            @Parameter(description = "Term Label") @RequestParam String termLabel
     )
            */
            public String updateItemUUIDGraphAPI(
            @Parameter(description = "List Item ID") @RequestParam String listItemId
     )
            {
        try {
            //First obtain new UUID and accesstoken 
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
           /*   String endpoint = String.format(
                    "https://graph.microsoft.com/v1.0/sites/%s/lists/%s/items/%s/fields",
                    siteId, listId, listItemId);
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
        //System.out.println("Generated UUID: " + uuid.toString());
    }
}
