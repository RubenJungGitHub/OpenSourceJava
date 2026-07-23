package contain.opensource.ils.bs.taxonomycacher.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import contain.opensource.ils.bs.taxonomycacher.model.TaxonomyResponse;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import contain.opensource.shared.configurationproperties.ILSRestProperties;
import java.net.URI;
import java.time.Duration;
import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import contain.opensource.ils.bs.taxonomycacher.redis.RedisManager;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service // Mark this as a Spring-managed service
public class CacheTaxonomies {

    private final ILSRestProperties ilsProperties;
    private final RedisManager redisManager;
    private final HttpClient httpClient;
    private static final Logger log = LoggerFactory.getLogger(CacheTaxonomies.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public CacheTaxonomies(ILSRestProperties ilsProperties, RedisManager redisManager) throws Exception {
        this.ilsProperties = ilsProperties;
        this.redisManager = redisManager;

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

    public void process() {
        // Now you have access to the service
        // get classifications

        String baseUrl = this.ilsProperties.gettaxonomyserviceendpoint();
        StringBuilder uriBuilder = new StringBuilder();

        try {
            uriBuilder.append(String.format("%s/gettaxonomies?parent=%s&lang=%s", baseUrl, "classification", "en"));
            uriBuilder.append("&includedepricated=").append(true);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uriBuilder.toString())) // Use the dynamic URI
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                try {
                    var classifications = objectMapper.readValue(response.body(), TaxonomyResponse.class);

                    if (classifications != null && classifications.options != null) {
                        for (TaxonomyResponse.Option option : classifications.options) {
                            // Example: Key = "taxonomy:classification:c_ab456850", Value = "Confidential"
                            String key = "taxonomy:classification:" + option.value;
                            // String CheckIndRedis = redisManager.getHashField(key, key);
                            // if (CheckIndRedis == null || CheckIndRedis.isEmpty()) {
                            this.redisManager.putHash("taxonomies", option.value, option.label, 3600); //
                            // Cache for 60 minutes
                            // }
                            System.out.println("Cached to Redis: " + key + " = " + option.label);
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to parse JSON: {}", e.getMessage());
                }
            }
            uriBuilder.delete(0, uriBuilder.length());            
            uriBuilder.append(String.format("%s/gettaxonomies?parent=%s&lang=%s", baseUrl, "marking", "en"));
            uriBuilder.append("&includedepricated=").append(true);
            request = HttpRequest.newBuilder()
                    .uri(URI.create(uriBuilder.toString())) // Use the dynamic URI
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                try {
                    var markings = objectMapper.readValue(response.body(), TaxonomyResponse.class);

                    if (markings != null && markings.options != null) {
                        for (TaxonomyResponse.Option option : markings.options) {
                            // Example: Key = "taxonomy:classification:c_ab456850", Value = "Confidential"
                            String key = "taxonomy:markings:" + option.value;
                            // String CheckIndRedis = redisManager.getHashField(key, key);
                            // if (CheckIndRedis == null || CheckIndRedis.isEmpty()) {
                            redisManager.putHash("taxonomies", option.value, option.label, 3600); //
                            // Cache for 60 minutes
                            // }
                            System.out.println("Cached to Redis: " + key + " = " + option.label);
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to parse JSON: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Failed to reach Taxonomy service at {}: {}", baseUrl, e.getMessage());
        }
    }
}
