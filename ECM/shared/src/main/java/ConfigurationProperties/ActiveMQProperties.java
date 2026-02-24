package contain.opensource.shared.ConfigurationProperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

//@Configuration
@ConfigurationProperties(prefix = "activemq")
public class ActiveMQProperties {
    private String brokerUrl;
    private String user;
    private String password;
    private String sharepointQueue;
    private String alfrescoQueue;

    // getters & setters
    public String getBrokerUrl() {
        return brokerUrl;
    }

    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSharepointQueue() {
        return sharepointQueue;
    }

    public void setSharepointQueue(String SharepointQueue) {
        this.sharepointQueue = SharepointQueue;
    }

    public String getAlfrescoQueue() {
        return alfrescoQueue;
    }

    public void setAlfrescoQueue(String AlfrescoQueue) {
        this.alfrescoQueue = AlfrescoQueue;
    }
}
