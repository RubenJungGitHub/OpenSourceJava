package contain.opensource.ils.bs.receiver.classes.alfresco;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.Objects;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import contain.opensource.ils.bs.receiver.classes.ConfigurationProperties.AlfrescoProperties;
import contain.opensource.ils.bs.receiver.classes.ConfigurationProperties.ILSRestProperties;
import contain.opensource.ils.bs.receiver.classes.Logger.IOLog;
import contain.opensource.ils.bs.receiver.classes.Redis.RedisManager;
import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import contain.opensource.ils.bs.receiver.classes.UUIDUtil;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.ContainPlatforms;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.NodeTypeFields;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.eActionPerformed;
import contain.opensource.ils.bs.receiver.services.GraphService;

/**
 * AlfrescoNodeController is a Spring component that manages interactions with
 * the Alfresco Content Management System.
 * It provides functionality to upload, retrieve, update, and delete documents
 * in Alfresco, as well as handle metadata
 * management and document relocation between platforms.
 *
 * h2>Key Responsibilities:</h2>
 *
 * Authentication with Alfresco REST API using Basic Auth
 * Upload SharePoint items to Alfresco with metadata
 * Retrieve node information, content, and properties from Alfresco
 * Update node metadata including custom properties (UUID, MARKING,
 * classification)
 * Delete nodes from Alfresco
 * Manage cross-platform document relocation (Alfresco to SharePoint and
 * vice versa)
 * Poll for UUID assignment and handle asynchronous node creation
 * 
 *
 * h2>Configuration:</h2>
 * This component is autowired with {@link AlfrescoProperties} and
 * {@link ILSRestProperties} to obtain
 * Alfresco endpoint URL, username, and password credentials.
 *
 * h2>Important Notes:</h2>
 *
 * Document upload and metadata update cannot be performed in a single
 * operation in Alfresco;
 * they require sequential API calls
 * The controller implements polling mechanisms to wait for asynchronous
 * operations (e.g., UUID assignment)
 * All HTTP communication uses CloseableHttpClient with proper resource
 * management
 * Alfresco site name and other constants are obtained from
 * {@link AlfrescoConstants}
 * 
 *
 * h2>Error Handling:</h2>
 * Most methods use try-catch blocks with logging to System.err and
 * printStackTrace() for debugging.
 * Consider implementing proper logging framework (SLF4J, Log4j) for production
 * use.
 *
 * @author [Author Name]
 * @version 1.0
 * @see AlfrescoProperties
 * @see ILSRestProperties
 * @see AlfrescoConstants
 * @see RelocateInformationObject
 * @see AlfrescoNodeResponse
 */
@Component
public class AlfrescoNodeController {
  public String nodeId;
  private String username;
  private String password;
  private String endpoint;

  public AlfrescoNodeResponse alfresconNodeResponse = null;
  private ILSRestProperties ilsRestProperties = null;
  private AlfrescoProperties alfrescoProperties;

  @Autowired
  public AlfrescoNodeController(AlfrescoProperties alfrescoProperties, ILSRestProperties ilsProperties) {
    this.alfrescoProperties = alfrescoProperties;
    this.ilsRestProperties = ilsProperties;
    this.endpoint = alfrescoProperties.getBaseUrl();
    this.username = alfrescoProperties.getUsername();
    this.password = alfrescoProperties.getPassword();
  }

  public AlfrescoNodeController() {
  }

  public AlfrescoNodeController(String nodeId) {
    this.nodeId = nodeId;
  }

  /**
   * Retrieves the GUID (Global Unique Identifier) of an Alfresco site node.
   * 
   * This method makes an HTTP GET request to the Alfresco REST API to fetch
   * information about a specific site and extracts its GUID from the response.
   * 
   * param client the CloseableHttpClient instance used to execute the HTTP
   * request
   * 
   * @return the GUID of the Alfresco site node if found and status code is 200,
   *         otherwise returns "FAILED"
   * @throws RuntimeException if an error occurs during the HTTP request or JSON
   *                          parsing,
   *                          with the cause wrapped in the exception
   * 
   * @see CloseableHttpClient
   * @see HttpGet
   * @see ObjectMapper
   */
  private String GetAlfrescoSiteNode(CloseableHttpClient client) {

    CloseableHttpResponse response = null;

    try {
      String endpoint = String.format(
          "%s/alfresco/api/-default-/public/alfresco/versions/1/sites/%s",
          this.endpoint,
          AlfrescoConstants.alfrescoDemoSiteName);
      String auth = Base64.getEncoder().encodeToString(
          (this.username + ":" + this.password).getBytes(StandardCharsets.UTF_8));

      HttpGet request = new HttpGet(endpoint);
      request.setHeader("Authorization", "Basic " + auth);
      response = client.execute(request);
      int status = response.getCode();
      if (status == 200) {
        String json = EntityUtils.toString(response.getEntity());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        JsonNode entryNode = root.get("entry");

        if (entryNode != null && entryNode.get("guid") != null) {
          String guid = entryNode.get("guid").asText();
          return guid;
        } else {
          System.out.println("GUID not found");
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Error resolving Alfresco site nodeId", e);
    }
    return "FAILED";
  }

  /**
   * Retrieves the document library node ID for a specified Alfresco site.
   * 
   * This method queries the Alfresco REST API to fetch all containers associated
   * with
   * a site and returns the ID of the container with folder ID "documentLibrary".
   * 
   * param client the CloseableHttpClient used to execute HTTP requests
   * param siteNodeId the Alfresco site node identifier (currently unused; the
   * method
   * uses AlfrescoConstants.alfrescoDemoSiteName instead)
   * 
   * @return the ID of the document library container if found, null otherwise
   * @throws Exception if an error occurs during the HTTP request or JSON
   *                   processing
   * 
   * @apiNote The method makes a GET request to:
   *          {endpoint}/alfresco/api/-default-/public/alfresco/versions/1/sites/{siteName}/containers
   *          using Basic authentication with configured username and password.
   * 
   * @implNote Exceptions are logged to System.err but not rethrown. The method
   *           returns
   *           null if the container is not found or if an exception occurs.
   */
  private String getDocumentLibraryNodeId(CloseableHttpClient client, String siteNodeId) throws Exception {
    try {
      // ChatGPT
      // Thanks — I see your full method. From what you’ve written, a 404 is almost
      // certainly because siteNodeId is not the correct Alfresco site ID (short
      // name). In Alfresco REST API:
      // sites/{siteId}/containers expects the site short name, e.g., "ontobind"
      String endpoint = String.format(
          "%s/alfresco/api/-default-/public/alfresco/versions/1/sites/%s/containers",
          this.endpoint, AlfrescoConstants.alfrescoDemoSiteName);

      String auth = Base64.getEncoder().encodeToString((this.username + ":" +
          this.password).getBytes());

      HttpGet request = new HttpGet(endpoint);
      request.setHeader("Authorization", "Basic " + auth);
      request.setHeader("Accept", "application/json");

      // request.setHeader("Authorization", "Basic " + auth);
      // Send request
      try (CloseableHttpResponse response = client.execute(request)) {
        int statusCode = response.getCode();

        if (statusCode >= 200 && statusCode < 300) {
          ObjectMapper mapper = new ObjectMapper();
          String responseBody = EntityUtils.toString(response.getEntity()); // ✅ get
          JsonNode root = mapper.readTree(responseBody);
          for (JsonNode container : root.path("list").path("entries")) {
            if ("documentLibrary".equals(container.path("entry").path("folderId").asText())) {
              return container.path("entry").path("id").asText();
            }
          }
        }
      }
    } catch (Exception e) {
      System.err.println("Exception uploading item to alfresco : " + e);
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Retrieves the node ID of a child node within an Alfresco library by matching
   * the file name.
   *
   * param client the {@link CloseableHttpClient} used to execute HTTP requests
   * to the Alfresco server
   * param libNode the parent node ID in Alfresco representing the library folder
   * param fileId the name of the file to search for within the library's
   * children
   * 
   * @return the node ID of the matching file if found
   * @throws Exception        if an HTTP error occurs or if the node is not found
   *                          in the library
   * @throws RuntimeException if the uploaded file cannot be found in the
   *                          specified library node
   *
   * @deprecated The {@link CloseableHttpClient#execute(ClassicHttpRequest)}
   *             method is deprecated.
   *             Consider updating to use the latest Apache HttpClient API.
   */
  private String GetNewNodeID(CloseableHttpClient client, String libNode, String fileId) throws Exception {
    String nodeId = null;
    String auth = Base64.getEncoder()
        .encodeToString((this.username + ":" + this.password).getBytes());
    String childrenEndpoint = String.format(

        "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s/children",
        this.endpoint, libNode);
    HttpGet get = new HttpGet(childrenEndpoint);
    get.setHeader("Authorization", "Basic " + auth);
    get.setHeader("Accept", "application/json");

    try (CloseableHttpResponse response = client.execute(get)) {
      String respBody = EntityUtils.toString(response.getEntity());
      ObjectMapper mapper = new ObjectMapper();
      JsonNode root = mapper.readTree(respBody);
      for (JsonNode entry : root.path("list").path("entries")) {
        JsonNode node = entry.path("entry");
        if (fileId.equals(node.path("name").asText())) {
          nodeId = node.path("id").asText();
          break;
        }
      }
    }
    if (nodeId == null) {
      throw new RuntimeException("Uploaded file not found in library");
    }
    try {
      // For POC purposes
      // getNodeFields(nodeId, client, auth);

    } catch (Exception e) {
      System.err.println("Exception uploading item to alfresco : " + e);
      e.printStackTrace();
    }
    return nodeId;
  }

  /**
   * Retrieves and prints the title and description properties of a node from the
   * Alfresco repository.
   *
   * param nodeId the ID of the Alfresco node to retrieve
   * param client the {@link CloseableHttpClient} used to execute the HTTP
   * request
   * param auth the Base64-encoded authentication string for the Alfresco API
   * 
   * @throws Exception if an error occurs during the HTTP request or JSON
   *                   processing
   */
  private void getNodeFields(String nodeId, CloseableHttpClient client, String auth) throws Exception {
    String nodeEndpoint = String.format("%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s",
        this.endpoint, nodeId);
    HttpGet get = new HttpGet(nodeEndpoint);
    get.setHeader("Authorization", "Basic " + auth);
    get.setHeader("Accept", "application/json");
    try (CloseableHttpResponse response = client.execute(get)) {
      String json = EntityUtils.toString(response.getEntity());
      ObjectMapper mapper = new ObjectMapper();
      JsonNode root = mapper.readTree(json);
      JsonNode properties = root.path("entry").path("properties");
      String title = properties.path("cm:title").asText();
      String description = properties.path("cm:description").asText();
      System.out.println("Title: " + title + ", Description: " + description);
    }
  }

  /**
   * Waits for the "contain:UUID" property to be assigned to a node in Alfresco,
   * retrying the request
   * up to a specified number of times with a delay between attempts.
   *
   * 
   * This method sends repeated HTTP GET requests to the Alfresco REST API for the
   * specified node,
   * checking if the "contain:UUID" property is present and non-empty. If the UUID
   * is found, it is returned.
   * If the UUID is not assigned after all retries, a {@link RuntimeException} is
   * thrown.
   *
   * param nodeId the Alfresco node ID to check for the UUID property
   * param maxRetries the maximum number of retry attempts before giving up
   * param sleepMs the number of milliseconds to wait between retries
   * 
   * @return the value of the "contain:UUID" property if found
   * @throws Exception if an error occurs during HTTP communication or JSON
   *                   parsing,
   *                   or if the UUID is not assigned after all retries
   */
  public String waitForUUID(String nodeId, int maxRetries, int sleepMs) throws Exception {
    String auth = Base64.getEncoder()
        .encodeToString((this.username + ":" + this.password).getBytes());

    String endpoint = String.format(
        "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s?include=properties,aspectNames",
        this.endpoint, nodeId);

    try (CloseableHttpClient client = HttpClients.createDefault()) {
      ObjectMapper mapper = new ObjectMapper();
      for (int attempt = 0; attempt < maxRetries; attempt++) {
        HttpGet get = new HttpGet(endpoint);
        get.setHeader("Authorization", "Basic " + auth);
        get.setHeader("Accept", "application/json");

        try (CloseableHttpResponse response = client.execute(get)) {
          int statusCode = response.getCode();
          String responseBody = EntityUtils.toString(response.getEntity());

          if (statusCode >= 200 && statusCode < 300) {
            JsonNode root = mapper.readTree(responseBody);
            JsonNode props = root.path("entry").path("properties");
            JsonNode uuidNode = props.path("contain:UUID");
            if (!uuidNode.isMissingNode() && !uuidNode.asText().isEmpty()) {
              return uuidNode.asText();
            }
          } else {
            throw new RuntimeException("Failed to get node: " + statusCode + " - " + responseBody);
          }
        }

        // wait before next retry
        System.out.println(
            "UUID not yet detected   : sleeping " + sleepMs + "ms before retry " + (attempt + 1) + " of " + maxRetries);

        Thread.sleep(sleepMs);
      }
    }

    throw new RuntimeException("UUID not assigned after waiting for " + (maxRetries * sleepMs) + "ms");
  }

  /**
   * Updates the metadata of an Alfresco node with the specified properties from
   * the given {@link RelocateInformationObject}.
   * 
   * This method constructs a JSON payload containing the relevant metadata fields
   * (such as title, description, UUID, marking, and classification)
   * and sends an HTTP PUT request to the Alfresco REST API to update the node's
   * properties.
   * 
   * If the update is successful, an action log is created using
   * {@link IOLog#log}. If the update fails, a {@link RuntimeException}
   * is thrown with details of the failure.
   *
   * param nodeId the ID of the Alfresco node to update
   * param IOobject the {@link RelocateInformationObject} containing the metadata
   * to update
   */
  public void updateMetaData(String nodeId, RelocateInformationObject IOobject)  throws Exception{
    try {
      // ===========================================================================================
      // Now it is working in separate methid but in my perception it should be
      // possible in one run updating the node.
      // ===========================================================================================

      String updateEndpoint = String.format(
          "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s",
          this.endpoint, nodeId);
      ObjectMapper mapper = new ObjectMapper();
      String auth = Base64.getEncoder()
          .encodeToString((this.username + ":" + this.password).getBytes());
      String.format(
          "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s",
          this.endpoint, nodeId);
      HttpPut put = new HttpPut(updateEndpoint);
      put.setHeader("Authorization", "Basic " + auth);
      put.setHeader("Content-Type", "application/json");
      CloseableHttpClient client = HttpClients.createDefault();

      // Wrap all model properties inside a "properties" object
      ObjectNode propertiesNode = mapper.createObjectNode();
      if (IOobject.getTitle() != null)
        propertiesNode.put("cm:title", IOobject.getTitle());
      if (IOobject.getDescription() != null)
        propertiesNode.put("cm:description", IOobject.getDescription());
      if (IOobject.getUuid() != null) {
        String rawUuid = IOobject.getUuid();
        // Remove leading/trailing quotes if they exist
        rawUuid = rawUuid.replaceAll("^\"|\"$", "");
        propertiesNode.put("contain:IOUUID", rawUuid);
      }
      if (IOobject.getMarking() != null) {
        String rawMarking = IOobject.getMarking();
        // Remove leading/trailing quotes if they exist
        rawMarking = rawMarking.replaceAll("^\"|\"$", "");
        propertiesNode.put("contain:MARKING", rawMarking);
      }
      if (IOobject.getclassification() != null) {
        String rawlabel = IOobject.getclassification();
        // Remove leading/trailing quotes if they exist
        rawlabel = rawlabel.replaceAll("^\"|\"$", "");
        propertiesNode.put("contain:CLASSIFICATION", rawlabel);
      }
      /*
       * chatgpt
       * Alfresco does not allow you to set an arbitrary version number. The version
       * is always managed internally (1.0, 1.1, etc.), and the only thing you can
       * control via REST is whether the next update is a major or minor version.
       * 
       * So instead of:
       * 
       * propertiesNode.put("cm:versionLabel", "1.5"); // ❌ won't work
       * 
       * 
       * You need to send a versioning hint when updating the content:
       */

      ObjectNode rootNode = mapper.createObjectNode();
      rootNode.set("properties", propertiesNode); // <--- key fix

      put.setEntity(new StringEntity(rootNode.toString(), ContentType.APPLICATION_JSON));
      // if (IOobject.getUuid() != null && !IOobject.getUuid().isEmpty())
      // updateProps.put("contain:UUID", IOobject.getUuid());

      // put.setEntity(new StringEntity(updateProps.toString(),
      // ContentType.APPLICATION_JSON));

      try (CloseableHttpResponse response = client.execute(put)) {
        int statusCode = response.getCode();
        if (statusCode < 200 || statusCode >= 300) {
          String resp = EntityUtils.toString(response.getEntity());
          throw new RuntimeException("Updating metadata failed: " + statusCode + " - " + resp);
        } else {
          String action = "Overwrite new IO UUID ingenerated in Alfresco for " + IOobject.getFileName() + " with UUID "
              + IOobject.getUuid() + "  generated in SPO ";

          IOLog.log(
              IOobject.getUuid(),
              "",
              "",
              action,
              // AlfrescoConstants.ContainPlatforms.SPO.toString(),
              // AlfrescoConstants.ContainPlatforms.SPO.toString(),
              IOobject.getPlatfrom().toString(),
              IOobject.getPlatformTo().toString(),
              "BOUND ON DESTINATION PLATFORM",
              IOobject.getFileName(),
              "",
              AlfrescoConstants.eActionPerformed.COPIEDUUID,
              "System",
              IOobject.marking,
              IOobject.classification,
              IOobject.version);
        }
      }
    } catch (Exception e) {
      System.err.println("Exception updating item to alfresco : " + e);
      e.printStackTrace();
    }
  }

  /**
   * Uploads a SharePoint item to Alfresco by creating a new node in the specified
   * document library.
   * 
   * This method performs the following steps:
   *
   * Retrieves the Alfresco site node and document library node IDs.
   * Builds a multipart HTTP POST request to upload the file and its
   * properties to Alfresco.
   * Handles the Alfresco response, retrieves the new node ID, and updates its
   * metadata.
   * 
   * 
   * If the upload is successful, the method returns an empty string. If any error
   * occurs, it logs the exception
   * and returns "Failed".
   *
   * param IOobject The {@link RelocateInformationObject} containing the file
   * data, name, and MIME type to upload.
   * 
   * @return An empty string if the upload is successful; otherwise, returns
   *         "Failed".
   */
  public String uploadSPItemToAlfresco(RelocateInformationObject IOobject) throws Exception{
    try {
      // First get SiteNode
      try (CloseableHttpClient client = HttpClients.createDefault()) {
        String siteNode = GetAlfrescoSiteNode(client);
        System.out.println("Sitenode  " + siteNode);
        String libNode = getDocumentLibraryNodeId(client, siteNode);
        // System.out.println("Libnode " + libNode);

        String auth = Base64.getEncoder()
            .encodeToString((this.username + ":" + this.password).getBytes());

        // Debug because Alfrescco only returns the fields if filled in. This however
        // this should not affect Put, which it seems to do
        // String nodeid = GetNewNodeID(client, libNode, "password.txt");
        // getNodeFields("ecb97ec6-7a68-490a-802b-52cfc5339941", client, auth);
        String endpoint = String.format(
            "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s/children",
            this.endpoint, libNode);
        HttpPost post = new HttpPost(endpoint);
        post.setHeader("Authorization", "Basic " + auth);
        post.setHeader("Accept", "application/json");

        // ==========================================================================================================
        // Actual upload format
        // Build properties JSON
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode props = mapper.createObjectNode();
        props.put("type", "contain:containdocument"); // mandatory
        props.put("name", IOobject.getFileName()); // mandatory

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.STRICT);
        builder.addTextBody(
            "properties",
            mapper.writeValueAsString(props),
            ContentType.create("application/json", StandardCharsets.UTF_8));
        builder.addBinaryBody(
            "filedata",
            IOobject.getContent(),
            ContentType.create(IOobject.getMimeType()),
            IOobject.getFileName());

        post.setEntity(builder.build());

        // ==========================================================================================================

        try (CloseableHttpResponse response = client.execute(post)) {
          int statusCode = response.getCode();
          String responseBody = EntityUtils.toString(response.getEntity());
          //Check if existant!
          if(statusCode == 409)
          {
            //Debug
            //Sometimes events cross. Avoid break of process
            //Integer a = 1; 
          }
          if (statusCode >= 200 && statusCode < 300) {
            // debug GetNode to check why fields are not set
            // get the new nodeID. THIS IS THE ONLY ABSURD WAY!!!
            // According to ChatGPT it is not possible to update all in one go but needs to
            // be separated.
            String nodeId = null;
            try {

              Thread.sleep(2000); // Give Lafresco time to create nodeid
              nodeId = GetNewNodeID(client, libNode, IOobject.getFileName());
            } catch (Exception e) {
              System.err.println("Exception uploading item to alfresco : " + e);
              e.printStackTrace();
            }
            // To do check null for transaction and persistance
            // Upload content + set cm:title/cm:description in one go” is NOT supported
            // reliably Checked relentlessly with ChatGPT 17-12-2025
            updateMetaData(nodeId, IOobject);
            System.out.println("Item  : " + IOobject.getFileName() + " moved to Alfresco succesfully");
            //BS
            return "";
          } else {
            throw new RuntimeException("Upload failed: " + statusCode + " - " + responseBody);
          }
        }
      }
    } catch (
    Exception e) {
      System.err.println("Exception uploading item to alfresco : " + e);
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * Retrieves metadata and content for a specific Alfresco node using the
   * configured endpoint, nodeId, and credentials.
   * 
   * This method performs the following steps:
   *
   * Builds the Alfresco REST API endpoint URL for the node.
   * Authenticates using HTTP Basic Auth with the provided username and
   * password.
   * Sends an HTTP GET request to fetch the node's metadata as JSON.
   * Parses the JSON response into an {@code AlfrescoNodeResponse}
   * object.
   * Extracts and sets properties such as title, description, version,
   * marking, classification, UUID, and MoveTo platform.
   * Checks for the presence of UUID and MoveTo information, handling missing
   * or malformed data gracefully.
   * Fetches the node's content by calling {@code GetNodeContent()}.
   * Handles and logs exceptions that may occur during the process.
   * 
   * 
   * Prints node metadata to the standard output and logs errors to the standard
   * error stream.
   * 
   * b>Note:</b> This method relies on several class fields (such as
   * {@code endpoint}, {@code nodeId}, {@code username}, {@code password}, and
   * {@code alfresconNodeResponse})
   * and expects the {@code AlfrescoNodeResponse} and {@code ContainPlatforms}
   * classes to be defined elsewhere in the codebase.
   */
  public void GetNode() {
    try {
      String endpoint = String.format(
          "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s",
          this.endpoint, this.nodeId);
      System.out.println("Alfresco endpoint applied : " + endpoint);

      String auth = Base64.getEncoder().encodeToString(
          (this.username + ":" + this.password).getBytes(StandardCharsets.UTF_8));

      try (CloseableHttpClient client = HttpClients.createDefault()) {

        HttpGet request = new HttpGet(endpoint);
        request.setHeader("Authorization", "Basic " + auth);

        var response = client.execute(request);
        int status = response.getCode();
        if (status == 200) {
          String json = EntityUtils.toString(response.getEntity());

          ObjectMapper mapper = new ObjectMapper();
          JsonNode rootNode = mapper.readTree(json);

          try {
            alfresconNodeResponse = mapper.readValue(json, AlfrescoNodeResponse.class);

            // Navigate to the title and description
            alfresconNodeResponse.Title = rootNode.path("entry")
                .path("properties")
                .path("cm:title")
                .asText();

            alfresconNodeResponse.Description = rootNode.path("entry")
                .path("properties")
                .path("cm:description")
                .asText();

            alfresconNodeResponse.version = rootNode.path("entry")
                .path("properties")
                .path("cm:versionLabel")
                .asText();

            alfresconNodeResponse.marking = rootNode.path("entry")
                .path("properties")
                .path("contain:MARKING")
                .asText();

            alfresconNodeResponse.classification = rootNode.path("entry")
                .path("properties")
                .path("contain:CLASSIFICATION")
                .asText();

            // Check if UUID present
            // Get node content
            Object ioUUIDValue = alfresconNodeResponse.entry.properties.otherProperties.get("contain:IOUUID");
            if (ioUUIDValue != null && !ioUUIDValue.toString().isBlank()) {
              alfresconNodeResponse.UUID = ioUUIDValue.toString();
            }
            Object value = alfresconNodeResponse.entry.properties.otherProperties.get("contain:IOMOVE");
            if (!Objects.equals(value, "<NO MOVE>")) {
              alfresconNodeResponse.MoveTo = ContainPlatforms.valueOf(value.toString().toUpperCase());
              ;
            }
            // Get node content
            GetNodeContent();
            try {
              alfresconNodeResponse.HasUUID = (alfresconNodeResponse.UUID != null);
            } catch (java.lang.NullPointerException e) {
              // No action. UUID not present
            } catch (Exception e) {
              System.err.println("Exception UUID : " + e);
              e.printStackTrace();
            }
            try {
              alfresconNodeResponse.MustMove = (!alfresconNodeResponse.MoveTo.equals("<NO MOVE>")); // IMPROVE!! SHOULD
                                                                                                    // ALSO CHECK IF NOT
                                                                                                    // FROM ALFRESCO TO
                                                                                                    // ALFRESCO!!!!
                                                                                                    // (Unless different
                                                                                                    // instance)
              //
            } catch (java.lang.NullPointerException e) {
              // No action. UUID not present
            } catch (Exception e) {
              System.err.println("Exceptionsetting moveto : " + e);
              e.printStackTrace();
            }
            // No action. UUID not present
          } catch (Exception e) {
            System.err.println("Exception getting node : " + e);
            e.printStackTrace();
          }
          System.out.println("Node metadata:");
          System.out.println(json);
        } else {
          throw new RuntimeException("Unexpected status: " + status);
        }
      }
    } catch (Exception e) {
      System.err.println("Exception getting node : " + e);
      e.printStackTrace();
    }
  }

  /**
   * Executes an HTTP GET request to the specified endpoint using basic
   * authentication.
   *
   * param endpoint the URL to send the GET request to
   * 
   * @return a {@link CloseableHttpResponse} containing the response from the
   *         server,
   *         or {@code null} if an exception occurs
   */
  private CloseableHttpResponse ProcessGetRequest(String endpoint) {
    CloseableHttpResponse response = null;
    String auth = Base64.getEncoder().encodeToString(
        (this.username + ":" + this.password).getBytes(StandardCharsets.UTF_8));

    try (CloseableHttpClient client = HttpClients.createDefault()) {

      HttpGet request = new HttpGet(endpoint);
      request.setHeader("Authorization", "Basic " + auth);

      response = client.execute(request);

    } catch (Exception e) {
      System.err.println("Exception getting node : " + e);
      e.printStackTrace();
    }
    return response;
  }

  /**
   * Retrieves the content of a node from the Alfresco repository using the REST
   * API.
   * 
   * This method constructs the appropriate endpoint URL, sets up HTTP Basic
   * Authentication,
   * sends a GET request to fetch the node's content, and stores the content bytes
   * in
   * {@code this.alfresconNodeResponse.content}.
   *
   *
   * @throws IOException if an I/O error occurs during the HTTP request or if the
   *                     response code is not 200.
   */
  private void GetNodeContent() throws IOException {
    String endpoint = String.format(
        "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s/content",
        this.endpoint, this.nodeId);
    String auth = Base64.getEncoder()
        .encodeToString(
            (this.username + ":" + this.password).getBytes(StandardCharsets.UTF_8));

    HttpGet request = new HttpGet(endpoint);
    request.setHeader("Authorization", "Basic " + auth);

    try (CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = client.execute(request);
        InputStream in = response.getEntity().getContent()) {

      if (response.getCode() != 200) {
        throw new IOException("HTTP error " + response.getCode());
      }
      this.alfresconNodeResponse.content = in.readAllBytes();
    }
  }

  /**
   * Updates a property of an Alfresco node based on the specified field and
   * value.
   * 
   * This method constructs a JSON payload to update a node's property in Alfresco
   * via its REST API.
   * The property to update is determined by the {@code field} parameter, and the
   * new value is provided
   * via {@code fieldValue}. Optionally, an IO path can be specified for logging
   * purposes.
   * 
   * The method handles authentication using Basic Auth and logs the update
   * action.
   * 
   * param field The {@link NodeTypeFields} enum specifying which property
   * to update (e.g., UUID, Title).
   * param IOPath An {@link Optional} containing the IO path for logging, or
   * empty if not applicable.
   * param fieldValue An {@link Optional} containing the new value for the
   * property, or empty for a default.
   * 
   * @return The updated property value if successful, or "Failed" if the update
   *         did not succeed.
   * 
   * @throws IllegalArgumentException if the specified field is not supported.
   */
  public String UpdateNode(NodeTypeFields field, Optional<String> IOPath, Optional<String> fieldValue) {
    try {
      String endpoint = String.format(
          "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s",
          this.endpoint, this.nodeId);
      // Map enum to Alfresco property
      String propertyName;
      String Path = IOPath.orElse("Unknown");
      String propertyValue = fieldValue.orElse("Dummy");
      String action = "";
      switch (field) {
        case UUID:
          propertyName = "contain:IOUUID"; // custom aspect property
          //propertyValue = AlfrescoConstants.ContainPlatforms.ALFRESCO.toString() + "-" + UUIDUtil.getUUIDOverHTTP();
          propertyValue =UUIDUtil.getUUIDOverHTTP(Optional.of(AlfrescoConstants.ContainPlatforms.ALFRESCO));
          action = "Assign UUID " + propertyValue + "  to new Alfresco IO " + this.alfresconNodeResponse.entry.filename;
          break;
        case Title:
          action = "To do title update for ballenbak registration";
          propertyName = "cm:title"; // standard property
          break;
        default:
          throw new IllegalArgumentException("Unsupported field: " + field);
      }
      // JSON body
      String jsonBody = """
          {
            "properties": {
              "%s": "%s"
            }
          }
          """.formatted(propertyName, propertyValue);

      System.out.println(contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.MAGENTA + "Updating "
          + this.alfresconNodeResponse.entry.filename + " " + jsonBody
          + contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RESET);

      // Encode username:password for Basic Auth
      String auth = Base64.getEncoder().encodeToString(
          (this.username + ":" + this.password).getBytes(StandardCharsets.UTF_8));

      try (CloseableHttpClient client = HttpClients.createDefault()) {
        HttpPut request = new HttpPut(endpoint);
        request.setHeader("Authorization", "Basic " + auth);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

        var response = client.execute(request);
        int statusCode = response.getCode();
        String responseJson = EntityUtils.toString(response.getEntity());

        if (statusCode == 200) {
          this.alfresconNodeResponse.UUID = propertyValue;
          RedisManager.putHash("IOinHashAssigned","IOinUUIDAssigned" + propertyValue, "InProcess",240);
          // Test ballenbak
          IOLog.log(
              propertyValue,
              this.alfresconNodeResponse.entry.id,
              Path,
              action,
              AlfrescoConstants.ContainPlatforms.ALFRESCO.toString(),
              AlfrescoConstants.ContainPlatforms.ALFRESCO.toString(),
              // UUIDUtil.getUUID(),
              "NewOnPlatform",
              this.alfresconNodeResponse.entry.filename,
              jsonBody,
              eActionPerformed.ASSIGNUUID,
              alfresconNodeResponse.entry.modifiedByUser.displayName,
              this.alfresconNodeResponse.marking,
              this.alfresconNodeResponse.classification,
              this.alfresconNodeResponse.version);

          System.out.println("Node updated successfully:");
          System.out.println(responseJson);
          return propertyValue;
        } else {
          System.err.println("Failed to update node. Status code: " + statusCode);
          System.err.println(responseJson);
          return "Failed";
        }
      }
    } catch (Exception e) {
      System.err.println("Exception updating field:");
      e.printStackTrace();
      return "Failed";
    }
  }

  /**
   * Relocates an Information Object (IO) from Alfresco to another platform.
   * 
   * This method logs the relocation action, uploads the node to SharePoint (or
   * another
   * specified platform) using a REST API, and then deletes the original Alfresco
   * node.
   * 
   * b>Note:</b> This method currently assumes only the happy path and does not
   * include
   * versioning, copy controls, or proper rollback/transaction management in case
   * of errors.
   * 
   * param IOobject The {@link RelocateInformationObject} containing details
   * about the node to relocate,
   * including its ID, UUID, file name, source and destination
   * platforms, hash, marking,
   * classification, and version.
   */
  public void RelocateIO(RelocateInformationObject IOobject) {
    /// ==================================================
    /// NOTE> NO VERSIONING AND COPY CONTROLS INCLUDED!!!
    /// Proper returnvalue needed..
    /// ==================================================

    // Could be done from here but because it is not sure from where relocation is
    // performed we are using a REST API
    //ADD ERRORHANDLING

    GraphService GService = new GraphService();
    this.nodeId = IOobject.getId();
    // For now assumed only happy path. If something goes wrong rollback in
    // ballenbak and complete transacton`
    String action = "Move UUID " + IOobject.getUuid() + " : " + IOobject.getFileName() + " from "
        + IOobject.getPlatfrom() + " to " + IOobject.getPlatformTo();
        
      GService.uploadAlfrescoNodeToSP(IOobject);
    IOLog.log(
        IOobject.getUuid(),
        "",
        "",
        action,
        IOobject.getPlatfrom().toString(),
        IOobject.getPlatformTo().toString(),
        IOobject.getHash(),
        IOobject.getFileName(),
        "",
        eActionPerformed.IOCOPIED,
        "System",
        IOobject.marking,
        IOobject.classification,
        IOobject.version);
    
    DeleteAlfrescoNode();
  }

  /**
   * Deletes a node from Alfresco using the REST API.
   * 
   * Constructs the endpoint URL using the configured endpoint and node ID,
   * then sends an HTTP DELETE request with basic authentication.
   * If the deletion is successful (HTTP 204), a success message is printed.
   * Otherwise, prints an error message and the response body if available.
   *
   *
   * @throws Exception if an error occurs during the HTTP request.
   */
  private void DeleteAlfrescoNode() {
    String endpoint = String.format(
        "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s",
        this.endpoint, this.nodeId);
    String auth = Base64.getEncoder().encodeToString(
        (this.username + ":" + this.password).getBytes(StandardCharsets.UTF_8));

    try (CloseableHttpClient client = HttpClients.createDefault()) {

      HttpDelete request = new HttpDelete(endpoint);
      request.setHeader("Authorization", "Basic " + auth);

      var response = client.execute(request);
      int status = response.getCode();
      if (status == 204) {
        System.out.println("Node deleted successfully: " + nodeId);
      } else {
        System.err.println("Failed to delete node. Status: " + status);
        if (response.getEntity() != null) {
          System.err.println(EntityUtils.toString(response.getEntity()));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}