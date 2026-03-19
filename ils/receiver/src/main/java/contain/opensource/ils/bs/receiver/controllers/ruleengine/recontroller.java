package contain.opensource.ils.bs.receiver.controllers.ruleengine;
import java.net.URL;
import org.springframework.web.bind.annotation.RequestBody;
import java.net.HttpURLConnection;
import java.security.PrivateKey;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.MediaType;

import contain.opensource.shared.configurationproperties.ILSRestProperties;

@RestController
public class recontroller {

    ILSRestProperties ilsproperties;

    @Autowired
    public recontroller(ILSRestProperties ilsProperties)
    {
        this.ilsproperties = ilsProperties;
    }

    @GetMapping("/getREContainers")
    public String getREContainers() {
        try {
            String endpoint = ilsproperties.getRuleenginecontainerendpoint();
                   System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BG_MAGENTA
                    + contain.opensource.shared.constants.AlfrescoConstants.RED
                    + ("Rule engine container endpoint  " + endpoint)
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);

                    URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");

            int status = conn.getResponseCode();
            System.out.println("Accessing uuid rest url on " + endpoint + " return code -> " + status);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            return "shot";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error acquiring token: " + e.getMessage();
        }
    }
}