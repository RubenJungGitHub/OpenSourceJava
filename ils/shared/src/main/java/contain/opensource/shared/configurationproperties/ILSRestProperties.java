package contain.opensource.shared.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.annotation.PostConstruct;


@ConfigurationProperties(prefix = "ils")
public class ILSRestProperties {

    private String baseUrl;
    private String deltaLinkFile;
    private String uudiutilendpoint;
    private String bindendpoint;
    private String relocateendpoint;
    private String ruleenginemoveendpoint;
    private String ruleenginecontainerendpoint;
    private String ruleengineendpoint;
    private String ruleengineprojectname;

    // ----------------- Getters & Setters -----------------
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDeltaLinkFile() {
        return deltaLinkFile;
    }

    public void setDeltaLinkFile(String deltaLinkFile) {
        this.deltaLinkFile = deltaLinkFile;
    }

    public String getUudiutilendpoint() {
        return uudiutilendpoint;
    }

    public void setUudiutilendpoint(String uudiutilendpoint) {
        this.uudiutilendpoint = uudiutilendpoint;
    }

    public String getBindendpoint() {
        return bindendpoint;
    }

    public void setBindendpoint(String bindendpoint) {
        this.bindendpoint = bindendpoint;
    }

    public String getruleenginemoveendpoint() {
        return ruleenginemoveendpoint;
    }

    public void setruleenginemoveendpoint(String ruleenginemoveendpoint) {
        this.ruleenginemoveendpoint = ruleenginemoveendpoint;
    }

    
    public String getRelocateendpoint() {
        return relocateendpoint;
    }

    public void setRelocateendpoint(String relocateendpoint) {
        this.relocateendpoint = relocateendpoint;
    }

    public String getRuleenginecontainerendpoint() {
        return ruleenginecontainerendpoint;
    }

    public void setRuleenginecontainerendpoint(String ruleenginecontainerendpoint) {
        this.ruleenginecontainerendpoint = ruleenginecontainerendpoint;
    }

    public String getRuleengineendpoint() {
        return ruleengineendpoint;
    }

    public void setRuleengineendpoint(String ruleengineendpoint) {
        this.ruleengineendpoint = ruleengineendpoint;
    }

    public String getRuleengineprojectname() {
        return ruleengineprojectname;
    }

    public void setRuleengineprojectname(String ruleengineprojectname) {
        this.ruleengineprojectname = ruleengineprojectname;
    }

    @PostConstruct
    public void init() {
        System.out.println("ILSRestProperties loaded:");
        System.out.println("baseUrl = " + baseUrl);
        System.out.println("deltaLinkFile = " + deltaLinkFile);
        System.out.println("uudiutilendpoint = " + uudiutilendpoint);
        System.out.println("bindendpoint = " + bindendpoint);
        System.out.println("relocateendpoint = " + relocateendpoint);
        System.out.println("ruleengineendpoint = " + ruleengineendpoint);
        System.out.println("ruleenginecontainerendpoint = " + ruleenginecontainerendpoint);
        System.out.println("ruleengineprojectname = " + ruleengineprojectname);
        System.out.println("ruleendpoint = " + ruleenginemoveendpoint);
    }
}