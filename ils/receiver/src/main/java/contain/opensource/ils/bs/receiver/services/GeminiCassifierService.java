package contain.opensource.ils.bs.receiver.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.http.*;
import java.util.List;
import java.util.Map;

@Service
public class GeminiCassifierService {

    @Value("${gemini.api.key:${GEMINI_API_KEY:#{null}}}")
    String apiKey;
    //Moet ENV Varoiable worden
    private final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=";

    public String evaluate(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("FOUT: Geen geldige Gemini API key gevonden in configuratie!");
            return "Unclassified";
        }

        RestTemplate restTemplate = new RestTemplate();
        String url = GEMINI_URL + apiKey;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)))));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        int maxRetries = 5;
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                attempt++;
                ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    Map body = response.getBody();
                    List<Map> candidates = (List<Map>) body.get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        Map content = (Map) candidates.get(0).get("content");
                        List<Map> parts = (List<Map>) content.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            return (String) parts.get(0).get("text");
                        }
                    }
                }
                break;
                
            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode().value() == 503 && attempt < maxRetries) {
                    long waitTime = 2000L * attempt;
                    System.err.println("Gemini 503 (Druk/Unavailable) - poging " + attempt + " mislukkt. Wacht " + (waitTime / 1000) + "s...");
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }
                
                System.err.println("Fout bij aanroepen Gemini API (Status " + e.getStatusCode() + "): " + e.getMessage());
                break;
                
            } catch (Exception e) {
                System.err.println("Onverwachte fout bij aanroepen Gemini API: " + e.getMessage());
                break;
            }
        }

        return "EX-Unclassified";
    }
}