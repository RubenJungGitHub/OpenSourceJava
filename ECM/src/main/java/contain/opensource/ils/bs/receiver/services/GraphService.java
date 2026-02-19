package contain.opensource.ils.bs.receiver.services;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

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
import contain.opensource.ils.bs.receiver.classes.Binding.PKCS12KeyLoader;
import contain.opensource.ils.bs.receiver.classes.ConfigurationProperties.ILSRestProperties;
import contain.opensource.ils.bs.receiver.classes.Logger.IODeltaLinkLog;
import contain.opensource.ils.bs.receiver.classes.Logger.IOLog;
import contain.opensource.ils.bs.receiver.classes.Logger.IOLogDeltaLink;
import contain.opensource.ils.bs.receiver.classes.Notification;
import contain.opensource.ils.bs.receiver.classes.Redis.RedisManager;
import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import contain.opensource.ils.bs.receiver.classes.UUIDUtil;
import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoNodeController;
import contain.opensource.ils.bs.receiver.classes.sharepoint.ChangedItemsResult;
import contain.opensource.ils.bs.receiver.classes.sharepoint.SharePointDriveInfo;
import contain.opensource.ils.bs.receiver.classes.sharepoint.SharePointItemResponse;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants;
import io.swagger.v3.oas.annotations.Parameter;


//========================================================================
//THIS CLASS IS WAY TO BIG AND SHOULD BE SPLIT
// Implement helper classes etc.
// Also the request with different endpooints is repeated way to often. This could be simplified big time  by converting this into one singe function!!!!
//For the POC this will DO
//========================================================================
@Service
public class GraphService {

    ObjectMapper mapper = new ObjectMapper();

    private String tenantDomain = "lls6.Sharepoint.com";
    //private String siteGUID = "d155b09d-c4de-4d04-8b37-198f35e78232";
    //private String siteGUID = "";
    //private String SiteName = "SP-EventReceivers-Test";
    //private String SiteName = "";
    //private String ListId = "9358df3d-0b30-4f09-a063-d1d8dcaeccd3";
    //private String ListId ="";
    //private String ListName = "Shared Documents";
    //private String ListName = "";

    private  String SiteID = "d155b09d-c4de-4d04-8b37-198f35e78232";
    private  String SiteName = "SP-EventReceivers-Test";
    private  String ListId = "9358df3d-0b30-4f09-a063-d1d8dcaeccd3";
    private  String ListName = "Shared Documents";

    private String newDeltaLink = "";
    private String DeltaLinkFile = null;
    private final ClientCredentialParameters parameters = ClientCredentialParameters.builder(AlfrescoConstants.GraphScopes).build();
    private String accessToken;
    private GraphServiceClient<?> graphClient;
    private AlfrescoConstants.eItemtype itemtype;
    private ILSRestProperties ILSProperties = null;
    @Autowired
    private AlfrescoNodeController aController;// = new AlfrescoNodeController();

    public GraphService() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Autowired
    public GraphService(ILSRestProperties ilsProperties) {
        this.ILSProperties = ilsProperties;
        this.DeltaLinkFile = ILSProperties.getDeltaLinkFile();
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
            @Parameter(description = "List Item ID") @RequestParam String listItemId,
            @Parameter(description = "List ID") @RequestParam String ListId) {
        try {
            // Initially check if SP item is added because of reloaction
            // String UID = this.SiteID + this.ListId + listItemId;
            // if (Globals.AlfrescoItemInProcess.contains(UID)) {
            // System.out.println("Relocated item, no update required");
            // return "Relocated item, no update required";
            // }

            // First obtain new UUID and accesstoken
            String AccessToken = getGraphToken();
            /// String uuid = GetUUID();
            String uuid = UUIDUtil.getUUIDOverHTTP(Optional.of(AlfrescoConstants.ContainPlatforms.SPO));

            HttpClient client = HttpClient.newHttpClient();

            // Build payload to update Title and ObjectClassificationText
            Map<String, Object> body = new HashMap<>();
            body.put("ContAInUUID", uuid);
            body.put("Title", "Test Updated Document Title Ruben from JaVa");
            body.put("ObjectClassificationText", "Changed from Java");

            String json = mapper.writeValueAsString(body);

            String endpoint = String.format(
                    "https://graph.microsoft.com/v1.0/sites/%s/lists/%s/items/%s/fields",
                    this.SiteID, ListId, listItemId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", "Bearer " + AccessToken)
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();

            // Send request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println(contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.MAGENTA
                        + "UUID assigned updated successfully to : " + listItemId
                        + contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RESET);
                // Cahc in Redis
                RedisManager.putHash("IOinUUIDAssigned", "IOinUUIDAssigned" + uuid, "InProcess", 240);
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
                    .header("Authorization", "Bearer " + AccessToken)
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
            String accessToken = getGraphToken();
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

    private void BindObject(SharePointItemResponse SPItem) {
        // First sign and log
        try {
            System.out.println(contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.BG_YELLOW
                    + contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RED
                    + ("Binding SP IO " + SPItem.UUID)
                    + contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RESET);
            PrivateKey key = PKCS12KeyLoader.PK;
            String privateKeyBase64 = Base64.getEncoder().encodeToString(key.getEncoded());

            BindRequest request = new BindRequest(SPItem.ToSecuredDocument(),
                    privateKeyBase64);
            String endPoint = ILSProperties.getBaseUrl() + "/api/Bind";
            System.out
                    .println(contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RED
                            + "Binding endpoint  : "
                            + endPoint
                            + contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RESET);

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

    public void RelocateIO(RelocateInformationObject ROobject) {
        try {
            String action = "Copy UUID " + ROobject.getUuid() + " : " + ROobject.getFileName() +
                    " from "
                    + ROobject.getPlatfrom() + " to " + ROobject.getPlatformTo();

            // Upload to Alfrewsco
            aController.uploadSPItemToAlfresco(ROobject);

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
            // Delete from SP
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
            System.out.println("Failed to relocate IO: " + ex.getMessage());
            ex.printStackTrace();
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
            info.setListName(drive.name);

            return info;

        } catch (Exception ex) {
            System.out.println("Error fetching drive ID: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    //public SharePointItemResponse getListItemsById(String listId, String listItemId,GraphServiceClient<?> graphClient)
    public SharePointItemResponse getListItemsById(String listId, String listItemId)
            throws Exception {
        try {
            boolean hasUUID = false;
            boolean mustMove = false;
            GraphServiceClient<?> graphClient = getGraphClient(AlfrescoConstants.tenantId);
            String SiteId =getSitecollectionID();
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
                    Object moveValue = adm.get("Move");
                    String moveStr = moveValue != null ? moveValue.toString().replace("\"", "") : "";
                    String uuidValue = adm.get("ContAInUUID") != null ? adm.get("ContAInUUID").toString() : "";
                    DriveItem driveItem = li.driveItem;
                    String mimeType = (driveItem != null && driveItem.file != null) ? driveItem.file.mimeType
                            : null;

                    hasUUID = uuidValue != null && !uuidValue.isEmpty();
                    mustMove = false;
                    for (AlfrescoConstants.ContainPlatforms type : AlfrescoConstants.ContainPlatforms.values()) {
                        // System.out.println("Check if " + type + " matches" + moveStr + " for itemid
                        // "+ li.id);
                        if (type.name().equalsIgnoreCase(moveStr)) {
                            System.out.println("Found matching platform: " + type + " for itemid " + li.id);
                            moveTo = type;
                            mustMove = true;
                        }
                    }

                    if (mustMove) {
                        System.out.println("Item " + li.id + " marked for move to " + moveTo);
                    }

                    // Get latest version
                    DriveItemVersion latestVersion = graphClient.sites(this.SiteID)
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
                        // String description = (String) fields.get("Description");
                        // To do get file content
                        SPItem.HasUUID = hasUUID;
                        SPItem.version = latestVersion.id;
                        SPItem.marking = marking;
                        SPItem.classification = classification;
                        SPItem.MustMove = mustMove;
                        SPItem.MoveTo = moveTo;
                        SPItem.filecontent = getSPItemContentById(li.id, listId);
                    } catch (Exception e) {
                        System.err.println("Failed to fetch item ID: " + li + " -> " + e.getMessage());
                        throw e;
                    }
                }
                return SPItem;
            }
            return null;
        } catch (Exception ex) {
            System.out.println("Failed to get list items by id: " + ex.getMessage());
            ex.printStackTrace();
            throw ex;
        }
    }

    public byte[] getSPItemContentById(String itemId, String ListId) throws IOException, InterruptedException {
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
            System.out.println(contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RED
                    + "Failed to fetch SharePoint item.  " + itemId
                    + contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RESET);
            return null;
        }
    }

  private String getSitecollectionID() throws IOException, InterruptedException {
        String endpoint = String.format("https://graph.microsoft.com/v1.0/sites/%s:/sites/%s",
                this.tenantDomain.toLowerCase(), this.SiteName);

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

    public void ProcessChangedSharepointItem(String ItemWebUrl, String ListItemID, String resourceValue)
            throws MalformedURLException, Exception {
        String siteId = null;
        String driveId = null;
        //String siteGUID = null;
        //String listId = null;
        String action = "";
        try {
            String accesstoken = getGraphToken();
            String [] parts = ItemWebUrl.split("/");
            this.tenantDomain = parts[2];
            this.SiteName = parts[4];
            this.ListName = parts[5];

            parts = resourceValue.split("/");
            this.ListId = parts[7];
            siteId = parts[5];
            String[] siteParts = siteId.split(",");
            this.SiteID = siteParts[1];
            //Single tennant for now
            GraphServiceClient<?> graphClient = getGraphClient(this.tenantDomain);
            //SharePointItemResponse SPItem = getListItemsById(this.ListId, ListItemID, graphClient );
            SharePointItemResponse SPItem = getListItemsById(this.ListId, ListItemID);
            if (!SPItem.MustMove && SPItem.HasUUID) {
                System.out.println("Changes detected on item : " + SPItem.id + " , only rebind required.");
            }
            if (!SPItem.HasUUID) {
                SPItem.UUID = updateSharepointItemGraphAPI(SPItem.id, this.ListId);
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
                return;
            }

            // Redis is redundant. To memcollection? Obsolete?
            // Check double binding -> To become seperate function for all ecm environments

            String redisLogId = SPItem.getUuid();
            ;
            // Add to Redis cache to avoid double binding.
           // for (ContainPlatforms platform : ContainPlatforms.values()) {
          //      redisLogId = redisLogId.replace(platform.toString(), "");
           // }
            String redisentryInRelocation = "IOinRelocateProcess" + redisLogId;
            String redisentryUUIDAssigned = "IOinUUIDAssigned" + SPItem.getUuid();
            if (RedisManager.getHashField(redisentryInRelocation) != null) {
                RedisManager.deleteEntry(redisentryInRelocation);
                return;
            }
            if (RedisManager.getHashField(redisentryUUIDAssigned) != null && SPItem.HasUUID) {
                RedisManager.deleteEntry(redisentryUUIDAssigned);
                return;
            }

            if (SPItem.MustMove) {
                // Relocate item
                try {
                    System.out.println(contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.GREEN
                            + "SPItem mustmove?" + SPItem.MustMove
                            + contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RESET);
                    // Add to relocation cache
                    RedisManager.putHash("IOinProcess", redisentryInRelocation, "InProcess", 240);
                    RelocateInformationObject ROobject = new RelocateInformationObject(SPItem);
                    // ROobject.setHash(bindresponse.getBody());
                    String endpoint = String.format(
                            "%s/RelocateIO",
                            this.ILSProperties.getBaseUrl());
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setBasicAuth(
                            AlfrescoConstants.username,
                            AlfrescoConstants.password,
                            StandardCharsets.UTF_8);
                    RestTemplate restTemplate = new RestTemplate();
                    HttpEntity<RelocateInformationObject> entitymove = new HttpEntity<>(ROobject,
                            headers);

                    ResponseEntity<String> response = restTemplate.postForEntity(endpoint, entitymove,
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
            } else {
                // Bind
                BindObject(SPItem);
            }
        } catch (Exception ex) {
            System.err.println("Failed to process changed SP item: " + ex);
            ex.printStackTrace();
            throw ex;
        }
    }

    
    
//************************************************************************************ */
//VOID BELOW HERE
//************************************************************************************ */
/*

    public void ProcessChangedSharepointItems(Notification notification) {
        String lastDeltaLink = null;
        String driveId = null;
        String siteId = null;
        String siteGUID = null;
        String domain = null;
        String listId = null;
        try {

            // OLD first ensure file exists
            ensureDeltaLinkFileExists();
            String resourceValue = notification.getResource();

            // NEW Read from datastore
            Optional<IOLogDeltaLink> existingLog = IODeltaLinkLog.GetLog(resourceValue);
            if (existingLog.isPresent()) {
                // Get latest token v
                IOLogDeltaLink log = existingLog.get();
                lastDeltaLink = log.getLastDeltaLink();
            }
            // Assume value is your Notification object

            // Get Graph token (assuming graphService has a synchronous method or you wrap
            // it in CompletableFuture)
            // String accessToken = getGraphToken();
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
            ChangedItemsResult changedItems = getChangedItems(AlfrescoConstants.tenantId, siteId, listId, driveId,
                    lastDeltaLink);
            if (changedItems.changedItems == null || changedItems.changedItems.isEmpty()) {
                System.out.println("No changed items detected.");
                // LogNewDeltaLinkToFile();
                return;
            }
            // Now get the SP items
            List<SharePointItemResponse> items = getListItemsByIds(siteId, listId, changedItems.changedItems);
            String action = "";
            for (SharePointItemResponse SPItem : items) {
                // One always needs to get content for binding

                SPItem.filecontent = getSPItemContentById(SPItem.id, listId);
                if (!SPItem.MustMove && SPItem.HasUUID) {
                    System.out.println("Changes detected on item : " + SPItem.id + " but no action required.");
                }
                if (!SPItem.HasUUID) {
                    // AssignUUID
                    SPItem.UUID = updateSharepointItemGraphAPI(SPItem.id, listId);
                    // Test ballenbak
                    action = "Assign UUID " + SPItem.UUID + "  to new SharePoint IO " + SPItem.filename;

                    IOLog.log(
                            SPItem.UUID,
                            SPItem.id,
                            "",
                            action,
                            AlfrescoConstants.ContainPlatforms.SPO.toString(),
                            AlfrescoConstants.ContainPlatforms.SPO.toString(),
                            "NewOnPlatform",
                            SPItem.filename,
                            "",
                            AlfrescoConstants.eActionPerformed.ASSIGNUUID,
                            "System",
                            SPItem.marking,
                            "",
                            SPItem.version);
                }

                // First sign and log
                // Only for ballenbak
                // ==========================================================================================
                // ALL FUNCTIONS SHOULD BE SEPARATED
                // ==========================================================================================

                // Add to Redis cache to avoid double binding.
                String redisentry = "IOinProcess-" + SPItem.UUID.replaceAll("^\"|\"$", "");

                if (RedisManager.getHashField(redisentry) == null) {
                    RedisManager.putHash("IOinProcess", redisentry, "InProcess", 240);
                } else {
                    RedisManager.deleteEntry(redisentry);
                    break;
                }

                // Hash IO (Separate?)
                PrivateKey key = PKCS12KeyLoader.PK;
                String privateKeyBase64 = Base64.getEncoder().encodeToString(key.getEncoded());

                BindRequest request = new BindRequest(SPItem.ToSecuredDocument(),
                        privateKeyBase64);
                String endPoint = ILSProperties.getBaseUrl() + "/api/Bind";
                System.out
                        .println(contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RED
                                + "Binding endpoint  : "
                                + endPoint
                                + contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RESET);

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
                action = "IO MODIFIED. BIND IO " + SPItem.UUID + "  to new SharePoint IO " + SPItem.filename;
                if (bindresponse.getStatusCode().value() == 200) {
                    IOLog.log(
                            SPItem.UUID,
                            SPItem.id,
                            "",
                            action,
                            AlfrescoConstants.ContainPlatforms.SPO.toString(),
                            AlfrescoConstants.ContainPlatforms.SPO.toString(),
                            bindresponse.getBody(),
                            SPItem.filename,
                            "",
                            AlfrescoConstants.eActionPerformed.IOBOUND,
                            "System",
                            SPItem.marking,
                            "",
                            SPItem.version);
                    // ==========================================================================================
                }
                System.out.println(contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.CYAN
                        + "SPItem mustmove?" + SPItem.MustMove
                        + contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RESET);
                if (SPItem.MustMove) {
                    // Relocate item
                    try {
                        RelocateInformationObject ROobject = new RelocateInformationObject(SPItem);
                        ROobject.setHash(bindresponse.getBody());
                        String endpoint = String.format(
                                "%s/RelocateIO",
                                this.ILSProperties.getBaseUrl());
                        headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.setBasicAuth(
                                AlfrescoConstants.username,
                                AlfrescoConstants.password,
                                StandardCharsets.UTF_8);

                        HttpEntity<RelocateInformationObject> entitymove = new HttpEntity<>(ROobject,
                                headers);

                        ResponseEntity<String> response = restTemplate.postForEntity(endpoint, entitymove,
                                String.class);

                        System.out.println("Status: " + response.getStatusCodeValue());
                        System.out.println("Body: " + response.getBody());

                        status = response.getStatusCode().value();
                        if (status != 200) {
                            throw new IOException("HTTP error " + status);
                        }

                    } catch (Exception e) {
                        System.out.println("Failed to delete SP item after move: " + e.getMessage());
                    }
                }
            }
           LogNewDeltaLinkToFile(); // to do only if success
        } catch (Exception ex) {
            System.out.println("Error reading file or delta link not yet registered: " + ex.getMessage());
            lastDeltaLink = null; // treat as first run
        }
    }


    
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
                        .expand("fields($select=Title,containIODescription,ContAInUUID,LinkFilename,Move, OData__UIVersionString, Marking, Label),driveItem")
                        .get();
                items.add(item);
            } catch (GraphServiceException gse) {
                if (gse.getResponseCode() == 404) {
                    // ✅ Item not found – handle separately
                    System.err.println("Item NOT FOUND: " + itemId);
                    // e.g. mark for deletion, skip, log, etc.
                    // continue;
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
                        for (Map.Entry<String, JsonElement> entry : adm.entrySet()) {
                            String key = entry.getKey();
                            JsonElement value = entry.getValue();

                            // Example: convert to String
                            if (value != null && !value.isJsonNull()) {
                                try {
                                    String valueStr = value.getAsString();
                                    System.out.println(key + " = " + valueStr);
                                } catch (Exception e) {
                                    System.err.println("Failed to fetch SP field value" + e.getMessage());
                                }
                            }
                        }
                        Object moveValue = adm.get("Move");
                        String moveStr = moveValue != null ? moveValue.toString().replace("\"", "") : "";
                        String uuidValue = adm.get("ContAInUUID") != null ? adm.get("ContAInUUID").toString() : "";
                        DriveItem driveItem = li.driveItem;
                        String mimeType = (driveItem != null && driveItem.file != null) ? driveItem.file.mimeType
                                : null;

                        hasUUID = uuidValue != null && !uuidValue.isEmpty();
                        mustMove = false;
                        for (AlfrescoConstants.ContainPlatforms type : AlfrescoConstants.ContainPlatforms.values()) {
                            // System.out.println("Check if " + type + " matches" + moveStr + " for itemid
                            // "+ li.id);
                            if (type.name().equalsIgnoreCase(moveStr)) {
                                System.out.println("Found matching platform: " + type + " for itemid " + li.id);
                                moveTo = type;
                                mustMove = true;
                            }
                        }

                        if (mustMove) {
                            System.out.println("Item " + li.id + " marked for move to " + moveTo);
                        }

                        // Get latest version
                        DriveItemVersion latestVersion = graphClient.sites(siteId)
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
                        // they always should be added
                        // if (!hasUUID || mustMove) {
                        // Convert SDK object to JSON string
                        try {
                            String json = mapper.writeValueAsString(li);
                            SPItem = mapper.readValue(json, SharePointItemResponse.class);
                            Object title = adm.get("Title");
                            Object filename = adm.get("LinkFilename");
                            Object description = adm.get("containIODescription");
                            String label = adm.get("Label").toString();
                            String marking = adm.get("Marking").toString();
                            String titleStr = title != null ? title.toString().replace("\"", "") : "";
                            String filenameStr = filename != null ? filename.toString().replace("\"", "") : "";
                            String descriptionStr = description != null ? description.toString().replace("\"", "") : "";
                            SPItem.title = titleStr;
                            SPItem.filename = filenameStr;
                            SPItem.description = descriptionStr;
                            SPItem.mimetype = mimeType;
                            SPItem.UUID = uuidValue;
                            // String description = (String) fields.get("Description");
                            // To do get file content
                            SPItem.HasUUID = hasUUID;
                            SPItem.version = latestVersion.id;
                            SPItem.marking = marking;
                            SPItem.classification = "";
                            SPItem.MustMove = mustMove;
                            SPItem.MoveTo = moveTo;
                            SPResponseItems.add(SPItem);
                        } catch (Exception e) {
                            System.err.println("Failed to fetch item ID: " + li + " -> " + e.getMessage());
                        }
                        // }
                    }
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
        return SPResponseItems;
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

    
    private void LogNewDeltaLinkToFile() {
        try {

            // Split delta link
            String[] dlParts = newDeltaLink.split("\\?");
            String SourceID = "/sites" + (dlParts[0].split("sites")[1]);
            String TokenID = dlParts[1].replace("token=", "");
            // Check if exists, if so overwrite, else add
            Optional<IOLogDeltaLink> existingLog = IODeltaLinkLog.GetLog(SourceID);

            if (existingLog.isPresent()) {
                // Overwrite token
                IOLogDeltaLink log = existingLog.get();
                log.setTokenId(TokenID);
                log.setLastDeltaLink(newDeltaLink);
                IODeltaLinkLog.log(SourceID, TokenID, newDeltaLink); // save updated record
            } else {
                // Add new record
                IODeltaLinkLog.log(SourceID, TokenID, newDeltaLink); // save new record
            }

            Path path = Paths.get(this.DeltaLinkFile);

            // Read all lines
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

            // Filter out lines containing the old delta link base
            List<String> filteredLines = lines.stream()
                    .filter(line -> !line.contains(dlParts[0]))
                    .collect(Collectors.toList());

            // Overwrite file with remaining lines
            Files.write(
                    path,
                    filteredLines,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            // Append corrected content
            String correctedContent = dlParts[0] + "|" + newDeltaLink;
            Files.write(
                    path,
                    Collections.singletonList(correctedContent),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND);

            // Console output with color (ANSI)
            // System.out.println("\u001B[36mFile " + this.DeltaLinkFile + " updated with
            // latest deltalink");
            System.out.println("DB updated with latest deltalink");
            System.out.println("______________________________________________________________________\u001B[0m");

            // return ResponseEntity.ok().build(); // Spring
            // return Response.ok().build(); // JAX-RS
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

        private void ensureDeltaLinkFileExists() throws IOException {
        Path path = Paths.get(this.DeltaLinkFile);

        // 1. Ensure parent directory exists (/app/data)
        Files.createDirectories(path.getParent());

        // 2. Create file if it does not exist
        if (Files.notExists(path)) {
            Files.createFile(path);
        }
    }
        */
}