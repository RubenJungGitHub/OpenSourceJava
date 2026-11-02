package contain.opensource.ils.bs.receiver.classes.Classification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;

import contain.opensource.ils.bs.receiver.model.TaxonomyResponse;
import contain.opensource.ils.bs.receiver.services.PythonClassifierService.PythonClassifierResponse;
import contain.opensource.ils.bs.receiver.services.TaxonomyServiceClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
public class AutoCLassify {

  private final Random random = new Random();

  // URL voor de nieuwe Zero-Shot Classifier in OpenShift (configureerbaar via
  // application.properties)

  private final TaxonomyServiceClient taxonomyServiceClient;

  @Autowired
  public AutoCLassify(TaxonomyServiceClient taxonomyServiceClient) {
    this.taxonomyServiceClient = taxonomyServiceClient;
  }

  public ClassificationResult ClassifyIO(byte[] content) {
    RestTemplate restTemplate = new RestTemplate();

    // Genereer een random getal tussen 0.0 en 1.0 voor de gewogen verdeling
    double roll = random.nextDouble();

    // Temp to force llmaclassifier
    // roll = 0.75;

    if (roll < 0.60) {
      // 60% Kans: Bart-large (Zero-Shot Classifier)
      return callClassifier(content, restTemplate, "http://localhost:8080/api/v1/classifypythonService",
          "pythonclassifier");
    } else if (roll < 0.95) {
      // 35% Kans (van 0.60 tot 0.95): Llama Classifier endpoint
      return callClassifier(content, restTemplate, "http://localhost:8080/api/v1/classifyllamaService",
          "llamaclassifier");
    } else {
      // Disable gemini for now
      // 5% Kans (van 0.95 tot 1.0): Gemini Classifier endpoint
      // return callClassifier(content, restTemplate,
      // "http://localhost:8080/api/v1/classifygeminiservice",
      // "geminiclassifier");
      return callClassifier(content, restTemplate, "http://localhost:8080/api/v1/classifypythonService",
          "pythonclassifier");
    }
  }

  private ClassificationResult callClassifier(byte[] content, RestTemplate restTemplate, String url,

      String classifierName) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
      HttpEntity<byte[]> requestEntity = new HttpEntity<>(content, headers);

      ResponseEntity<ClassificationResult> response = restTemplate.postForEntity(
          url,
          requestEntity,
          ClassificationResult.class);

      return response.getBody();
    } catch (Exception e) {
      System.err.println("Fout bij aanroepen " + classifierName + ": " + e.getMessage());
      return new ClassificationResult(classifierName, "ERROR", 0.0, "FAILED", "<Unknown>");
    }
  }

  /*
   * // ==========================================
   * // GECOMMENTARIEERDE ALTERNATIEVEN (Oude opties)
   * // ==========================================
   * 
   * // Llama service endpoint
   * // String url = "http://localhost:8080/api/v1/classifyllamaService";
   * 
   * // Gemini endpoint
   * // String url = "http://localhost:8080/api/v1/classifygeminiservice";
   * 
   * // ChatGPT service endpoint
   * // String url = "http://localhost:8080/api/v1/chatgptService";
   * 
   * HttpHeaders oldHeaders = new HttpHeaders();
   * oldHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
   * 
   * HttpEntity<byte[]> oldRequestEntity = new HttpEntity<>(content, oldHeaders);
   * 
   * ResponseEntity<ClassificationResult> oldResponse =
   * restTemplate.postForEntity(
   * url,
   * oldRequestEntity,
   * ClassificationResult.class);
   * 
   * return oldResponse.getBody();
   */
}
