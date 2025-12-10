package contain.opensource.java.ils.bs.receiver.classes;


//import org.apache.hc.client5.http.classic.CloseableHttpClient;

public class AlfrescoObjectController {
  String nodeId;
  String username = "admin";
  String password = "admin";
  String alfrescoEndPoint;

  public AlfrescoObjectController(String nodeId) 
  {
    this.nodeId = nodeId;
   }

  public void GetNode() {
    alfrescoEndPoint = "http://localhost:8080/alfresco/api/-default-/public/alfresco/versions/1/nodes/" + this.nodeId;
  }

  public void UpdateNode(String fieldId, String fieldValue) {
    try {

      // JSON body with properties to update
      String jsonBody = """
          {
            "properties": {
              "cm:title": "Updated Title",
              "cm:description": "Updated Description"
            }
          }
          """;

      // Encode username:password for Basic Auth
      String auth = java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

/* */

   /*     try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(alfrescoUrl + nodeId);
            request.setHeader("Authorization", "Basic " + auth);

            var response = client.execute(request);
            String json = EntityUtils.toString(response.getEntity());

            System.out.println("Node metadata:");
            System.out.println(json);
        }
            */
    }
     catch (Exception e)
    {
            System.err.println("Exception updating field:");
            e.printStackTrace();
    }
  }
}
