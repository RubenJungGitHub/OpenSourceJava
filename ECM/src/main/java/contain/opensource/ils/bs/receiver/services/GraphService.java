package contain.opensource.ils.bs.receiver.services;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.Drive;
import com.microsoft.graph.models.FieldValueSet;
import com.microsoft.graph.models.ListItem;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.serializer.AdditionalDataManager;

import contain.opensource.ils.bs.receiver.classes.Notification;
import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoNodeController;
import contain.opensource.ils.bs.receiver.classes.sharepoint.ChangedItemsResult;
import contain.opensource.ils.bs.receiver.classes.sharepoint.SharePointDriveInfo;
import contain.opensource.ils.bs.receiver.classes.sharepoint.SharePointItemResponse;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants;
import io.swagger.v3.oas.annotations.Parameter;

@Service
public class GraphService {

    ObjectMapper mapper = new ObjectMapper();

    private final String tenantDomain = "lls6.Sharepoint.com";
    // private final Set<String> scopes =
    // Collections.singleton("https://graph.microsoft.com/.default");
    private final String SiteID = "d155b09d-c4de-4d04-8b37-198f35e78232";
    private final String SiteName = "SP-EventReceivers-Test";
    private final String ListId = "9358df3d-0b30-4f09-a063-d1d8dcaeccd3";
    private final String ListName = "Shared Documents";

    private final ClientCredentialParameters parameters = ClientCredentialParameters
            .builder(AlfrescoConstants.GraphScopes).build();
    // private final ObjectMapper objectMapper = new ObjectMapper();
    private String accessToken;
    private GraphServiceClient<?> graphClient;
    private AlfrescoConstants.eItemtype itemtype;
    private String newDeltaLink = "";
    
    public GraphService() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public String getGraphToken() throws MalformedURLException, ExecutionException, InterruptedException {

        // Build confidential client application
        ConfidentialClientApplication app = ConfidentialClientApplication.builder(
                AlfrescoConstants.clientId,
                ClientCredentialFactory.createFromSecret(AlfrescoConstants.clientSecret))
                .authority("https://login.microsoftonline.com/" + AlfrescoConstants.tenantId)
                .build();

        // Scopes for client credentials flow
        // Set<String> scopes =
        // Collections.singleton("https://graph.microsoft.com/.default");

        // Acquire token
        IAuthenticationResult result = app.acquireToken(parameters).get();
        this.accessToken = result.accessToken();
        return this.accessToken;
    }

    private static final List<String> scopes = new ArrayList<>(AlfrescoConstants.GraphScopes);

    public GraphServiceClient<?> getGraphClient(String tenantId) {

        // Build the credential
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .tenantId(tenantId)
                .clientId(AlfrescoConstants.clientId)
                .clientSecret(AlfrescoConstants.clientSecret)
                .build();

        // Wrap credential in Graph auth provider
        TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(scopes, credential);

        // Build Graph client
        this.graphClient = GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();

        return this.graphClient;
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

            String json = mapper.writeValueAsString(body);

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

    public String updateSharepointItemGraphAPI(RelocateInformationObject node, String listItemId) {
        try {
            // First obtain new UUID and accesstoken
            String AccessToken = getGraphToken();
            String uuid = GetUUID();
            HttpClient client = HttpClient.newHttpClient();

            // Build payload to update Title and ObjectClassificationText
            Map<String, Object> body = new HashMap<>();
            body.put("ContAInUUID", node.getUuid());
            body.put("Title", node.getTitle());
            body.put("ObjectClassificationText", "Changed from Java after move");

            String json = mapper.writeValueAsString(body);

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

    public void uploadAlfrescoNodeToSP(RelocateInformationObject IOobject) {
        try {
            String accessToken = getGraphToken();
            byte[] fileBytes = IOobject.getContent();
            String fileName = IOobject.getFileName();
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
                // ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response.body());
                driveItemId = rootNode.path("id").asText(); // DriveItem ID
                System.out.println("Uploaded file DiveItemID: " + driveItemId);
            } else {
                System.err.println("Failed to upload file: " + response.statusCode());
                System.err.println(response.body());
            }

            // Step 1 : Get new listitemid
            String listItemId = getListItemId(driveId, driveItemId);
            String retval = updateSharepointItemGraphAPI(IOobject, listItemId);
            // Function should return something in the future for transaction purposes

        } catch (Exception e) {
            System.err.println("Failed to move Alfresco node: " + e);
            e.printStackTrace();
        }
    }


    //REMOVE FROM HERE, IS ALFRESCOCONTROLLER
    private void uploadSPItemToAlfresco(RelocateInformationObject IOobject) {
        try {
            String accessToken = getGraphToken();
            byte[] fileBytes = IOobject.getContent();
            String fileName = IOobject.getFileName();
            String driveItemId = "";
            // to do check null
            String driveId = getDriveID();
            // String alfrescoEndpoint = String.format(
            // "https://YOUR_ALFRESCO_SERVER/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s/children",
            // alfrescoParentNodeId
            // );
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
                // ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response.body());
                driveItemId = rootNode.path("id").asText(); // DriveItem ID
                System.out.println("Uploaded file DiveItemID: " + driveItemId);
            } else {
                System.err.println("Failed to upload file: " + response.statusCode());
                System.err.println(response.body());
            }

            // Step 1 : Get new listitemid
            String listItemId = getListItemId(driveId, driveItemId);
            String retval = updateSharepointItemGraphAPI(IOobject, listItemId);
            // Function should return something in the future for transaction purposes

        } catch (Exception e) {
            System.err.println("Failed to move Alfresco node: " + e);
            e.printStackTrace();
        }
    }

    public void ProcessChangedSharepointItems(Notification notification) {
        String lastDeltaLink = null;
        String driveId = null;
        String siteId = null;
        String siteGUID = null;
        String domain = null;
        String listId = null;
        try {
            List<String> lines = Files.readAllLines(Paths.get(AlfrescoConstants.DeltaLinkFile));
            String resourceValue = notification.getResource();
            // Find the first line that contains the resource
            Optional<String> match = lines.stream()
                    .filter(line -> line.contains(resourceValue))
                    .findFirst();

            if (match.isPresent()) {
                lastDeltaLink = match.get().split("\\|")[1]; // take the part after '|'
            }
            // Assume value is your Notification object

            // Get Graph token (assuming graphService has a synchronous method or you wrap
            // it in CompletableFuture)
            String accessToken = getGraphToken();

            // Extract siteId and listId from resource URL
            String[] parts = resourceValue.split("/");

            if (resourceValue.contains("drives")) {
                driveId = parts[2];

                // Get drive info (synchronously for now)
                SharePointDriveInfo driveInfo = getListInfoFromDriveID(driveId, AlfrescoConstants.tenantId);
                // value.getTenantId());

                siteGUID = driveInfo.getSiteId();
                domain = driveInfo.getSiteUrl().split("/")[2];
                listId = driveInfo.getListId();

                // siteId = domain + "," + siteGUID + "," + driveInfo.getWebId();

            } else {
                if (parts.length >= 4) {
                    siteId = parts[2];
                    String[] siteParts = siteId.split(",");
                    domain = siteParts[0];
                    siteGUID = siteParts[1];
                    listId = parts[4];
                }
            }
            String accesstoken = getGraphToken();
            ChangedItemsResult changedItems = getChangedItems(AlfrescoConstants.tenantId, siteId, listId, driveId,
                    lastDeltaLink);
            // Now get the SP items
            List<SharePointItemResponse> items = getListItemsByIds(siteId, listId, changedItems.changedItems);
            for (SharePointItemResponse SPItem : items) {
                if (!SPItem.HasUUID) {
                    // AssignUUID
                    SPItem.UUID = updateSharepointItemGraphAPI(SPItem.id);
                }
                if (SPItem.MustMove) {
                    AlfrescoNodeController aController = new AlfrescoNodeController();
                    aController.uploadSPItemToAlfresco();
                    //Remove from SP
                }
            }

            //If no exeptions, update NewDeltaLink


        } catch (Exception ex) {
            System.out.println("Error reading file or delta link not yet registered: " + ex.getMessage());
            lastDeltaLink = null; // treat as first run
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
            // ObjectMapper objectMapper = new ObjectMapper();

            // Parse JSON string into JsonNode
            JsonNode rootNode = mapper.readTree(responseBody);
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
            int retryCount = 10;
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
                System.out.println(contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.MAGENTA
                        + "Try " + retryCounter + " ->  Get new listitemId for driveitemID " + driveItemId
                        + contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RESET);

                HttpResponse<String> response = HttpClient.newHttpClient().send(lirequest,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    // ObjectMapper mapper = new ObjectMapper();
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

    public SharePointDriveInfo getListInfoFromDriveID(String driveId, String tenantId) {
        try {
            GraphServiceClient<?> graphClient = getGraphClient(tenantId);
            if (graphClient == null) {
                System.out.println("Graph client is null");
                return null;
            }

            // Get the Drive
            Drive drive = graphClient.drives(driveId)
                    .buildRequest()
                    .select("id,name,driveType,sharepointIds")
                    .get();

            if (drive == null) {
                System.out.println("Drive not found.");
                return null;
            }

            AdditionalDataManager adm = drive.additionalDataManager();

            if (adm.containsKey("sharepointIds")) {
                Object spObj = adm.get("sharepointIds");
                if (spObj instanceof Map) {
                    Map<String, Object> spMap = (Map<String, Object>) spObj;
                    System.out.println("SharePointIds: " + spMap);
                    // Example: get siteId
                    String siteId = (String) spMap.get("siteId");
                    System.out.println("Site ID: " + siteId);
                }
            }
            SharePointDriveInfo info = new SharePointDriveInfo();
            info.setDriveId(drive.id);
            info.setDriveName(drive.name);
            info.setDriveType(drive.driveType);

            /*
             * if (sp != null) {
             * info.setSiteUrl(sp.siteUrl);
             * info.setTenantID(sp.tenantId);
             * info.setSiteId(sp.siteId);
             * info.setWebId(sp.webId);
             * info.setListId(sp.listId);
             * info.setListItemId(sp.listItemId);
             * }
             */
            info.setListName(drive.name);

            return info;

        } catch (Exception ex) {
            System.out.println("Error fetching drive ID: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    public ChangedItemsResult getChangedItems(String tenantId, String siteId, String listId, String driveId,
            String deltaLink) throws Exception {
        List<String> changedItems = new ArrayList<>();
        String newDeltaLink = null;

        // Determine delta URL
        String deltaUrl;
        if (deltaLink != null && !deltaLink.isEmpty()) {
            deltaUrl = deltaLink;
        } else if (driveId != null && !driveId.isEmpty()) {
            deltaUrl = "https://graph.microsoft.com/v1.0/drives/" + driveId + "/root/delta";
            this.itemtype = AlfrescoConstants.eItemtype.Graph;
        } else if (listId != null && !listId.isEmpty()) {
            deltaUrl = "https://graph.microsoft.com/v1.0/sites/" + siteId + "/lists/" + listId + "/items/delta";
            this.itemtype = AlfrescoConstants.eItemtype.SharePoint;
        } else {
            throw new IllegalArgumentException("Either listId or driveId must be provided.");
        }

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(deltaUrl))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Retry if deltaLink expired
        if (response.statusCode() == 400) {
            System.out.println("Delta link expired. Starting fresh.");
            deltaUrl = "https://graph.microsoft.com/v1.0/sites/" + siteId + "/lists/" + listId + "/items/delta";
            request = HttpRequest.newBuilder()
                    .uri(URI.create(deltaUrl))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "Graph delta call failed: " + response.statusCode() + " - " + response.body());
            }
        } else if (response.statusCode() != 200) {
            throw new RuntimeException("Graph delta call failed: " + response.statusCode() + " - " + response.body());
        }

        // Parse JSON response
        // ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());

        if (root.has("value")) {
            for (JsonNode item : root.get("value")) {
                if (item.has("id")) {
                    changedItems.add(item.get("id").asText());
                }
            }
        }

        if (root.has("@odata.deltaLink")) {
            this.newDeltaLink = root.get("@odata.deltaLink").asText();
        }

        return new ChangedItemsResult(changedItems, newDeltaLink, this.itemtype);
    }

    /**
     * Fetch specific SharePoint list items by IDs
     *
     * @param siteId  SharePoint Site ID
     * @param listId  SharePoint List ID
     * @param itemIds List of item IDs to fetch
     * @return List of ListItem objects
     */
    public List<SharePointItemResponse> getListItemsByIds(String siteId, String listId, List<String> changedItemsIds) {
        List<ListItem> items = new ArrayList<>();
        List<SharePointItemResponse> SPResponseItems = new ArrayList<>();
        boolean hasUUID = false;
        boolean mustMove = false;
        GraphServiceClient<?> graphClient = getGraphClient(AlfrescoConstants.tenantId);
        for (String itemId : changedItemsIds) {
            try {
                ListItem item = graphClient.sites(siteId)
                        .lists(listId)
                        .items(itemId)
                        .buildRequest()
                        .select("id,fields,createdBy,createdDateTime,contentType") // top-level props
                        .expand("fields") // <-- Important
                        .get();
                items.add(item);
            } catch (GraphServiceException gse) {
                if (gse.getResponseCode() == 404) {
                    // ✅ Item not found – handle separately
                    System.err.println("Item NOT FOUND: " + itemId);
                    // e.g. mark for deletion, skip, log, etc.
                    continue;
                }
            } catch (Exception e) {
                System.err.println("Failed to fetch item ID: " + itemId + " -> " + e.getMessage());
            }
        }
        if (!items.isEmpty()) {
            try {
                for (ListItem li : items) {
                    FieldValueSet fields = li.fields;
                    AlfrescoConstants.ContainPlatforms moveTo = null;
                    SharePointItemResponse SPItem = null;
                    if (fields != null) {
                        AdditionalDataManager adm = fields.additionalDataManager();
                        Object moveValue = adm.get("Move");
                        String moveStr = moveValue != null ? moveValue.toString().replace("\"", "") : "";
                        String uuidValue = adm.get("ContAInUUID") != null ? adm.get("ContAInUUID").toString() : "";
                        hasUUID = uuidValue != null && !uuidValue.isEmpty();

                        for (AlfrescoConstants.ContainPlatforms type : AlfrescoConstants.ContainPlatforms.values()) {
                            // System.out.println("Check if " + type + " matches" + moveStr + " for itemid "
                            // + li.id);
                            if (type.name().equalsIgnoreCase(moveStr)) {
                                System.out.println("Found matching platform: " + type);
                                moveTo = type;
                                mustMove = true;
                            }
                        }
                        // Now create only the objects that require actions
                        if (!hasUUID || mustMove) {
                            // Convert SDK object to JSON string
                            try {
                                String json = mapper.writeValueAsString(li);
                                SPItem = mapper.readValue(json, SharePointItemResponse.class);
                                SPItem.HasUUID = hasUUID;
                                SPItem.MustMove = mustMove;
                                SPResponseItems.add(SPItem);
                                SPItem.MoveTo = moveTo;
                            } catch (Exception e) {
                                System.err.println("Failed to fetch item ID: " + li + " -> " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
        return SPResponseItems;
    }
}
