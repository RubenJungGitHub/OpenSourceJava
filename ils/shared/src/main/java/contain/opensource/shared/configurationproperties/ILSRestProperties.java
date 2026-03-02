package contain.opensource.shared.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@ConfigurationProperties(prefix = "ils")
public class ILSRestProperties {
    private String baseUrl;
    private String DeltaLinkFile;
    private String uudiutilendpoint;
    private String bindendpoint;
    private String relocateendpoint;

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

    public String getuudiutilendpoint() {
        return uudiutilendpoint;
    }

    public void setuudiutilendpoint(String uudiutilendPoint) {
        this.uudiutilendpoint = uudiutilendPoint;
    }

    public String getbindendpoint() {
        return bindendpoint;
    }

    public void setbindendpoint(String bindendpoint) {
        this.bindendpoint = bindendpoint;
    }

    
    public String getrelocateendpoint() {
        return relocateendpoint;
    }

    public void setrelocateendpoint(String relocateendpoint) {
        this.relocateendpoint = relocateendpoint;
    }

    @PostConstruct
    public void init() {
        System.out.println("ILS baseUrl in postcinstruct : " + baseUrl);
    }
}