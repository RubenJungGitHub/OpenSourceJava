package contain.opensource.ils.bs.receiver.services;

import contain.opensource.ils.bs.receiver.classes.Classification.ClassificationResult;
import contain.opensource.ils.bs.receiver.model.TaxonomyResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PythonClassifierService {

    private final RestTemplate restTemplate;
    private final TaxonomyServiceClient taxonomyServiceClient;
    
    // Adres van je Python Zero-Shot microservice endpoint
private final String zeroShotUrl = "http://zero-shot-classifier-service:8000/classify";

    public PythonClassifierService(TaxonomyServiceClient taxonomyServiceClient) {
        this.restTemplate = new RestTemplate();
        this.taxonomyServiceClient = taxonomyServiceClient;
    }

    public ClassificationResult evaluate(String extractedText, List<String> candidateLabels) {
        // 1. Haal de taxonomie opnieuw op om de GUID mapping te garanderen
        TaxonomyResponse taxonomy = taxonomyServiceClient.GetTaxonomies("classification", "en");
        Map<String, String> labelToValueMap = new HashMap<>();

        if (taxonomy != null && taxonomy.options != null) {
            for (var option : taxonomy.options) {
                String cleanLabel = option.label != null ? option.label.replaceFirst("^-\\s*", "").trim() : "";
                String guidValue = option.value;
                if (!cleanLabel.isEmpty() && guidValue != null) {
                    labelToValueMap.put(cleanLabel.toLowerCase(), guidValue);
                }
            }
        }

        // 2. Bouw de request body specifiek voor de Python service (tekst + labels array)
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("text", extractedText);
        requestBody.put("labels", candidateLabels);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity requestEntity = new HttpEntity(requestBody, headers);

        try {
            ResponseEntity<PythonClassifierResponse> response = restTemplate.postForEntity(
                    zeroShotUrl,
                    requestEntity,
                    PythonClassifierResponse.class);

            PythonClassifierResponse pyResponse = response.getBody();
            if (pyResponse != null && pyResponse.getTop_label() != null) {
                String topLabel = pyResponse.getTop_label().trim();
                
                // Zoek de juiste Vocbench GUID op basis van het geretourneerde label
                String correspondingGuid = labelToValueMap.getOrDefault(topLabel.toLowerCase(), "UNKNOWN_GUID");

                return new ClassificationResult(
                        "bart-large-mnli",
                        topLabel,
                        pyResponse.getConfidence(),
                        "SUCCESS",
                        correspondingGuid
                );
            }
        } catch (Exception e) {
            System.err.println("Fout bij aanroepen Python Zero-Shot Classifier: " + e.getMessage());
            e.printStackTrace();
        }

        return new ClassificationResult("bart-large-mnli", "UNKNOWN", 0.0, "FAILED", "<Unknown>");
    }

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
}