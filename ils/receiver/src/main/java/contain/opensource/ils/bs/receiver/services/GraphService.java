package contain.opensource.ils.bs.receiver.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.JsonElement;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.Drive;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemVersion;
import com.microsoft.graph.models.FieldValueSet;
import com.microsoft.graph.models.ListItem;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.serializer.AdditionalDataManager;

import contain.opensource.ils.bs.receiver.classes.Binding.BindRequest;
import contain.opensource.ils.bs.receiver.classes.Logger.IOLog;
import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoNodeController;
import contain.opensource.ils.bs.receiver.classes.sharepoint.SharePointDriveInfo;
import contain.opensource.ils.bs.receiver.classes.sharepoint.SharePointItemResponse;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.shared.constants.AlfrescoConstants;
import io.swagger.v3.oas.annotations.Parameter;

//========================================================================
//THIS CLASS IS WAY TO BIG AND SHOULD BE SPLIT
// Implement helper classes etc.
// Also the request with different endpooints is repeated way to often. This could be simplified big time  by converting this into one singe function!!!!
//For the POC this will DO
//========================================================================
@Service
public class GraphService {

    static ObjectMapper mapper = new ObjectMapper();

    static String tenantDomain = "lls6.Sharepoint.com";

    static String SiteID = "d155b09d-c4de-4d04-8b37-198f35e78232";
    static String SiteName = "SP-EventReceivers-Test";
    static String ListId = "9358df3d-0b30-4f09-a063-d1d8dcaeccd3";
    static String ListName = "Shared Documents";

    private String newDeltaLink = "";
    private String DeltaLinkFile = null;
    private static final ClientCredentialParameters parameters = ClientCredentialParameters
            .builder(AlfrescoConstants.GraphScopes).build();
    static String accessToken;
    static GraphServiceClient<?> graphClient;
    static AlfrescoConstants.eItemtype itemtype;
    static ILSRestProperties ILSProperties = null;
    static AlfrescoNodeController acontroller = null;

    @Autowired
    public GraphService(ILSRestProperties ilsProperties, AlfrescoNodeController alfresconodecontroller) {
        this.ILSProperties = ilsProperties;
        mapper = new ObjectMapper();
        this.DeltaLinkFile = ILSProperties.getdeltalinkfile();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.acontroller = alfresconodecontroller;
    }

    public static byte[] getSPItemContentById(String itemId, String ListId) throws IOException, InterruptedException {
        // First obtain SiteCollectionID
        String sitecollectionid = "";
        try {
            sitecollectionid = getSitecollectionID();
        } catch (Exception ex) {
            System.out.println("Error retrieving sitecollectionID " + ex.getMessage());
        }

        String endpoint = String.format("https://graph.microsoft.com/v1.0/sites/%s/lists/%s/items/%s/driveItem/content",
                sitecollectionid, ListId, itemId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/octet-stream")
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() == 302) {
            String redirectUrl = response.headers().firstValue("Location").orElseThrow();
            HttpRequest redirectRequest = HttpRequest.newBuilder()
                    .uri(URI.create(redirectUrl))
                    .GET()
                    .build();

            response = client.send(redirectRequest,
                    HttpResponse.BodyHandlers.ofInputStream());
        }

        if (response.statusCode() == 200) {
            try (InputStream is = response.body()) {
                return is.readAllBytes();
            }
        } else {
            // throw new IOException("Failed to fetch SharePoint item. Status: " +
            // response.statusCode());
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.RED
                    + "Failed to fetch SharePoint item.  " + itemId
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
            return null;
        }
    }

    private static final List<String> scopes = new ArrayList<>(AlfrescoConstants.GraphScopes);

    public static GraphServiceClient<?> getGraphClient(String tenantId) {

        // Build the credential
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .tenantId(tenantId)
                .clientId(AlfrescoConstants.clientId)
                .clientSecret(AlfrescoConstants.clientSecret)
                .build();

        // Wrap credential in Graph auth provider
        TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(scopes, credential);

        // Build Graph client
        graphClient = GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();

        return graphClient;
    }

    public static String updateSharepointItemGraphAPI(
            @Parameter(description = "List Item ID") @RequestParam String listItemId,
            @Parameter(description = "List ID") @RequestParam String ListId) {
        try {
            if (accessToken == null || accessToken.isEmpty()) {
                accessToken = getGraphToken();
            }

            // First obtain new UUID and accesstoken
            String uuid = "";
            // String uuid =
            // UUIDUtil.getUUIDOverHTTP(Optional.of(AlfrescoConstants.ContainPlatforms.SPO));
            System.out.println(
                    AlfrescoConstants.RED + "Get UUID endpoint  : "
                            + ILSProperties.getuudiutilendpoint() + AlfrescoConstants.RESET);

            String query = "?prefix=" + AlfrescoConstants.ContainPlatforms.SPO;

            String urlString = ILSProperties.getuudiutilendpoint() + query;

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int status = conn.getResponseCode();
            System.out.println(
                    "Accessing uuid rest url on " + ILSProperties.getuudiutilendpoint() + " return code -> " + status);

            if (status == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                uuid = in.readLine(); // assuming API returns plain UUID
                in.close();
            }

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
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();

            // Send request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println(contain.opensource.shared.constants.AlfrescoConstants.MAGENTA
                        + "UUID assigned updated successfully to : " + listItemId
                        + contain.opensource.shared.constants.AlfrescoConstants.RESET);
                // Cahc in Redis
                // RedisManager.putHash("IOinUUIDAssigned", "IOinUUIDAssigned" + uuid,
                // "InProcess", 2400);
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
            if (accessToken == null || accessToken.isEmpty()) {
                accessToken = getGraphToken();
            }
            // String uuid = GetUUID();
            HttpClient client = HttpClient.newHttpClient();

            // Build payload to update Title and ObjectClassificationText
            Map<String, Object> body = new HashMap<>();
            body.put("ContAInUUID", node.getUuid());
            body.put("Title", node.getTitle());
            body.put("Marking", node.getMarking());
            body.put("Classification", node.getclassification());

            // Graph API will not allow description field to be a[dated. Even wordso./ If
            // this field is added the entire update fails!
            // Presumably it will work using SP RestAPI but then the Graph token will not
            // work.
            // I tried using the SP token in the past but i did not get it to work.

            // body.put("Description", node.getDescription());
            body.put("containIODescription", node.getDescription());
            body.put("ObjectClassificationText", "Changed from Java after move");

            String json = mapper.writeValueAsString(body);

            String endpoint = String.format(
                    "https://graph.microsoft.com/v1.0/sites/%s/lists/%s/items/%s/fields",
                    this.SiteID, ListId, listItemId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();

            // Send request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("Item  : " + node.getFileName() + " moved to SharePoint succesfully");
                return "Success";
            } else {
                System.out.println("Failed to update field: " + response.body());
                return "Failed";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Failed";
        }
    }

    public void uploadAlfrescoNodeToSP(RelocateInformationObject IOobject) {
        try {
            if (accessToken == null || accessToken.isEmpty()) {
                accessToken = getGraphToken();
            }
            byte[] fileBytes = IOobject.getContent();
            String rawFileName = IOobject.getFileName();
            String fileName = URLEncoder.encode(rawFileName, StandardCharsets.UTF_8).replace("+", "%20"); // IMPORTANT
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
            // Prevent webhooklistener to update UUID and other fields prior to completion.
            // In future the webhooklistener should place messages on the queue and this
            // should become obsolete.ILSApplication
            String listItemId = getListItemId(driveId, driveItemId);
            // UniqueIdentifier
            String UID = this.SiteID + this.ListId + listItemId;
            // Globals.AlfrescoItemInProcess.add(UID);
            String retval = updateSharepointItemGraphAPI(IOobject, listItemId);
            // Globals.AlfrescoItemInProcess.remove(UID);
            // Function should return something in the future for transaction purposes

        } catch (Exception e) {
            System.err.println("Failed to move Alfresco node: " + e);
            e.printStackTrace();
        }
    }

    private static void BindObject(SharePointItemResponse SPItem) {
        // First sign and log
        try {
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_MAGENTA
                    + contain.opensource.shared.constants.AlfrescoConstants.RED
                    + ("Binding SP IO " + SPItem.UUID)
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
            // PrivateKey key = PKCS12KeyLoader.PK;
            PrivateKey key;
            // Create request WITHOUT key
            BindRequest request = new BindRequest(
                    SPItem.ToSecuredDocument());

            String endPoint = ILSProperties.getbindendpoint();
            System.out
                    .println(contain.opensource.shared.constants.AlfrescoConstants.RED
                            + "Binding endpoint  : "
                            + endPoint
                            + contain.opensource.shared.constants.AlfrescoConstants.RESET);

            URL url = new URL(endPoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");

            int status = conn.getResponseCode();
            System.out.println("Accessing uuid rest url on " + endPoint + " return code -> " + status);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<BindRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<String> bindresponse = restTemplate.postForEntity(endPoint, entity,
                    String.class);

            // Move log to binding function
            String action = "IO MODIFIED. BIND IO " + SPItem.UUID + "  to new SharePoint IO " + SPItem.filename;
            if (bindresponse.getStatusCode().value() == 200) {
                IOLog.log(
                        SPItem.UUID,
                        SPItem.id,
                        SPItem.Path,
                        action,
                        AlfrescoConstants.ContainPlatforms.SPO.toString(),
                        AlfrescoConstants.ContainPlatforms.SPO.toString(),
                        bindresponse.getBody(),
                        SPItem.filename,
                        "",
                        AlfrescoConstants.eActionPerformed.IOBOUND,
                        "System",
                        SPItem.marking,
                        SPItem.classification,
                        SPItem.version);
                // ==========================================================================================
            }
        } catch (Exception ex) {
            System.err.println("Failed to bind object: " + ex);
            ex.printStackTrace();
        }
    }

    public void RelocateIO(RelocateInformationObject ROobject) throws Exception {
        try {

            String action = "Copy UUID " + ROobject.getUuid() + " : " + ROobject.getFileName() +
                    " from "
                    + ROobject.getPlatfrom() + " to " + ROobject.getPlatformTo();

            // Upload to Alfresco
            this.acontroller.uploadSPItemToAlfresco(ROobject);

            // log
            IOLog.log(
                    ROobject.getUuid(),
                    ROobject.getId(),
                    "",
                    action,
                    ROobject.getPlatfrom().toString(),
                    ROobject.getPlatformTo().toString(),
                    // ROobject.getHash(),
                    "BOUND ON DESTINATION PLATFORM",
                    ROobject.getFileName(),
                    "",
                    AlfrescoConstants.eActionPerformed.IOCOPIED,
                    "System",
                    ROobject.marking,
                    ROobject.classification,
                    ROobject.version);
            // Delete from SP (If no exception. This is to be implemented for persistance
            // and transactions)
            deleteSPItemById(ROobject.getId());

            // Log
            action = "Deleted  UUID " + ROobject.getUuid() + " : " + ROobject.getFileName()
                    + " from "
                    + ROobject.getPlatfrom();
            IOLog.log(
                    "DeletedFromPlatform",
                    ROobject.getId(),
                    "",
                    action,
                    ROobject.getPlatfrom().toString(),
                    ROobject.getPlatfrom().toString(),
                    "DeletedFromPlatform",
                    ROobject.getFileName(),
                    "",
                    AlfrescoConstants.eActionPerformed.IODELETED,
                    "System",
                    ROobject.marking,
                    ROobject.classification,
                    ROobject.version);
        } catch (Exception ex) {
            // System.out.println("Failed to relocate IO: " + ex.getMessage());
            // ex.printStackTrace();
            throw ex;
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
                System.out.println(contain.opensource.shared.constants.AlfrescoConstants.MAGENTA
                        + "Try " + retryCounter + " ->  Get new listitemId for driveitemID " + driveItemId
                        + contain.opensource.shared.constants.AlfrescoConstants.RESET);

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
            info.setListName(drive.name);

            return info;

        } catch (Exception ex) {
            System.out.println("Error fetching drive ID: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    private static String getSitecollectionID() throws IOException, InterruptedException {
        String endpoint = String.format("https://graph.microsoft.com/v1.0/sites/%s:/sites/%s",
                tenantDomain.toLowerCase(), SiteName);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() == 200) {
            try (InputStream is = response.body()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(is);
                return json.get("id").asText(); // composite siteId
            }
        } else {
            throw new IOException("Failed to fetch SharePoint item. Status: " + response.statusCode());
        }
    }

    private void deleteSPItemById(String itemId) throws Exception {
        try {
            String endpoint = String.format(
                    "https://graph.microsoft.com/v1.0/sites/%s:/sites/%s:/lists/%s/items/%s",
                    this.tenantDomain, this.SiteName, this.ListId, itemId);

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .DELETE()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204) {
                System.out.println("Document deleted successfully.");
            } else {
                throw new RuntimeException(
                        "Failed to delete document. HTTP "
                                + response.statusCode() + " - " + response.body());
            }
        } catch (Exception ex) {
            System.out.println("Failed to delete SP item: " + ex.getMessage());
        }
    }

    public static String getGraphToken() throws MalformedURLException, ExecutionException, InterruptedException {
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
        accessToken = result.accessToken();
        return accessToken;
    }

    private static boolean itemmustmigrate(SharePointItemResponse SPItem) {
        String endpoint = ILSProperties.getruleenginemoveendpoint();

        // Haal de raw JSON primitives uit de additionalDataManager
        // JsonPrimitive markingJson = (JsonPrimitive)
        // li.fields.additionalDataManager().get("Marking");
        // JsonPrimitive classificationJson = (JsonPrimitive)
        // li.fields.additionalDataManager().get("Classification");
        // String cleanClassification = (classificationJson != null)
        // ? classificationJson.getAsString().replace("\"", "").trim()
        // : "";
        // String cleanMarking = (markingJson != null)
        // ? markingJson.getAsString().replace("\"", "").trim()
        // : "";

        String cleanPlatformFrom = SPItem.containplatformfrom.name().replace("\"", "");
        String cleanClassification = SPItem.classification.replace("\"", "");
        String cleanMarking = SPItem.marking.replace("\"", "");
        String cleancontainfromcontainer = SPItem.containfromcontainer.replace("\"", "");

        // Bouw de URL exact zoals Swagger het doet
        URI targetUri = UriComponentsBuilder.fromHttpUrl(ILSProperties.getruleenginemoveendpoint())
                .queryParam("platformfrom", cleanPlatformFrom)
                .queryParam("containerfrom", cleancontainfromcontainer)
                .queryParam("classification", cleanClassification)
                .queryParam("marking", cleanMarking)
                .build()
                .encode()
                .toUri();

        HttpClient client = HttpClient.newHttpClient();
        // 1. Bouw de Request (gebruik de URL die je net hebt samengesteld)
        HttpRequest request = HttpRequest.newBuilder()
                .uri(targetUri)
                .GET() // Of .POST(BodyPublishers.noBody()) afhankelijk van je endpoint
                .build();

        // 2. Verstuur de aanvraag en vang de response op
        // Let op: client.send gooit Checked Exceptions (IOException,
        // InterruptedException)
        String responseBody = null;
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 3. Controleer de statuscode
            int statusCode = response.statusCode();
            responseBody = response.body();

            if (statusCode == 200) {
                System.out.println("Succes! Response: " + responseBody);
            } else {
                System.err.println("Fout van Rule Engine: " + statusCode + " - " + responseBody);
            }
        } catch (IOException | InterruptedException e) {
            // Log de fout als de Rule Engine onbereikbaar is
            e.printStackTrace();
        }

        return responseBody != null && !responseBody.isEmpty();
    }

    // public SharePointItemResponse getListItemsById(String listId, String
    // listItemId,GraphServiceClient<?> graphClient)
    public static SharePointItemResponse getListItemsById(String listId, String listItemId)
            throws Exception {
        try {
            if (accessToken == null || accessToken.isEmpty()) {
                accessToken = getGraphToken();
            }
            boolean hasUUID = false;
            boolean mustMove = false;
            GraphServiceClient<?> graphClient = getGraphClient(AlfrescoConstants.tenantId);
            String SiteId = getSitecollectionID();
            ListItem li = graphClient.sites(SiteId)
                    .lists(listId)
                    .items(listItemId)
                    .buildRequest()
                    .select("id,fields,createdBy,createdDateTime,contentType") // top-level props
                    .expand("fields($select=Title,containIODescription,ContAInUUID,LinkFilename,Move,OData__UIVersionString,Marking,classification),driveItem")
                    .get();
            if (li != null) {
                FieldValueSet fields = li.fields;
                AlfrescoConstants.ContainPlatforms moveTo = null;
                SharePointItemResponse SPItem = null;
                if (fields != null) {
                    AdditionalDataManager adm = fields.additionalDataManager();
                    for (Map.Entry<String, JsonElement> entry : adm.entrySet()) {
                        String key = entry.getKey();
                        JsonElement value = entry.getValue();

                        // Example: convert to String
                        if (value != null && !value.isJsonNull()) {
                            try {
                                String valueStr = value.getAsString();
                                // System.out.println(key + " = " + valueStr);
                            } catch (Exception e) {
                                System.err.println("Failed to fetch SP field value" + e.getMessage());
                            }
                        }
                    }
                    // Object moveValue = adm.get("Move");
                    // String moveStr = moveValue != null ? moveValue.toString().replace("\"", "") :
                    // "";
                    String uuidValue = adm.get("ContAInUUID") != null ? adm.get("ContAInUUID").toString() : "";
                    DriveItem driveItem = li.driveItem;
                    String mimeType = (driveItem != null && driveItem.file != null) ? driveItem.file.mimeType
                            : null;

                    hasUUID = uuidValue != null && !uuidValue.isEmpty();

                    // Get latest version
                    DriveItemVersion latestVersion = graphClient.sites(SiteID)
                            .lists(listId)
                            .items(li.id)
                            .driveItem()
                            .versions()
                            .buildRequest()
                            .top(1) // newest version first
                            .get()
                            .getCurrentPage()
                            .get(0);

                    // Now create only the objects that require actions. reove because for binding
                    try {
                        String json = mapper.writeValueAsString(li);
                        SPItem = mapper.readValue(json, SharePointItemResponse.class);
                        Object title = adm.get("Title");
                        Object filename = driveItem.name;
                        Object description = adm.get("containIODescription");
                        String classification = adm.get("Classification").toString();
                        String marking = adm.get("Marking").toString();
                        String titleStr = title != null ? title.toString().replace("\"", "") : "";
                        String filenameStr = filename != null ? filename.toString().replace("\"", "") : "";
                        String descriptionStr = description != null ? description.toString().replace("\"", "") : "";
                        SPItem.title = titleStr;
                        SPItem.filename = filenameStr;
                        SPItem.description = descriptionStr;
                        SPItem.mimetype = mimeType;
                        SPItem.Path = driveItem.webUrl;
                        SPItem.UUID = uuidValue;
                        SPItem.containplatformfrom = AlfrescoConstants.ContainPlatforms.SPO;
                        SPItem.containfromcontainer = tenantDomain + "/" + SiteName + "/" + ListId;
                        // String description = (String) fields.get("Description");
                        // To do get file content
                        SPItem.HasUUID = hasUUID;
                        SPItem.version = latestVersion.id;
                        SPItem.marking = marking;
                        SPItem.classification = classification;
                        SPItem.filecontent = getSPItemContentById(li.id, listId);
                    } catch (Exception e) {
                        System.err.println("Failed to fetch item ID: " + li + " -> " + e.getMessage());
                        throw e;
                    }
                }
                // Validate if item should move
                // mustMove = itemmustmigrate(li);
                SPItem.MustMove = itemmustmigrate(SPItem);

                if (mustMove) {
                    System.out.println("Item " + li.id + " marked for move to " + moveTo);
                }
                return SPItem;
            }
            return null;
        } catch (GraphServiceException ex) {
            // Catch Graph-specific errors
            if ("itemNotFound".equals(ex.getServiceError().code)) {
                System.out.println("The item was not found (itemNotFound). Ignoring...");
                return null;
            } else {
                // rethrow other GraphServiceExceptions
                throw ex;
            }
        } catch (Exception ex) {
            System.out.println("Failed to get list items by id: " + ex.getMessage());
            ex.printStackTrace();
            throw ex;
        }
    }

    public static boolean ProcessChangedSharepointItem(String ItemWebUrl, String ListItemID, String resourceValue)
            throws MalformedURLException, Exception {
        String siteId = null;
        String driveId = null;
        // String siteGUID = null;
        // String listId = null;
        String action = "";
        boolean MustMove = false;
        try {
            if (accessToken == null || accessToken.isEmpty()) {
                accessToken = getGraphToken();
            }
            String[] parts = ItemWebUrl.split("/");
            tenantDomain = parts[2];
            SiteName = parts[4];
            ListName = parts[5];

            parts = resourceValue.split("/");
            ListId = parts[7];
            siteId = parts[5];
            String[] siteParts = siteId.split(",");
            SiteID = siteParts[1];
            // Single tennant for now
            GraphServiceClient<?> graphClient = getGraphClient(tenantDomain);
            // SharePointItemResponse SPItem = getListItemsById(this.ListId, ListItemID,
            // graphClient );
            SharePointItemResponse SPItem = getListItemsById(ListId, ListItemID);
            if (SPItem != null) {
                MustMove = SPItem.MustMove;
                // Check if this item is to me migrated based on ruleengine

                if (!SPItem.MustMove && SPItem.HasUUID) {
                    System.out.println("Changes detected on item : " + SPItem.id + " , only rebind required.");
                }
                if (!SPItem.HasUUID) {
                    SPItem.UUID = updateSharepointItemGraphAPI(SPItem.id, ListId);
                    action = "Assign UUID " + SPItem.UUID + "  to new SharePoint IO " + SPItem.filename;

                    IOLog.log(
                            SPItem.UUID,
                            SPItem.id,
                            SPItem.Path,
                            action,
                            AlfrescoConstants.ContainPlatforms.SPO.toString(),
                            AlfrescoConstants.ContainPlatforms.SPO.toString(),
                            "NewOnPlatform",
                            SPItem.filename,
                            "",
                            AlfrescoConstants.eActionPerformed.ASSIGNUUID,
                            "System",
                            SPItem.marking,
                            SPItem.classification,
                            SPItem.version);
                    return MustMove;
                }
                BindObject(SPItem);
                return MustMove;
            }
        } catch (Exception ex) {
            System.err.println("Failed to process changed SP item: " + ex);
            ex.printStackTrace();
            throw ex;
        }
        return MustMove;
    }
}