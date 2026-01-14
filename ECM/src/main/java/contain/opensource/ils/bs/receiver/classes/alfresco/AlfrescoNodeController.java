package contain.opensource.ils.bs.receiver.classes.alfresco;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import contain.opensource.ils.bs.receiver.classes.Logger.IOLog;
import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import contain.opensource.ils.bs.receiver.classes.UUIDUtil;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.NodeTypeFields;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.eActionPerformed;
import contain.opensource.ils.bs.receiver.services.GraphService;

public class AlfrescoNodeController {
  String nodeId;
  public AlfrescoNodeResponse alfresconNodeResponse = null;

  public AlfrescoNodeController() {
  }

  public AlfrescoNodeController(String nodeId) {
    this.nodeId = nodeId;

  }

  private String GetAlfrescoSiteNode(CloseableHttpClient client) {

    CloseableHttpResponse response = null;

    try {
      String endpoint = String.format(
          "%s/alfresco/api/-default-/public/alfresco/versions/1/sites/%s",
          AlfrescoConstants.alfrescoBaseUrl,
          AlfrescoConstants.alfrescoDemoSiteName);
      String auth = Base64.getEncoder().encodeToString(
          (AlfrescoConstants.username + ":" + AlfrescoConstants.password).getBytes(StandardCharsets.UTF_8));

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

  private String getDocumentLibraryNodeId(CloseableHttpClient client, String siteNodeId) throws Exception {
    try {
      // ChatGPT
      // Thanks — I see your full method. From what you’ve written, a 404 is almost
      // certainly because siteNodeId is not the correct Alfresco site ID (short
      // name). In Alfresco REST API:
      // sites/{siteId}/containers expects the site short name, e.g., "ontobind"
      String endpoint = String.format(
          "%s/alfresco/api/-default-/public/alfresco/versions/1/sites/%s/containers",
          AlfrescoConstants.alfrescoBaseUrl, AlfrescoConstants.alfrescoDemoSiteName);

      String auth = Base64.getEncoder().encodeToString((AlfrescoConstants.username + ":" +
          AlfrescoConstants.password).getBytes());

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

  private String GetNewNodeID(CloseableHttpClient client, String libNode, String fileId) throws Exception {
    String nodeId = null;
    String auth = Base64.getEncoder()
        .encodeToString((AlfrescoConstants.username + ":" + AlfrescoConstants.password).getBytes());
    String childrenEndpoint = String.format(

        "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s/children",
        AlfrescoConstants.alfrescoBaseUrl, libNode);
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

  private void getNodeFields(String nodeId, CloseableHttpClient client, String auth) throws Exception {

    String nodeEndpoint = String.format("%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s",
        AlfrescoConstants.alfrescoBaseUrl, nodeId);
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

  public String waitForUUID(String nodeId, int maxRetries, int sleepMs) throws Exception {
    String auth = Base64.getEncoder()
        .encodeToString((AlfrescoConstants.username + ":" + AlfrescoConstants.password).getBytes());

    String endpoint = String.format(
        "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s?include=properties,aspectNames",
        AlfrescoConstants.alfrescoBaseUrl, nodeId);

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

  public void updateMetaData(String nodeId, RelocateInformationObject IOobject) {
    try {
      // ===========================================================================================
      // Now it is working in separate methid but in my perception it should be
      // possible in one run updating the node.
      // ===========================================================================================

      String updateEndpoint = String.format(
          "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s",
          AlfrescoConstants.alfrescoBaseUrl, nodeId);
      ObjectMapper mapper = new ObjectMapper();
      String auth = Base64.getEncoder()
          .encodeToString((AlfrescoConstants.username + ":" + AlfrescoConstants.password).getBytes());
      String.format(
          "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s",
          AlfrescoConstants.alfrescoBaseUrl, nodeId);
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
      // if (IOobject.getUuid() != null)
      // propertiesNode.put("contain:UUID", IOobject.getUuid());
      // UUID is an aspect, skip it here

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
              AlfrescoConstants.ContainPlatforms.SPO.toString(),
              AlfrescoConstants.ContainPlatforms.SPO.toString(),
              IOobject.getHash(),
              IOobject.getFileName(),
              "",
              AlfrescoConstants.eActionPerformed.COPIEDUUID,
              "System");
        }
      }
    } catch (Exception e) {
      System.err.println("Exception updating item to alfresco : " + e);
      e.printStackTrace();
    }
  }

  public String uploadSPItemToAlfresco(RelocateInformationObject IOobject) {
    try {
      // First get SiteNode
      try (CloseableHttpClient client = HttpClients.createDefault()) {
        String siteNode = GetAlfrescoSiteNode(client);
        System.out.println("Sitenode  " + siteNode);
        String libNode = getDocumentLibraryNodeId(client, siteNode);
        // System.out.println("Libnode " + libNode);

        String auth = Base64.getEncoder()
            .encodeToString((AlfrescoConstants.username + ":" + AlfrescoConstants.password).getBytes());

        // Debug because Alfrescco only returns the fields if filled in. This however
        // this should not affect Put, which it seems to do
        // String nodeid = GetNewNodeID(client, libNode, "password.txt");
        // getNodeFields("ecb97ec6-7a68-490a-802b-52cfc5339941", client, auth);
        String endpoint = String.format(
            "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s/children",
            AlfrescoConstants.alfrescoBaseUrl, libNode);
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
    }
    return "Failed";
  }

  public void GetNode() {
    try {
      String endpoint = "http://localhost:8080/alfresco/api/-default-/public/alfresco/versions/1/nodes/" + nodeId;
      String auth = Base64.getEncoder().encodeToString(
          (AlfrescoConstants.username + ":" + AlfrescoConstants.password).getBytes(StandardCharsets.UTF_8));

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
            // Check if UUID present
            // Get node content
            //alfresconNodeResponse.entry.properties.otherProperties.keySet().forEach(System.out::println);
            alfresconNodeResponse.UUID = alfresconNodeResponse.entry.properties.otherProperties.get("contain:IOUUID").toString();
            Object value = alfresconNodeResponse.entry.properties.otherProperties.get("contain:IOMOVE");
            String MOVETO = value != null ? value.toString() : "";
            alfresconNodeResponse.MoveTo = MOVETO;
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

  private CloseableHttpResponse ProcessGetRequest(String endpoint) {
    CloseableHttpResponse response = null;
    String auth = Base64.getEncoder().encodeToString(
        (AlfrescoConstants.username + ":" + AlfrescoConstants.password).getBytes(StandardCharsets.UTF_8));

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

  private void GetNodeContent() throws IOException {

    String endpoint = "http://localhost:8080/alfresco/api/-default-/public/alfresco/versions/1/nodes/" + nodeId
        + "/content";

    String auth = Base64.getEncoder()
        .encodeToString(
            (AlfrescoConstants.username + ":" + AlfrescoConstants.password).getBytes(StandardCharsets.UTF_8));

    HttpGet request = new HttpGet(endpoint);
    request.setHeader("Authorization", "Basic " + auth);

    try (CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = client.execute(request);
        InputStream in = response.getEntity().getContent()) {

      if (response.getCode() != 200) {
        throw new IOException("HTTP error " + response.getCode());
      }

      this.alfresconNodeResponse.file = in.readAllBytes();
    }
  }

  public String UpdateNode(NodeTypeFields field, Optional<String> IOPath, Optional<String> fieldValue) {
    try {
      String endpoint = "http://localhost:8080/alfresco/api/-default-/public/alfresco/versions/1/nodes/" + nodeId;
      // Map enum to Alfresco property
      String propertyName;
      String Path = IOPath.orElse("Unknown");
      String propertyValue = fieldValue.orElse("Dummy");
      String action = "";
      switch (field) {
        case UUID:
          propertyName = "contain:IOUUID"; // custom aspect property
          propertyValue = UUIDUtil.getUUID();
          ;
          action = "Assign UUID " + propertyValue + "  to new Alfresco IO " + this.alfresconNodeResponse.entry.name;
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
          + this.alfresconNodeResponse.entry.name + " " + jsonBody
          + contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.RESET);

      // Encode username:password for Basic Auth
      String auth = Base64.getEncoder().encodeToString(
          (AlfrescoConstants.username + ":" + AlfrescoConstants.password).getBytes(StandardCharsets.UTF_8));

      try (CloseableHttpClient client = HttpClients.createDefault()) {
        HttpPut request = new HttpPut(endpoint);
        request.setHeader("Authorization", "Basic " + auth);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

        var response = client.execute(request);
        int statusCode = response.getCode();
        String responseJson = EntityUtils.toString(response.getEntity());

        if (statusCode == 200) {
          // Test ballenbak
          IOLog.log(
              propertyValue,
              this.alfresconNodeResponse.entry.id,
              Path,
              action,
              AlfrescoConstants.ContainPlatforms.ALFRESCO.toString(),
              AlfrescoConstants.ContainPlatforms.ALFRESCO.toString(),
              //UUIDUtil.getUUID(),
              null,
              this.alfresconNodeResponse.entry.name,
              jsonBody,
              eActionPerformed.ASSIGNUUID,
              alfresconNodeResponse.entry.modifiedByUser.displayName
              );
          
          System.out.println("Node updated successfully:");
          System.out.println(responseJson);
          return  propertyValue;
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

  public void RelocateIO(RelocateInformationObject IOobject) {
    /// ==================================================
    /// NOTE> NO VERSIONING AND COPY CONTROLS INCLUDED!!!
    /// Proper returnvalue needed..
    /// ==================================================

    // Could be done from here but because it is not sure from where relocation is
    // performed we are using a REST API
    GraphService GService = new GraphService();
    this.nodeId = IOobject.getId();
    // For now assumed only happy path. If something goes wrong rollback in
    // ballenbak and complete transacton
    String action = "Move UUID " + IOobject.getUuid() + " : " + IOobject.getFileName() + " from "
        + IOobject.getPlatfrom() + " to " + IOobject.getPlatformTo();
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
        "System"
        );
         GService.uploadAlfrescoNodeToSP(IOobject);
    DeleteAlfrescoNode();
  }

  private void DeleteAlfrescoNode() {
    String endpoint = "http://localhost:8080/alfresco/api/-default-/public/alfresco/versions/1/nodes/" + nodeId;
    String auth = Base64.getEncoder().encodeToString(
        (AlfrescoConstants.username + ":" + AlfrescoConstants.password).getBytes(StandardCharsets.UTF_8));

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