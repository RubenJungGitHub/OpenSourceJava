package contain.opensource.shared.configurationproperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
@ConfigurationProperties(prefix = "ils")
public class ILSRestProperties {
    private String baseUrl;
    private String DeltaLinkFile;

    // getters & setters
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDeltaLinkFile() {
        return DeltaLinkFile;
    }

    public void setDeltaLinkFile(String deltaLinkFile) {
        this.DeltaLinkFile = deltaLinkFile;
    }


    @PostConstruct
    public void init() {
        System.out.println("ILS baseUrl in postcinstruct : " + baseUrl);
    }
}