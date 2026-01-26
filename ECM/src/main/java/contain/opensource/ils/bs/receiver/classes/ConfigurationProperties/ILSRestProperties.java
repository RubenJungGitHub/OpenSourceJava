package contain.opensource.ils.bs.receiver.classes.ConfigurationProperties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
@Component
@ConfigurationProperties(prefix = "ils")
public class ILSRestProperties {
    private String baseUrl;

    // getters & setters
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @PostConstruct
    public void init() {
        System.out.println("ILS baseUrl in postcinstruct : " + baseUrl);
    }
}