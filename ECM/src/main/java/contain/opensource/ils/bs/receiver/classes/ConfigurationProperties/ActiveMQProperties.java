package contain.opensource.ils.bs.receiver.classes.ConfigurationProperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

//@Configuration
@ConfigurationProperties(prefix = "activemq")
public class ActiveMQProperties {
    private String brokerUrl;
    private String user;
    private String password;

    // getters & setters
    public String getBrokerUrl() { return brokerUrl; }
    public void setBrokerUrl(String brokerUrl) { this.brokerUrl = brokerUrl; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
