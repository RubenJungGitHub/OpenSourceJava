package contain.opensource.ils.bs.receiver.classes.alfresco;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Hashtable;
import java.util.Optional;
import java.net.URL;
import java.net.URI;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;

import jakarta.jms.DeliveryMode;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.Connection;
import jakarta.jms.TextMessage;
import jakarta.jms.Queue;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import contain.opensource.ils.bs.receiver.classes.Classification.AutoCLassify;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import contain.opensource.ils.bs.receiver.classes.Logger.IOLog;
import org.springframework.jms.core.JmsTemplate;
import org.apache.activemq.ActiveMQConnectionFactory;

import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import contain.opensource.ils.bs.receiver.classes.Binding.BindRequest;
import contain.opensource.shared.constants.AlfrescoConstants;
import contain.opensource.shared.constants.AlfrescoConstants.NodeTypeFields;
import contain.opensource.shared.constants.AlfrescoConstants.eActionPerformed;
import contain.opensource.ils.bs.receiver.services.TaxonomyServiceClient;
import contain.opensource.ils.bs.receiver.classes.Classification.ClassificationResult;
import contain.opensource.shared.classes.MigrationQueueMessage;
import contain.opensource.shared.configurationproperties.ActiveMQProperties;
import contain.opensource.shared.configurationproperties.ActiveMQProperties.BrokerConfig;

@Component
public class AlfrescoNodeController {
  public String nodeId;
  private String username;
  private String password;
  private String endpoint;

  public AlfrescoNodeResponse alfresconNodeResponse;
  private ILSRestProperties ilsproperties;
  private AlfrescoProperties alfrescoProperties;
  private ActiveMQProperties activeMQprops;
  private TaxonomyServiceClient taxonomyServiceClient;
  private AutoCLassify autoClassify;
  private CloseableHttpClient httpClient;
  private RestTemplate restTemplate;

  // (alongside your other fields like ilsproperties, taxonomyServiceClient, etc.)

  @Autowired
  private JmsTemplate jmsTemplate;

  @Autowired
  public AlfrescoNodeController(
      RestTemplate restTemplate,
      AlfrescoProperties alfrescoProperties,
      ILSRestProperties ilsproperties,
      ActiveMQProperties activeMQProps,
      TaxonomyServiceClient taxonomyServiceClient,
      CloseableHttpClient httpClient,
      AutoCLassify autoClassify) {

    this.restTemplate = restTemplate;
    this.alfrescoProperties = alfrescoProperties;
    this.ilsproperties = ilsproperties;
    this.activeMQprops = activeMQProps;
    this.taxonomyServiceClient = taxonomyServiceClient;
    this.httpClient = httpClient;
    this.autoClassify = autoClassify;

    // Initialize the endpoint and credentials from your configuration properties
    this.endpoint = alfrescoProperties.getBaseUrl();
    this.username = alfrescoProperties.getUsername();
    this.password = alfrescoProperties.getPassword();
  }

  public AlfrescoNodeController() {
    this.httpClient = null;
  }

  public AlfrescoNodeController(String nodeId) {
    this.nodeId = nodeId;
    this.httpClient = null;
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
  private String GetAlfrescoSiteNode(CloseableHttpClient client, RelocateInformationObject IOobject) {

    CloseableHttpResponse response = null;

    try {
      String endpoint = String.format(
          "%s/alfresco/api/-default-/public/alfresco/versions/1/sites",
          this.endpoint);
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
        JsonNode entriesNode = root.path("list").path("entries");

        for (JsonNode entry : entriesNode) {
          JsonNode siteNode = entry.path("entry");
          if (siteNode.path("id").asText().equals(IOobject.getcontainerto().split("[/\\\\]")[0])) {
            String guid = siteNode.path("guid").asText();
            IOobject.setsiteid(guid);
            return guid;
          }
        }
        System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_RED
            + "site GUID not found for " + IOobject.getcontainerto().split("[/\\\\]")[0]
            + contain.opensource.shared.constants.AlfrescoConstants.RESET);
        return "site GUID not found";
      }
    } catch (Exception e) {
      throw new RuntimeException("Error resolving Alfresco site nodeId", e);
    }
    return "FAILED";
  }

  /**
   * Retrieves the document library node ID for a specified Alfresco site.
   */
  private String getDocumentLibraryNodeId(CloseableHttpClient client, RelocateInformationObject IOobject)
      throws Exception {
    try {
      String endpoint = String.format("%s/alfresco/api/-default-/public/alfresco/versions/1/sites/%s/containers",
          this.endpoint, IOobject.getcontainerto().split("[/\\\\]")[0]);

      String auth = Base64.getEncoder().encodeToString((this.username + ":" +
          this.password).getBytes());

      HttpGet request = new HttpGet(endpoint);
      request.setHeader("Authorization", "Basic " + auth);
      request.setHeader("Accept", "application/json");

      try (CloseableHttpResponse response = client.execute(request)) {
        int statusCode = response.getCode();

        if (statusCode >= 200 && statusCode < 300) {
          ObjectMapper mapper = new ObjectMapper();
          String responseBody = EntityUtils.toString(response.getEntity());
          JsonNode root = mapper.readTree(responseBody);
          for (JsonNode container : root.path("list").path("entries")) {
            if (IOobject.getcontainerto().split("[/\\\\]")[1]
                .equalsIgnoreCase(container.path("entry").path("folderId").asText())) {
              IOobject.setlibid(container.path("entry").path("id").asText());
              return container.path("entry").path("id").asText();
            }
          }
        }
      }
    } catch (Exception e) {
      System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_RED
          + "Exception retrieving documentlibrary from Alfresco " + e
          + contain.opensource.shared.constants.AlfrescoConstants.RESET);
      e.printStackTrace();
    }
    return null;
  }

  private String getDocumentLibraryChild(CloseableHttpClient client, RelocateInformationObject IOobject,
      String doclibNodeId)
      throws Exception {
    try {
      String endpoint = String.format("%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s/children",
          this.endpoint, doclibNodeId);

      String auth = Base64.getEncoder().encodeToString((this.username + ":" +
          this.password).getBytes());

      HttpGet request = new HttpGet(endpoint);
      request.setHeader("Authorization", "Basic " + auth);
      request.setHeader("Accept", "application/json");

      try (CloseableHttpResponse response = client.execute(request)) {
        int statusCode = response.getCode();

        if (statusCode >= 200 && statusCode < 300) {
          ObjectMapper mapper = new ObjectMapper();
          String responseBody = EntityUtils.toString(response.getEntity());
          JsonNode root = mapper.readTree(responseBody);
          for (JsonNode container : root.path("list").path("entries")) {
            JsonNode entry = container.path("entry");

            if (entry.path("isFolder").asBoolean()) {
              String targetFolderName = IOobject.getcontainerto().split("[/\\\\]")[2];
              if (targetFolderName.equalsIgnoreCase(entry.path("name").asText())) {
                return entry.path("id").asText();
              }
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
   */
  private String GetNewNodeID(CloseableHttpClient client, String libNode, String fileId) throws Exception {
    String auth = Base64.getEncoder()
        .encodeToString((this.username + ":" + this.password).getBytes());

    // Kick off the recursive search across subfolders
    String nodeId = findNodeRecursive(client, libNode, fileId, auth);

    if (nodeId == null) {
      throw new RuntimeException("Uploaded file not found in library: " + fileId);
    }

    return nodeId;
  }

  private String findNodeRecursive(CloseableHttpClient client, String currentNodeId, String fileName, String auth) {
    String childrenEndpoint = String.format(
        "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s/children",
        this.endpoint, currentNodeId);

    HttpGet get = new HttpGet(childrenEndpoint);
    get.setHeader("Authorization", "Basic " + auth);
    get.setHeader("Accept", "application/json");

    try (CloseableHttpResponse response = client.execute(get)) {
      if (response.getCode() != 200) {
        return null;
      }

      String respBody = EntityUtils.toString(response.getEntity());
      ObjectMapper mapper = new ObjectMapper();
      JsonNode root = mapper.readTree(respBody);

      for (JsonNode entry : root.path("list").path("entries")) {
        JsonNode node = entry.path("entry");
        String nodeName = node.path("name").asText();
        String nodeId = node.path("id").asText();
        boolean isFolder = node.path("isFolder").asBoolean(false);

        // If it's a file and matches the target filename, return its ID immediately
        if (!isFolder && fileName.equals(nodeName)) {
          return nodeId;
        }

        // If it's a folder (e.g., Unclassified, Secret, etc.), search inside it
        // recursively
        if (isFolder) {
          String foundId = findNodeRecursive(client, nodeId, fileName, auth);
          if (foundId != null) {
            return foundId;
          }
        }
      }
    } catch (Exception e) {
      System.err.println(
          ">>> [MIGRATION] Error during recursive node lookup in node " + currentNodeId + ": " + e.getMessage());
    }
    return null; // Not found in this branch
  }

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
   * Waits for the "contain:UUID" property to be assigned to a node in Alfresco.
   */
  public String waitForUUID(String nodeId, int maxRetries, int sleepMs) throws Exception {
    String auth = Base64.getEncoder()
        .encodeToString((this.username + ":" + this.password).getBytes());

    String endpoint = String.format(
        "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s?include=properties,aspectNames",
        this.endpoint, nodeId);

    ObjectMapper mapper = new ObjectMapper();
    for (int attempt = 0; attempt < maxRetries; attempt++) {
      HttpGet get = new HttpGet(endpoint);
      get.setHeader("Authorization", "Basic " + auth);
      get.setHeader("Accept", "application/json");

      try (CloseableHttpResponse response = this.httpClient.execute(get)) {
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

      System.out.println(
          "UUID not yet detected   : sleeping " + sleepMs + "ms before retry " + (attempt + 1) + " of " + maxRetries);

      Thread.sleep(sleepMs);
    }

    throw new RuntimeException("UUID not assigned after waiting for " + (maxRetries * sleepMs) + "ms");
  }

  /**
   * Updates the metadata of an Alfresco node with specified properties.
   */
  public void updateMetaData(String nodeId, RelocateInformationObject IOobject) throws Exception {
    try {
      String updateEndpoint = String.format(
          "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s",
          this.endpoint, nodeId);
      ObjectMapper mapper = new ObjectMapper();
      String auth = Base64.getEncoder()
          .encodeToString((this.username + ":" + this.password).getBytes());

      HttpPut put = new HttpPut(updateEndpoint);
      put.setHeader("Authorization", "Basic " + auth);
      put.setHeader("Content-Type", "application/json");

      ObjectNode propertiesNode = mapper.createObjectNode();

      if (IOobject.getTitle() != null)
        propertiesNode.put("cm:title", IOobject.getTitle());
      if (IOobject.getDescription() != null)
        propertiesNode.put("cm:description", IOobject.getDescription());
      if (IOobject.getUuid() != null) {
        String rawUuid = IOobject.getUuid();
        rawUuid = rawUuid.replaceAll("^\"|\"$", "");
        propertiesNode.put("contain:IOUUID", rawUuid);
      }
      if (IOobject.getMarking() != null) {
        String rawMarking = IOobject.getMarking();
        rawMarking = rawMarking.replaceAll("^\"|\"$", "");
        propertiesNode.put("contain:EXTTAXMARKINGVAL", rawMarking);
      }
      if (IOobject.getclassification() != null) {
        String rawlabel = IOobject.getclassification();
        rawlabel = rawlabel.replaceAll("^\"|\"$", "");
        propertiesNode.put("contain:EXTTAXCLASSIFICATIONVAL", rawlabel);
      }
      if (IOobject.getMarkingID() != null) {
        String rawMarking = IOobject.getMarkingID();
        rawMarking = rawMarking.replaceAll("^\"|\"$", "");
        propertiesNode.put("contain:EXTTAXMARKINGID", rawMarking);
      }
      if (IOobject.getclassificationID() != null) {
        String rawlabel = IOobject.getclassificationID();
        rawlabel = rawlabel.replaceAll("^\"|\"$", "");
        propertiesNode.put("contain:EXTTAXCLASSIFICATIONID", rawlabel);
      }

      ObjectNode rootNode = mapper.createObjectNode();
      rootNode.set("properties", propertiesNode);

      put.setEntity(new StringEntity(rootNode.toString(), ContentType.APPLICATION_JSON));

      try (CloseableHttpResponse response = this.httpClient.execute(put)) {
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
              IOobject.getplatformfrom().toString(),
              IOobject.getplatformto().toString(),
              "BOUND ON DESTINATION PLATFORM",
              IOobject.getFileName(),
              "",
              AlfrescoConstants.eActionPerformed.COPIEDUUID,
              "System",
              IOobject.markingID,
              this.taxonomyServiceClient.GetTaxLabel(IOobject.markingID.toString()),
              IOobject.classificationID,
              this.taxonomyServiceClient.GetTaxLabel(IOobject.classificationID.toString()),
              IOobject.getSuggestedBy(),
              IOobject.getSuggestedClassification(),
              IOobject.getSuggestedClassificationConfidence(),
              IOobject.getVersion());
        }
      }
    } catch (Exception e) {
      System.err.println("Exception updating item to alfresco : " + e);
      e.printStackTrace();
    }
  }

  /**
   * Uploads a SharePoint item to Alfresco by creating a new node.
   */
  public String uploadSPItemToAlfresco(RelocateInformationObject IOobject) throws Exception {
    try {
      String siteNode = GetAlfrescoSiteNode(this.httpClient, IOobject);
      System.out.println("Sitenode  " + siteNode);
      String libNode = getDocumentLibraryNodeId(this.httpClient, IOobject);
      System.out.println("Libnode " + libNode);

      if (IOobject.getcontainerto().split("[/\\\\]").length > 2) {
        libNode = getDocumentLibraryChild(this.httpClient, IOobject, libNode);
        System.out.println("Foldernode " + libNode);
      }

      String auth = Base64.getEncoder()
          .encodeToString((this.username + ":" + this.password).getBytes());

      String endpoint = String.format(
          "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s/children",
          this.endpoint, libNode);
      HttpPost post = new HttpPost(endpoint);
      post.setHeader("Authorization", "Basic " + auth);
      post.setHeader("Accept", "application/json");

      ObjectMapper mapper = new ObjectMapper();
      ObjectNode props = mapper.createObjectNode();
      props.put("type", "contain:containdocument");
      props.put("name", IOobject.getFileName());

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

      try (CloseableHttpResponse response = this.httpClient.execute(post)) {
        int statusCode = response.getCode();
        String responseBody = EntityUtils.toString(response.getEntity());
        if (statusCode == 409) {
        }
        if (statusCode >= 200 && statusCode < 300) {
          String nodeId = null;
          try {
            Thread.sleep(2000);
            nodeId = GetNewNodeID(this.httpClient, libNode, IOobject.getFileName());
          } catch (Exception e) {
            System.err.println("Exception uploading item to alfresco : " + e);
            e.printStackTrace();
          }
          updateMetaData(nodeId, IOobject);
          System.out.println("Item  : " + IOobject.getFileName() + " moved to Alfresco succesfully");
          String action = "Copy UUID " + IOobject.getUuid() + " : " + IOobject.getFileName() + " from "
              + IOobject.getplatformfrom() + ":" + IOobject.getcontainerfrom() + " -> " + IOobject.getplatformto()
              + " : " + IOobject.getcontainerto();
          IOLog.log(
              IOobject.getUuid(),
              IOobject.getId(),
              "",
              action,
              IOobject.getplatformfrom().toString(),
              IOobject.getplatformto().toString(),
              "BOUND ON DESTINATION PLATFORM",
              IOobject.getFileName(),
              "",
              AlfrescoConstants.eActionPerformed.IOCOPIED,
              "System",
              IOobject.markingID,
              this.taxonomyServiceClient.GetTaxLabel(IOobject.markingID.toString()),
              IOobject.classificationID,
              this.taxonomyServiceClient.GetTaxLabel(IOobject.classificationID.toString()),
              IOobject.getSuggestedBy(),
              IOobject.getSuggestedClassification(),
              IOobject.getSuggestedClassificationConfidence(),
              IOobject.version);
          return "";
        } else {
          throw new RuntimeException("Upload failed: " + statusCode + " - " + responseBody);
        }
      }
    } catch (Exception e) {
      System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_RED
          + "Exception uploading item to alfresco : " + e
          + contain.opensource.shared.constants.AlfrescoConstants.RESET);
      e.printStackTrace();
      throw e;
    }
  }

  public void SendAlfrescoNodeMessageToQueue(String nodeid, String secondpath) {
    System.out.println(">>> Direct queue push requested for node: " + nodeid);
    // ActiveMQProperties activeMQProps = new ActiveMQProperties();
    // Should be loaded from bean but fails... hardcoded for now.
    BrokerConfig config = activeMQprops.getAlfrescoSource();
    config.setUser("admin");
    config.setPassword("admin");
    config.setQueue("Consumer.MyJavaConsumer.VirtualTopic.alfresco.repo.events.nodes");
    // Use the 'migconnection' and 'migsession' you created locally
    ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(config.getUser(), config.getPassword(),
        config.getBrokerUrl());
    try (Connection conn = factory.createConnection()) {
      conn.start();
      try (Session session = conn.createSession(true, Session.SESSION_TRANSACTED)) {
        ObjectMapper objectMapper = new ObjectMapper();
        Queue migqueue = session.createQueue(config.getQueue());
        MessageProducer producer = session.createProducer(migqueue);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        long currentTimestamp = System.currentTimeMillis();
        String eventUuid = java.util.UUID.randomUUID().toString();
        String extractedFilename = "";
        if (secondpath == null || secondpath.isEmpty()) {
          extractedFilename = "unknown";
        }
        int lastSlash = secondpath.lastIndexOf('/');
        extractedFilename = lastSlash != -1 ? secondpath.substring(lastSlash + 1) : secondpath;
        // MigrationQueueMessage payload = new MigrationQueueMessage(weburl, "Migrate",
        // deltalink, platformfrom,
        // id, migrateinfo.get("platformto"), migrateinfo.get("containerto"));
        String payload = "{"
            + "\"@class\":\"org.alfresco.sync.events.types.NodeContentPutEvent\","
            + "\"id\":\"" + eventUuid + "\","
            + "\"type\":\"CONTENTPUT\","
            + "\"username\":\"admin\","
            + "\"timestamp\":" + currentTimestamp + ","
            + "\"seqNumber\":0,"
            + "\"nodeId\":\"" + nodeid + "\","
            + "\"siteId\":\"ontobind\","
            + "\"nodeType\":\"contain:containdocument\","
            + "\"name\":\"" + extractedFilename + "\","
            + "\"paths\":[\"java.util.ArrayList\",[\"" + secondpath + "\"]],"
            + "\"nodeProperties\":{\"@class\":\"java.util.HashMap\"}"
            + "}";

        // Ensure this variable name matches what's in the Grandparent
        // String json = objectMapper.writeValueAsString(payload);

        // Use 'migsession' here, NOT 'session'
        TextMessage message = session.createTextMessage(payload);

        producer.send(message);

        producer.close();
        session.commit();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String timestamp = ZonedDateTime.now(ZoneId.of("Europe/Amsterdam")).toLocalDateTime().format(formatter);
        String feedback = timestamp + " -> Information object queued for migration";
        System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_BRIGHT_CYAN
            + feedback
            + contain.opensource.shared.constants.AlfrescoConstants.RESET);
      }
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to send migration message to ActiveMQ", ex);
    }
  }

  public Hashtable<String, String> processalfresconodepoint(String nodeid, String secondpath)
      throws MalformedURLException, Exception {
    Hashtable<String, String> migrationinfo = new Hashtable<>();
    try {

      this.nodeId = nodeid;
      System.out.println("processalfresconodepoint nodeID " + nodeid + " secondpath " + secondpath);

      // 1. Meet de starttijd vóór de stap
      long startTime = System.currentTimeMillis();

      AlfrescoNodeResponse alfrescoresponse = GetNode(nodeid);
      // For now Force Autoclassification value for alfresco.

      // 2. Meet de eindtijd en bereken het verschil
      long duration = System.currentTimeMillis() - startTime;

      // 3. Log de verstreken tijd (bijv. via System.out of je logger)
      System.out.println(
          contain.opensource.shared.constants.AlfrescoConstants.BG_BRIGHT_CYAN
              + "Performance: GetNode for ID " + nodeid + " and autoclassifiy took " + duration + " ms"
              + contain.opensource.shared.constants.AlfrescoConstants.RESET);
      try {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonOutput = objectMapper.writeValueAsString(alfrescoresponse);
        System.out.println("AlfrescoNodeResponse JSON: " + jsonOutput);
      } catch (Exception e) {
        System.err
            .println("Failed to serialize response, possibly hiddenrendition too pdf thumbnail: " + e.getMessage());
      }
      itemmustmigrate(alfrescoresponse);
      migrationinfo.put("platformto",
          (alfrescoresponse.getplatformto() != null) ? alfrescoresponse.getplatformto().name() : "<NO MOVE>");
      migrationinfo.put("containerto",
          alfrescoresponse.getcontainerto() != null ? alfrescoresponse.getcontainerto() : "");
      alfrescoresponse.setpath(secondpath);
      if (alfrescoresponse.getmustmove()) {
        System.out.println("Item " + alfrescoresponse.getcontainerfrom() + " marked for move to "
            + alfrescoresponse.getplatformto() + " ->" + alfrescoresponse.getcontainerto());
      }
      boolean SuggestedClassificationUpdateResult;
      boolean bindIO = true;
      // =
      // UpdateNode(AlfrescoConstants.NodeTypeFields.SuggestedClassification,Optional.ofNullable(secondpath),
      // Optional.empty());

      if (!alfrescoresponse.HasUUID) {
        boolean uuiddupdateResult = UpdateNode(AlfrescoConstants.NodeTypeFields.UUID, Optional.ofNullable(secondpath),
            Optional.empty());
            bindIO = true;

      } else {
        if (!alfrescoresponse.HasSuggestedClassification) {
          SuggestedClassificationUpdateResult = UpdateNode(AlfrescoConstants.NodeTypeFields.SuggestedClassification,
              Optional.ofNullable(secondpath), Optional.empty());
          bindIO = false;
        }
      }
      if (bindIO || this.alfresconNodeResponse.getForceSuggestedClassification()) {
        BindObject(alfrescoresponse);
      }
    } catch (Exception ex) {
      System.err.println("Failed to process changed SP item: " + ex);
      ex.printStackTrace();
      throw ex;
    }
    return migrationinfo;
  }

  private void itemmustmigrate(AlfrescoNodeResponse aitem) {
    String endpoint = ilsproperties.getruleenginemoveendpoint();
    String cleanPlatformFrom = aitem.platformfrom.name().replace("\"", "");
    String cleanClassification = aitem.classificationID.replace("\"", "").replace("-", "");
    String cleanMarking = aitem.markingID.replace("\"", "").replace("-", "");
    String cleancontainerfrom = aitem.containerfrom.replace("\"", "");

    URI targetUri = UriComponentsBuilder.fromHttpUrl(ilsproperties.getruleenginemoveendpoint())
        .queryParam("platformfrom", cleanPlatformFrom)
        .queryParam("containerfrom", cleancontainerfrom)
        .queryParam("classification", cleanClassification)
        .queryParam("marking", cleanMarking)
        .build()
        .encode()
        .toUri();

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(targetUri)
        .GET()
        .build();

    String responseBody = null;
    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      int statusCode = response.statusCode();
      responseBody = response.body();
      System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_MAGENTA
          + "Item must migrate Request : " + request
          + contain.opensource.shared.constants.AlfrescoConstants.RESET);
      if (statusCode == 200) {
        System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_CYAN
            + "Item must migrate Response : " + responseBody
            + contain.opensource.shared.constants.AlfrescoConstants.RESET);

        ObjectMapper mapper = new ObjectMapper();
        RelocateInformationObject result = mapper.readValue(responseBody, RelocateInformationObject.class);
        aitem.setplatformto(result.getplatformto());
        aitem.setcontainerto(result.getcontainerto());

        System.out.println("Destination platform: " + aitem.getplatformto());
        System.out.println("Destination container  " + aitem.getcontainerto());
      } else {
        System.err.println("Fout van Rule Engine of geen resultaat: " + statusCode + " - " + responseBody);
      }
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
    aitem.setmustmove();
  }

  private void BindObject(AlfrescoNodeResponse Alfrescoitem) {
    try {
      System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_MAGENTA
          + contain.opensource.shared.constants.AlfrescoConstants.RED
          + ("Binding SP IO " + Alfrescoitem.UUID)
          + contain.opensource.shared.constants.AlfrescoConstants.RESET);

      BindRequest request = new BindRequest(
          Alfrescoitem.ToSecuredDocument());

      String endPoint = ilsproperties.getbindendpoint();
      System.out.println(contain.opensource.shared.constants.AlfrescoConstants.RED
          + "Binding endpoint  : "
          + endPoint
          + contain.opensource.shared.constants.AlfrescoConstants.RESET);

      // Use the injected restTemplate (or ensure it's available in your controller
      // class scope)
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<BindRequest> entity = new HttpEntity<>(request, headers);
      ResponseEntity<String> bindresponse = restTemplate.postForEntity(endPoint, entity, String.class);

      System.out.println(
          "Accessing uuid rest url on " + endPoint + " return code -> " + bindresponse.getStatusCode().value());

      String action = "IO MODIFIED. BIND IO " + Alfrescoitem.UUID + "  to ALFRESCO IO " + Alfrescoitem.entry.filename;
      if (bindresponse.getStatusCode().value() == 200) {
        IOLog.log(
            Alfrescoitem.UUID,
            Alfrescoitem.id,
            Alfrescoitem.getpath(),
            action,
            AlfrescoConstants.ContainPlatforms.ALFRESCO.toString(),
            AlfrescoConstants.ContainPlatforms.ALFRESCO.toString(),
            bindresponse.getBody(),
            Alfrescoitem.entry.filename,
            "",
            AlfrescoConstants.eActionPerformed.IOBOUND,
            "System",
            Alfrescoitem.markingID,
            Alfrescoitem.marking = this.taxonomyServiceClient.GetTaxLabel(Alfrescoitem.markingID),
            Alfrescoitem.classificationID,
            Alfrescoitem.classification = this.taxonomyServiceClient.GetTaxLabel(Alfrescoitem.classificationID),
            Alfrescoitem.getSuggestedBy(),
            Alfrescoitem.getSuggestedClassification(),
            Alfrescoitem.getSuggestedClassificationConfidence(),
            Alfrescoitem.version);
      }
    } catch (Exception ex) {
      System.err.println("Failed to bind object: " + ex);
      ex.printStackTrace();
    }
  }

  public AlfrescoNodeResponse GetNode(String nodeid) {
    this.nodeId = nodeid;

    String cleanEndpoint = this.endpoint != null ? this.endpoint.replaceAll("/+$", "") : "";

    String endpoint = String.format(
        "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s?include=path",
        cleanEndpoint, this.nodeId);
    System.out.println("Alfresco endpoint applied : " + endpoint);

    String auth = Base64.getEncoder().encodeToString(
        (this.username + ":" + this.password).getBytes(StandardCharsets.UTF_8));

    HttpGet request = new HttpGet(endpoint);
    request.setHeader("Authorization", "Basic " + auth);

    // try-with-resources zorgt ervoor dat de response ALTIJD netjes wordt gesloten
    // en vrijgegeven
    try (CloseableHttpResponse response = (CloseableHttpResponse) this.httpClient.execute(request)) {
      int status = response.getCode();
      if (status == 200) {
        String json = EntityUtils.toString(response.getEntity());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(json);
        JsonNode entry = rootNode.path("entry");

        // 🛑 GUARD CLAUSE: Skip background renditions and thumbnails to prevent
        // duplicate events
        // 🛑 GUARD CLAUSE: Skip background renditions and thumbnails to prevent
        // duplicate events
        boolean isHiddenRendition = false;
        JsonNode aspectNames = entry.path("aspectNames");
        if (aspectNames.isArray()) {
          for (JsonNode aspect : aspectNames) {
            String aspectStr = aspect.asText();
            // Alleen negeren bij daadwerkelijke, ongewenste achtergrond-renditions of
            // thumbnails
            if (aspectStr.equals("app:thumbnail") || aspectStr.equals("rn:hiddenRendition")) {
              isHiddenRendition = true;
              break;
            }
          }
        }
        if (isHiddenRendition) {
          // Exit early so background preview generation isn't processed as an IO event
          return null;
        }

        try {
          alfresconNodeResponse = mapper.readValue(json, AlfrescoNodeResponse.class);
          alfresconNodeResponse.setplatformfrom(AlfrescoConstants.ContainPlatforms.ALFRESCO);

          alfresconNodeResponse.filename = rootNode.path("entry")
              .path("name")
              .asText();

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
              .path("contain:EXTTAXMARKINGVAL")
              .asText();

          alfresconNodeResponse.markingID = rootNode.path("entry")
              .path("properties")
              .path("contain:EXTTAXMARKINGID")
              .asText();

          alfresconNodeResponse.classification = rootNode.path("entry")
              .path("properties")
              .path("contain:EXTTAXCLASSIFICATIONVAL")
              .asText();

          alfresconNodeResponse.classificationID = rootNode.path("entry")
              .path("properties")
              .path("contain:EXTTAXCLASSIFICATIONID")
              .asText();

          alfresconNodeResponse.SuggestedBy = rootNode.path("entry")
              .path("properties")
              .path("contain:AUTOCLASSIFIER")
              .asText();
          alfresconNodeResponse.Suggestedclassification = rootNode.path("entry")
              .path("properties")
              .path("contain:SUGGESTEDCLASSIFICATIONVAL")
              .asText();

          alfresconNodeResponse.SuggestedclassificationConfidence = rootNode.path("entry")
              .path("properties")
              .path("contain:SUGGESTEDCLASSIFICATIONCONFIDENCE").asDouble();

          alfresconNodeResponse.classificationID = rootNode.path("entry")
              .path("properties")
              .path("contain:EXTTAXCLASSIFICATIONID")
              .asText();

          alfresconNodeResponse.setcontainerfrom(rootNode.path("entry")
              .path("path")
              .path("name")
              .asText());

          alfresconNodeResponse.mimeType = rootNode.path("entry")
              .path("content")
              .path("mimeType")
              .asText();

          // Object ioUUIDValue =
          // alfresconNodeResponse.entry.properties.otherProperties.get("contain:IOUUID");
          JsonNode ioUuidNode = rootNode.path("entry").path("properties").path("contain:IOUUID");

          if (!ioUuidNode.isMissingNode() && !ioUuidNode.isNull() && !ioUuidNode.asText().isBlank()) {
            alfresconNodeResponse.UUID = ioUuidNode.asText();
          } else {
            alfresconNodeResponse.UUID = ""; // Fallback to an empty string instead of null
          }

          // Get content and classify
          try {
            alfresconNodeResponse.HasUUID = (alfresconNodeResponse.UUID != null && !alfresconNodeResponse.UUID.isEmpty());
            ClassificationResult classificationResult = GetNodeContent();
          } catch (java.lang.NullPointerException e) {
          } catch (Exception e) {
            System.err.println("Exception UUID : " + e);
            e.printStackTrace();
          }
        } catch (Exception e) {
          System.err.println("Exception parsing node json : " + e);
          e.printStackTrace();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode entryNode = rootNode.path("entry");

        // 1. FileName (uit entry -> name)
        String fileName = entryNode.path("name").asText();

        // 2. FileType (uit entry -> content -> mimeTypeName)
        String fileType = entryNode.path("content").path("mimeTypeName").asText();

        // 3. Classification (uit entry -> properties ->
        // contain:EXTTAXCLASSIFICATIONVAL)
        String classification = entryNode.path("properties").path("contain:EXTTAXCLASSIFICATIONVAL").asText();

        // 4. Marking (uit entry -> properties -> contain:EXTTAXMARKINGVAL)
        String marking = entryNode.path("properties").path("contain:EXTTAXMARKINGVAL").asText();

        // Netjes weergeven op de console
        System.out.println("========================================");
        System.out.println(" FileName                       : " + fileName);
        System.out.println(" FileType                       : " + fileType);
        System.out.println(" Classification                 : " + classification);
        System.out.println(" Marking                        : " + marking);
        System.out.println(" Suggestedclassification        : " + this.alfresconNodeResponse.Suggestedclassification);
        System.out.println(" SuggestedBy                    : " + this.alfresconNodeResponse.SuggestedBy);
        System.out
            .println(
                " Force suggested classification : " + this.alfresconNodeResponse.getForceSuggestedClassification());
        System.out.println("========================================");
        System.out.println("Node metadata:");
        System.out.println(json);
      } else {
        System.err.println("Alfresco returned unexpected status: " + status + " for node ID: " + this.nodeId);
      }
    } catch (Exception e) {
      System.err.println("Exception getting node : " + e);
      e.printStackTrace();
    }
    return alfresconNodeResponse;
  }

  private CloseableHttpResponse ProcessGetRequest(String endpoint) {
    CloseableHttpResponse response = null;
    String auth = Base64.getEncoder().encodeToString(
        (this.username + ":" + this.password).getBytes(StandardCharsets.UTF_8));

    try {
      HttpGet request = new HttpGet(endpoint);
      request.setHeader("Authorization", "Basic " + auth);
      response = this.httpClient.execute(request);
    } catch (Exception e) {
      System.err.println("Exception getting node : " + e);
      e.printStackTrace();
    }
    return response;
  }

  private ClassificationResult GetNodeContent() throws IOException {
    // Strip any trailing slashes from the base endpoint to avoid double slashes
    String cleanEndpoint = this.endpoint != null ? this.endpoint.replaceAll("/+$", "") : "";

    String endpoint = String.format(
        "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s/content",
        cleanEndpoint, this.nodeId);

    String auth = Base64.getEncoder()
        .encodeToString(
            (this.username + ":" + this.password).getBytes(StandardCharsets.UTF_8));

    HttpGet request = new HttpGet(endpoint);
    request.setHeader("Authorization", "Basic " + auth);

    ClassificationResult classificationResult = null;

    try (CloseableHttpResponse response = this.httpClient.execute(request);
        InputStream in = response.getEntity().getContent()) {

      if (response.getCode() != 200) {
        throw new IOException("HTTP error " + response.getCode());
      }

      this.alfresconNodeResponse.content = in.readAllBytes();

      // 1. Haal het ClassificationResult object op
      classificationResult = autoClassify.ClassifyIO(this.alfresconNodeResponse.content);

      // Force suggestedclassification
      this.alfresconNodeResponse.ForceSuggestedClassification = false;
      this.alfresconNodeResponse.SuggestedclassificationGuid = classificationResult.correspondingGuid();

      // 2. Zet de properties netjes door naar je alfresconNodeResponse
      if (classificationResult != null) {
        this.alfresconNodeResponse.HasSuggestedClassification = (this.alfresconNodeResponse.Suggestedclassification != null
            && !this.alfresconNodeResponse.Suggestedclassification.isEmpty());
      }

      if (this.alfresconNodeResponse.HasSuggestedClassification) {
        this.alfresconNodeResponse.HasSuggestedClassification = this.alfresconNodeResponse.Suggestedclassification
            .equals(classificationResult.label());
      }
      if (!this.alfresconNodeResponse.HasSuggestedClassification) {
        System.out.println("Suggested classification has changed from "
            + alfresconNodeResponse.Suggestedclassification + " to " + classificationResult.label());
        this.alfresconNodeResponse.SuggestedBy = classificationResult.classifier();
        this.alfresconNodeResponse.Suggestedclassification = classificationResult.label();
        this.alfresconNodeResponse.setSuggestedClassificationConfidence(classificationResult.confidence());
        this.alfresconNodeResponse.setSuggestedClassificationGUID(classificationResult.correspondingGuid());

        System.out.println("Suggested classification Result: " + classificationResult.label() + " with confidence: "
            + classificationResult.confidence());
      }
      // Check if we need to force the suggestedclassification
      if (this.alfresconNodeResponse.getForceSuggestedClassification() && !this.alfresconNodeResponse.classificationID
          .equals(this.alfresconNodeResponse.SuggestedclassificationGuid)) {
        // Now if we want the engine to force classification then always override
        this.alfresconNodeResponse.setForceSuggestedClassification();
        this.alfresconNodeResponse.HasSuggestedClassification = false;
      }

    } catch (

    Exception e) {
      System.err.println("Exception getting node content : " + e);
      e.printStackTrace();
      throw new IOException("Failed to get node content", e);
    }

    return classificationResult;
  }

  private boolean UpdateNode(NodeTypeFields field, Optional<String> IOPath,
      Optional<String> fieldValue) {
    try {
      // Clean up trailing slashes to prevent double-slash URI errors
      String cleanEndpoint = this.endpoint != null ? this.endpoint.replaceAll("/+$", "") : "";

      String endpoint = String.format(
          "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s",
          cleanEndpoint, this.nodeId);

      String Path = IOPath.orElse("Unknown");
      String propertyValue = fieldValue.orElse("Dummy");

      String action = "";
      String jsonPropertiesBlock = "";
      String classVal = this.alfresconNodeResponse.Suggestedclassification;
      Double confVal = this.alfresconNodeResponse.getSuggestedClassificationConfidence();
      String classifier = this.alfresconNodeResponse.SuggestedBy;
      eActionPerformed actionperformed;
      String pkihash = "";
      switch (field) {
        case AlfrescoConstants.NodeTypeFields.UUID:
          pkihash = "NewOnPlatform";
          actionperformed = eActionPerformed.ASSIGNUUID;
          System.out.println(
              AlfrescoConstants.RED + "Get UUID endpoint  : "
                  + ilsproperties.getuudiutilendpoint() + AlfrescoConstants.RESET);

          String query = "?prefix=" + AlfrescoConstants.ContainPlatforms.ALFRESCO;
          String urlString = ilsproperties.getuudiutilendpoint() + query;

          URL url = new URL(urlString);
          HttpURLConnection conn = (HttpURLConnection) url.openConnection();
          conn.setRequestMethod("GET");

          int status = conn.getResponseCode();
          System.out.println(
              "Accessing uuid rest url on " + ilsproperties.getuudiutilendpoint() + " return code -> " + status);

          if (status == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            propertyValue = in.readLine();
            in.close();
          } else {
            System.err.println("REST call failed with status: " + status);
          }

          action = "Assign UUID " + propertyValue + " and suggested classification values to new Alfresco IO "
              + this.alfresconNodeResponse.entry.filename;

          // Haal de suggested values op
          // classVal = this.alfresconNodeResponse.Suggestedclassification;
          // confVal = this.alfresconNodeResponse.getSuggestedClassificationConfidence();
          // classifier = this.alfresconNodeResponse.SuggestedBy;

          // Zet UUID én de suggested values tegelijk in de properties block
          jsonPropertiesBlock = String.format(
              "\"contain:IOUUID\": \"%s\",\n              \"contain:SUGGESTEDCLASSIFICATIONVAL\": \"%s\",\n              \"contain:SUGGESTEDCLASSIFICATIONCONFIDENCE\": \"%s\",\n              \"contain:AUTOCLASSIFIER\": \"%s\"",
              propertyValue, classVal, confVal, classifier);

          this.alfresconNodeResponse.UUID = propertyValue;
          break;

        case AlfrescoConstants.NodeTypeFields.Title:
          actionperformed = eActionPerformed.TITLECHANGE;
          action = "To do title update for ballenbak registration";
          jsonPropertiesBlock = String.format("\"cm:title\": \"%s\"", propertyValue);

          // Debug
          if (jsonPropertiesBlock.contains("Dummy")) {
            System.out.println("Warning: The properties block contains a Dummy value!");
          }

          break;

        case AlfrescoConstants.NodeTypeFields.SuggestedClassification:
          pkihash = "Identical to previous";
          // classVal = this.alfresconNodeResponse.Suggestedclassification;
          actionperformed = eActionPerformed.IOSUGGESTEDCLASSIFICATIONUPDATED;
          if (this.alfresconNodeResponse.ForceSuggestedClassification) {
            actionperformed = eActionPerformed.SUGGESTEDCLASSIFICATIONFORCED;
          }
          // Gebruik hier direct de getter van je object:
          // confVal = this.alfresconNodeResponse.getSuggestedClassificationConfidence();
          propertyValue = alfresconNodeResponse.UUID;
          action = "Assign Suggested Classification " + classVal + " and Confidence " + confVal + " to Alfresco IO "
              + this.alfresconNodeResponse.entry.filename;

          jsonPropertiesBlock = String.format(
              "\"contain:IOUUID\": \"%s\",\n              \"contain:SUGGESTEDCLASSIFICATIONVAL\": \"%s\",\n              \"contain:SUGGESTEDCLASSIFICATIONCONFIDENCE\": \"%s\",\n              \"contain:AUTOCLASSIFIER\": \"%s\"",
              propertyValue, classVal, confVal, classifier);
          break;

        default:
          throw new IllegalArgumentException("Unsupported field: " + field);
      }

      // Altijd beginnen met een lege string, NOOIT null!
      String forcedClassificationProperties = "";

      if (this.alfresconNodeResponse.getForceSuggestedClassification()) {
        String classId = this.alfresconNodeResponse.classificationID; // Of je suggested classification ID veld

        if (classId != null && !classId.isEmpty() && this.taxonomyServiceClient != null) {
          String classLabel = this.taxonomyServiceClient.GetTaxLabel(classId);

          // De komma zit hier netjes vooraan in de string
          forcedClassificationProperties = String.format(
              ",\n            \"contain:EXTTAXCLASSIFICATIONID\": \"%s\",\n            \"contain:EXTTAXCLASSIFICATIONVAL\": \"%s\"",
              classId, classLabel);
        }
      }

      // Samenvoegen in de JSON block
      jsonPropertiesBlock = String.format(
          "\"contain:IOUUID\": \"%s\",\n            \"contain:SUGGESTEDCLASSIFICATIONVAL\": \"%s\",\n            \"contain:SUGGESTEDCLASSIFICATIONCONFIDENCE\": \"%s\",\n            \"contain:AUTOCLASSIFIER\": \"%s\"%s",
          propertyValue, classVal, confVal, classifier, forcedClassificationProperties);

      // ... (binnen je UpdateNode methode, net voor de try-with-resources van de
      // httpClient):

      String jsonBody = """
          {
            "properties": {
              %s
            }
          }
          """.formatted(jsonPropertiesBlock);

      System.out.println(contain.opensource.shared.constants.AlfrescoConstants.MAGENTA + "Updating "
          + this.alfresconNodeResponse.entry.filename + " " + jsonBody
          + contain.opensource.shared.constants.AlfrescoConstants.RESET);

      String auth = Base64.getEncoder().encodeToString(
          (this.username + ":" + this.password).getBytes(StandardCharsets.UTF_8));

      HttpPut request = new HttpPut(endpoint);
      request.setHeader("Authorization", "Basic " + auth);
      request.setHeader("Content-Type", "application/json");
      request.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

      // STEL HIER DE TIMEOUT OP OP (bijv. 120 seconden i.p.v. de standaard 10 sec)
      RequestConfig requestConfig = RequestConfig.custom()
          .setConnectTimeout(Timeout.ofSeconds(30))
          .setResponseTimeout(Timeout.ofSeconds(120)) // Voorkomt dat de client te vroeg opgeeft
          .build();

      request.setConfig(requestConfig);

      try (CloseableHttpResponse response = this.httpClient.execute(request)) {
        int statusCode = response.getCode();
        String responseJson = EntityUtils.toString(response.getEntity());

        if (statusCode == 200) {
          this.alfresconNodeResponse.UUID = propertyValue;
          IOLog.log(
              propertyValue,
              this.alfresconNodeResponse.entry.id,
              Path,
              action,
              AlfrescoConstants.ContainPlatforms.ALFRESCO.toString(),
              AlfrescoConstants.ContainPlatforms.ALFRESCO.toString(),
              pkihash,
              this.alfresconNodeResponse.entry.filename,
              jsonBody,
              actionperformed,
              alfresconNodeResponse.entry.modifiedByUser.displayName,
              this.alfresconNodeResponse.markingID,
              this.alfresconNodeResponse.marking = this.taxonomyServiceClient
                  .GetTaxLabel(this.alfresconNodeResponse.markingID),
              this.alfresconNodeResponse.classificationID,
              this.alfresconNodeResponse.classification = this.taxonomyServiceClient
                  .GetTaxLabel(this.alfresconNodeResponse.classificationID),
              this.alfresconNodeResponse.getSuggestedBy(),
              this.alfresconNodeResponse.getSuggestedClassification(),
              this.alfresconNodeResponse.getSuggestedClassificationConfidence(),
              this.alfresconNodeResponse.version);

          System.out.println("Node updated successfully:");
          System.out.println(responseJson);
          return true;
        } else {
          System.err.println("Failed to update node. Status code: " + statusCode);
          System.err.println(responseJson);
          return false;
        }
      }
    } catch (Exception e) {
      System.err.println("Exception updating field:");
      e.printStackTrace();
      return false;
    }
  }

  public void DeleteAlfrescoNode(String nodeId) throws Exception {
    this.nodeId = nodeId;
    String endpoint = String.format(
        "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s",
        this.endpoint, this.nodeId);
    String auth = Base64.getEncoder().encodeToString(
        (this.username + ":" + this.password).getBytes(StandardCharsets.UTF_8));

    HttpDelete request = new HttpDelete(endpoint);
    request.setHeader("Authorization", "Basic " + auth);

    try (CloseableHttpResponse response = this.httpClient.execute(request)) {
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