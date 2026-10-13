package contain.opensource.ils.bs.receiver.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.http.*;
import java.util.List;
import java.util.Map;

@Service
public class ChatGPTService {

    @Value("${openai.api.key:${OPENAI_API_KEY:#{null}}}")
    String apiKey;

    // OpenAI Chat Completions endpoint
    private final String CHATGPT_URL = "https://api.openai.com/v1/chat/completions";

    public String evaluate(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("FOUT: Geen geldige OpenAI API key gevonden in configuratie!");
            return "Unclassified";
        }

        RestTemplate restTemplate = new RestTemplate();

        // 1. OpenAI Request Body Structure
        Map<String, Object> requestBody = Map.of(
            "model", "gpt-4o-mini", // Of "gpt-4o" afhankelijk van je voorkeur
            "messages", List.of(
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.1
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // OpenAI gebruikt Bearer token authenticatie in plaats van een URL query param
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        int maxRetries = 5;
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                attempt++;
                ResponseEntity<Map> response = restTemplate.postForEntity(CHATGPT_URL, entity, Map.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    Map body = response.getBody();
                    
                    // 2. OpenAI Response Parsing Structure ("choices" -> "message" -> "content")
                    List<Map> choices = (List<Map>) body.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map message = (Map) choices.get(0).get("message");
                        if (message != null && message.containsKey("content")) {
                            return (String) message.get("content");
                        }
                    }
                }
                break;
                
            } catch (HttpStatusCodeException e) {
                // OpenAI gebruikt 429 Too Many Requests in plaats van 503 voor rate limits / drukte
                if ((e.getStatusCode().value() == 429 || e.getStatusCode().value() == 503) && attempt < maxRetries) {
                    long waitTime = 2000L * attempt;
                    System.err.println("ChatGPT Rate Limited/Unavailable - poging " + attempt + " mislukt. Wacht " + (waitTime / 1000) + "s...");
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }
                
                System.err.println("Fout bij aanroepen ChatGPT API (Status " + e.getStatusCode() + "): " + e.getMessage());
                break;
                
            } catch (Exception e) {
                System.err.println("Onverwachte fout bij aanroepen ChatGPT API: " + e.getMessage());
                break;
            }
        }

        return "EX-Unclassified";
    }
}