package contain.opensource.ils.bs.receiver.services;

import contain.opensource.shared.configurationproperties.ILSRestProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.Duration;
import contain.opensource.ils.bs.receiver.model.TaxonomyResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

//MIND YOU>.. ALL CERTIFICATES ARE TRUSTED.. THIS IS FOR DEV PURPOSES ONLY
@Service

public class TaxonomyServiceClient {
    private static final Logger log = LoggerFactory.getLogger(TaxonomyServiceClient.class);
    private final HttpClient httpClient;
    private final ILSRestProperties ilsProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TaxonomyServiceClient(ILSRestProperties ilsProperties) throws Exception {
        this.ilsProperties = ilsProperties;

        // 1. Create a TrustManager that trusts all certs (already done)
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        // 2. IMPORTANT: We must disable Hostname Verification
        // Since HttpClient doesn't expose a simple setter for this,
        // the most reliable way in modern Java is to set this system property:
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");

        this.httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String GetTaxLabel(String taxid) {
        if (taxid == null || taxid.isBlank()) {
            return "No Marking Provided";
        }

        // Use a cleaner way to build URIs to avoid manual string concatenation errors
        String baseUrl = this.ilsProperties.gettaxonomyserviceendpoint();
        String uri = String.format("%s/gettaxonomyLabelFromGuid?guid=%s&lang=en", baseUrl, taxid);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json") // Good practice to define expected response
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                log.warn("Taxonomy service returned status {}: {}", response.statusCode(), response.body());
                return "Label Not Found";
            }
        } catch (Exception e) {
            log.error("Failed to reach Taxonomy service at {}: {}", baseUrl, e.getMessage());
            return "Service Unavailable";
        }
    }

    public TaxonomyResponse GetTaxonomies(String parent, String lang) {
        // TaxonomyResponse response = GetTaxonomies(parent, lang);
        String baseUrl = this.ilsProperties.gettaxonomyserviceendpoint();
        String uri = String.format("%s/gettaxonomies?parent=%s&lang=%s", baseUrl, parent, lang);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json") // Good practice to define expected response
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                try {
                    return objectMapper.readValue(response.body(), TaxonomyResponse.class);
                } catch (Exception e) {
                    log.error("Failed to parse JSON: {}", e.getMessage());
                    return null; // Return null if parsing fails
                }
            } else {
                log.warn("Server returned status: {}", response.statusCode());
                return null; // Return null if status is not 200
            }
        } catch (Exception e) {
            log.error("Failed to reach Taxonomy service at {}: {}", baseUrl, e.getMessage());
            return null; // Return null if connection fails
        }
    }
}
