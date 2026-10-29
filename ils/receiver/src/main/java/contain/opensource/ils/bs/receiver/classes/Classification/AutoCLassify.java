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
import contain.opensource.ils.bs.receiver.services.TaxonomyServiceClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
public class AutoCLassify {

  private final TaxonomyServiceClient taxonomyServiceClient;
  private final Random random = new Random();

  // URL voor de nieuwe Zero-Shot Classifier in OpenShift (configureerbaar via
  // application.properties)
  @Value("${classifier.service.url:http://zero-shot-classifier-service:8000/classify}")
  private String zeroShotUrl;

  @Autowired
  public AutoCLassify(TaxonomyServiceClient taxonomyServiceClient) {
    this.taxonomyServiceClient = taxonomyServiceClient;
  }

  public ClassificationResult ClassifyIO(byte[] content) {
    RestTemplate restTemplate = new RestTemplate();

    // Genereer een random getal tussen 0.0 en 1.0 voor de gewogen verdeling
    double roll = random.nextDouble();

    //Temp to force llmaclassifier 
    roll = 0.75;

    if (roll < 0.60) {
      // 60% Kans: Bart-large (Zero-Shot Classifier)
      return callZeroShotClassifier(content, restTemplate);
    } else if (roll < 0.95) {
      // 35% Kans (van 0.60 tot 0.95): Llama Classifier endpoint
      return callLegacyClassifier(content, restTemplate, "http://localhost:8080/api/v1/classifyllamaService",
          "llamaclassifier");
    } else {
      // 5% Kans (van 0.95 tot 1.0): Gemini Classifier endpoint
      return callLegacyClassifier(content, restTemplate, "http://localhost:8080/api/v1/classifygeminiservice",
          "geminiclassifier");
    }
  }

  private ClassificationResult callZeroShotClassifier(byte[] content, RestTemplate restTemplate) {
    String extractedText = new String(content, StandardCharsets.UTF_8);

    // Haal de labels dynamisch op uit VocBench via de TaxonomyServiceClient
    TaxonomyResponse taxonomy = taxonomyServiceClient.GetTaxonomies("classification", "en");
    List<String> candidateLabels = new ArrayList<>();

    if (taxonomy != null && taxonomy.options != null) {
      for (var option : taxonomy.options) {
        String cleanLabel = option.label.replaceFirst("^-\\s*", "").trim();
        if (!cleanLabel.isEmpty()
            && !cleanLabel.toLowerCase().contains("unable to classify content")
            && !cleanLabel.toLowerCase().contains("unclassified")) {
          candidateLabels.add(cleanLabel);
        }
      }
    }

    if (candidateLabels.isEmpty()) {
      candidateLabels = List.of("General");
    }

    Map<String, Object> requestBody = Map.of(
        "text", extractedText,
        "labels", candidateLabels);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

    try {
      ResponseEntity<PythonClassifierResponse> response = restTemplate.postForEntity(
          zeroShotUrl,
          requestEntity,
          PythonClassifierResponse.class);

      PythonClassifierResponse pyResponse = response.getBody();
      if (pyResponse != null) {
        return new ClassificationResult(
            "bart-large-mnli",
            pyResponse.getTop_label(),
            pyResponse.getConfidence(),
            "TODO",
            "SUCCESS");
      }
    } catch (Exception e) {
      System.err.println("Fout bij aanroepen Zero-Shot Classifier: " + e.getMessage());
    }

    return new ClassificationResult("bart-large-mnli", "UNKNOWN", 0.0, "FAILED","<Unknown>");
  }

  private ClassificationResult callLegacyClassifier(byte[] content, RestTemplate restTemplate, String url,
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

  // Interne helper class om het antwoord van de Python Zero-Shot service op te
  // vangen
  public static class PythonClassifierResponse {
    private String top_label;
    private double confidence;

    public String getTop_label() {
      return top_label;
    }

    public void setTop_label(String top_label) {
      this.top_label = top_label;
    }

    public double getConfidence() {
      return confidence;
    }

    public void setConfidence(double confidence) {
      this.confidence = confidence;
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
