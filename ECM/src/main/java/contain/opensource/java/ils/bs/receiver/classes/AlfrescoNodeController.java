package contain.opensource.java.ils.bs.receiver.classes;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import contain.opensource.java.ils.bs.receiver.constants.AlfrescoConstants.NodeTypeFields;
import contain.opensource.java.ils.bs.receiver.services.GraphService;

public class AlfrescoNodeController {
  String nodeId;
  String username = "admin";
  String password = "admin";
  String alfrescoEndPoint;
  AlfrescoNodeResponse alfresconNodeResponse = null;

  public AlfrescoNodeController(String nodeId) {
    this.nodeId = nodeId;

  }

  public void GetNode() {
    try {
      String endpoint = "http://localhost:8080/alfresco/api/-default-/public/alfresco/versions/1/nodes/" + nodeId;
      String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

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
    String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

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
        .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

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

      System.out.println(contain.opensource.java.ils.bs.receiver.constants.AlfrescoConstants.MAGENTA + "Updating "
          + this.alfresconNodeResponse.entry.name + " " + jsonBody
          + contain.opensource.java.ils.bs.receiver.constants.AlfrescoConstants.RESET);

      // Encode username:password for Basic Auth
      String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

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

  public void MoveNode() {
    /// ==================================================
    /// NOTE> NO VERSIONING AND COPY CONTROLS INCLUDED!!!
    /// ==================================================
    GraphService GService = new GraphService();
    GService.uploadAlfrescoNodeToSP(alfresconNodeResponse);
    DeleteNode();

  }

  private void DeleteNode() {
    String endpoint = "http://localhost:8080/alfresco/api/-default-/public/alfresco/versions/1/nodes/" + nodeId;
    String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

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