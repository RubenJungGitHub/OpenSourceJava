package contain.opensource.ils.bs.receiver.classes.Classification;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
@Component
public class AutoCLassify {
  public ClassificationResult ClassifyIO(byte[] content) {
    RestTemplate restTemplate = new RestTemplate();
    // FROM CONFIG!!!!!! (Let op: hier zou tzt de URL van je eigen service of
    // cluster kunnen staan)

    // lamaservieEndpoint. Text generator in OS pod. No licence but inconsistant and unreliable responds. 
    String url = "http://localhost:8080/api/v1/classifyllamaService";

    // GeminiEndpoint
    //Very limited usage without payed account (Approx 10 / 24 hours) before 429 too many requests.  
    //String url = "http://localhost:8080/api/v1/classifygeminiservice";


    // chatgptservieEndpoint
    //No use without payed account
    //String url = "http://localhost:8080/api/v1/chatgptService";


    // 1. Geef aan dat het om een raw binary stream gaat
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

    // 2. Stop de byte[] direct in de HttpEntity body
    HttpEntity<byte[]> requestEntity = new HttpEntity<>(content, headers);

    // 3. Verstuur het request
    ResponseEntity<ClassificationResult> response = restTemplate.postForEntity(
        url,
        requestEntity,
        ClassificationResult.class);

    return response.getBody();
  }
}
