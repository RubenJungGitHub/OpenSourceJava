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
public class LocalLlamaService {

    private final RestTemplate restTemplate;
    // Adres van je lokale Ollama instantie. MOET UIT CONIF KOMEN!!!!
    private final String ollamaApiUrl = "http://ollama:11434/api/generate";

    public LocalLlamaService() {
        this.restTemplate = new RestTemplate();
    }

    public String evaluate(String promptText) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "llama3.2");
        requestBody.put("prompt", promptText);
        requestBody.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // Specificeer Map<String, Object> op de ResponseEntity
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
            // Log de echte foutmelding in je terminal voor debug-doeleinden
            System.err.println("Ollama aanroep mislukt: " + e.getMessage());
            e.printStackTrace();
        }

        return "UNKNOWN";
    }
}