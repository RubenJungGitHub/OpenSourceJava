package contain.opensource.ils.bs.receiver.classes.alfresco;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import com.azure.core.http.HttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import contain.opensource.ils.bs.receiver.classes.RelocateInformationObject;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.NodeTypeFields;
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

  public void  uploadSPItemToAlfresco(RelocateInformationObject IOobject) {
      try {
      // First get SiteNode
      try (CloseableHttpClient client = HttpClients.createDefault()) {
        String siteNode = GetAlfrescoSiteNode(client);
        System.out.println("Sitenode  " + siteNode);
        String libNode = getDocumentLibraryNodeId(client, siteNode);
        System.out.println("Libnode   " + libNode);
        
        String auth = Base64.getEncoder()
                .encodeToString((AlfrescoConstants.username + ":" + AlfrescoConstants.password).getBytes());

        //Actual upload
        String endpoint = String.format(
            "%s/alfresco/api/-default-/public/alfresco/versions/1/nodes/%s/children",
            AlfrescoConstants.alfrescoBaseUrl, libNode);
           HttpPost post = new HttpPost(endpoint);
        post.setHeader("Authorization", "Basic " + auth);
        post.setHeader("Accept", "application/json");   

        // Build multipart form: file + optional properties
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
     //   builder.addBinaryBody("filedata", content, ContentType.create(mimetype), fileName);

      }
    } catch (Exception e) {
      System.err.println("Exception uploading item to alfresco : " + e);
      e.printStackTrace();
    }
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

            // Navigate to the title
            alfresconNodeResponse.Title = rootNode.path("entry")
                .path("properties")
                .path("cm:title")
                .asText();
            // Check if UUID present
            // Get node content

            alfresconNodeResponse.UUID = alfresconNodeResponse.entry.properties.otherProperties.get("contain:UUID")
                .toString();
            alfresconNodeResponse.MoveTo = alfresconNodeResponse.entry.properties.otherProperties.get("contain:Move")
                .toString();
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
          } catch (java.lang.NullPointerException e) {
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

  public void UpdateNode(NodeTypeFields field, Optional<String> fieldValue) {
    try {
      String endpoint = "http://localhost:8080/alfresco/api/-default-/public/alfresco/versions/1/nodes/" + nodeId;
      // Map enum to Alfresco property
      String propertyName;
      String propertyValue = fieldValue.orElse("Dummy");
      switch (field) {
        case UUID:
          propertyName = "contain:UUID"; // custom aspect property
          propertyValue = UUID.randomUUID().toString();
          break;
        case Title:
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
          System.out.println("Node updated successfully:");
          System.out.println(responseJson);
        } else {
          System.err.println("Failed to update node. Status code: " + statusCode);
          System.err.println(responseJson);
        }
      }
    } catch (Exception e) {
      System.err.println("Exception updating field:");
      e.printStackTrace();
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