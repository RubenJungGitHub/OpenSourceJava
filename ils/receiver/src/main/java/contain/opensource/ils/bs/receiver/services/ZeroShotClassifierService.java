package contain.opensource.ils.bs.receiver.services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.List;
import java.util.Map;

@Service
public class ZeroShotClassifierService {

    private final RestTemplate restTemplate = new RestTemplate();
    
    // De interne OpenShift DNS URL van je service op poort 8000
    private final String classifierUrl = "http://vocbench-classifier-service:8000/classify";

    public Map<String, Object> classifyText(String text, List<String> candidateLabels) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Stel de payload samen (zorg dat dit overeenkomt met wat jouw Python app.py verwacht)
        Map<String, Object> requestBody = Map.of(
            "text", text,
            "labels", candidateLabels
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            // Verstuur de POST naar de Python microservice
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(classifierUrl, request, Map.class);
            return response;
        } catch (Exception e) {
            // Handel eventuele verbindingsfouten af
            throw new RuntimeException("Fout bij communicatie met Zero-Shot Classifier microservice: " + e.getMessage(), e);
        }
    }
}