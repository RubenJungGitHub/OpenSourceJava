package contain.opensource.ils.bs.receiver.services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

@Service
public class LlamaClassifierService {

    private final RestTemplate restTemplate;
    // Adres van je lokale Ollama instantie. MOET UIT CONIF KOMEN!!!!
    private final String ollamaApiUrl = "http://ollama:11434/api/generate";

    public LlamaClassifierService() {
        this.restTemplate = new RestTemplate();
    }

    public String evaluate(String promptText) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "llama3.2");
        requestBody.put("prompt", promptText);
        requestBody.put("stream", false);

        // --- FORCE DETERMINISTIC RESULTS ---
        Map<String, Object> options = new HashMap<>();
        options.put("temperature", 0.0); // 0.0 = Geen creativiteit, puur voorspelbaar
        options.put("seed", 42); // Vaste seed voor consistente random-generatie
        requestBody.put("options", options);
        // -----------------------------------

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    ollamaApiUrl,
                    org.springframework.http.HttpMethod.POST,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("response")) {
                Object responseValue = responseBody.get("response");
                if (responseValue != null) {
                    return responseValue.toString().trim();
                }
            }
        } catch (Exception e) {
            System.err.println("Ollama aanroep mislukt: " + e.getMessage());
            e.printStackTrace();
            return e.getMessage();
        }

        return "UNKNOWN";
    }
}