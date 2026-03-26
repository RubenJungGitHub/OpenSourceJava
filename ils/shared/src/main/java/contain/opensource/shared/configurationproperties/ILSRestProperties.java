package contain.opensource.shared.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.annotation.PostConstruct;

@ConfigurationProperties(prefix = "ils")
public class ILSRestProperties {

    private String baseurl;
    private String deltalinkfile;
    private String uudiutilendpoint;
    private String bindendpoint;
    private String relocateendpoint;
    private String ruleenginemoveendpoint;
    private String ruleenginecontainerendpoint;
    private String ruleengineendpoint;
    private String ruleengineprojectname;
    private String processspitemsendpoint;
    private String validatealfrescouuidendpoint;

    // ----------------- Getters & Setters -----------------
    public String getbaseurl() {
        return baseurl;
    }

    public void setbaseurl(String baseurl) {
        this.baseurl = baseurl;
    }

    public String getdeltalinkfile() {
        return deltalinkfile;
    }

    public void setdeltalinkfile(String deltalinkfile) {
        this.deltalinkfile = deltalinkfile;
    }

    public String getuudiutilendpoint() {
        return uudiutilendpoint;
    }

    public void setuudiutilendpoint(String uudiutilendpoint) {
        this.uudiutilendpoint = uudiutilendpoint;
    }

    public String getbindendpoint() {
        return bindendpoint;
    }

    public void setbindendpoint(String bindendpoint) {
        this.bindendpoint = bindendpoint;
    }

    public String getruleenginemoveendpoint() {
        return ruleenginemoveendpoint;
    }

    public void setruleenginemoveendpoint(String ruleenginemoveendpoint) {
        this.ruleenginemoveendpoint = ruleenginemoveendpoint;
    }

    public String getrelocateendpoint() {
        return relocateendpoint;
    }

    public void setrelocateendpoint(String relocateendpoint) {
        this.relocateendpoint = relocateendpoint;
    }

    public String getruleenginecontainerendpoint() {
        return ruleenginecontainerendpoint;
    }

    public void setruleenginecontainerendpoint(String ruleenginecontainerendpoint) {
        this.ruleenginecontainerendpoint = ruleenginecontainerendpoint;
    }

    public String getruleengineendpoint() {
        return ruleengineendpoint;
    }

    public void setruleengineendpoint(String ruleengineendpoint) {
        this.ruleengineendpoint = ruleengineendpoint;
    }

    public String getruleengineprojectname() {
        return ruleengineprojectname;
    }

    public void setruleengineprojectname(String ruleengineprojectname) {
        this.ruleengineprojectname = ruleengineprojectname;
    }

    public String getprocessspitemsendpoint() {
        return processspitemsendpoint;
    }

    public void setprocessspitemsendpoint(String processspitemsendpoint) {
        this.processspitemsendpoint = processspitemsendpoint;
    }

    
    public String getvalidatealfrescouuidendpoint() {
        return validatealfrescouuidendpoint;
    }

    public void setvalidatealfrescouuidendpoint(String validatealfrescouuidendpoint) {
        this.validatealfrescouuidendpoint = validatealfrescouuidendpoint;
    }


    
    @PostConstruct
    public void init() {
        System.out.println("ILSRestProperties loaded:");
        System.out.println("baseurl = " + baseurl);
        System.out.println("deltalinkfile = " + deltalinkfile);
        System.out.println("uudiutilendpoint = " + uudiutilendpoint);
        System.out.println("bindendpoint = " + bindendpoint);
        System.out.println("relocateendpoint = " + relocateendpoint);
        System.out.println("ruleengineendpoint = " + ruleengineendpoint);
        System.out.println("ruleenginecontainerendpoint = " + ruleenginecontainerendpoint);
        System.out.println("ruleengineprojectname = " + ruleengineprojectname);
        System.out.println("ruleendpoint = " + ruleenginemoveendpoint);
        System.out.println("processspitemsendpoint = " + processspitemsendpoint);
        System.out.println("validatealfrescouuidendpoint = " + validatealfrescouuidendpoint);
        
    }
}