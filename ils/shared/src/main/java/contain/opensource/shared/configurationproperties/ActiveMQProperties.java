package contain.opensource.shared.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

//@Component
@ConfigurationProperties(prefix = "activemq")
public class ActiveMQProperties {

// Initialize these to prevent NullPointerExceptions during Spring's startup phase
    private BrokerConfig alfrescoSource = new BrokerConfig(); 
    private BrokerConfig migrationHub = new BrokerConfig();

    // --- Inner class to represent a single Broker's settings ---
    public static class BrokerConfig {
        private String brokerUrl;
        private String user;
        private String password;
        private String queue;
        private String sharepointQueue;
        private String migrationQueue;

        // Getters and Setters
        public String getBrokerUrl() { return brokerUrl; }
        public void setBrokerUrl(String brokerUrl) { this.brokerUrl = brokerUrl; }

        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getQueue() { return queue; }
        public void setQueue(String queue) { this.queue = queue; }

        public String getSharepointQueue() { return sharepointQueue; }
        public void setSharepointQueue(String sharepointQueue) { this.sharepointQueue = sharepointQueue; }

        public String getMigrationQueue() { return migrationQueue; }
        public void setMigrationQueue(String migrationQueue) { this.migrationQueue = migrationQueue; }
    }

    // --- Getters and Setters for the main blocks ---
    public BrokerConfig getAlfrescoSource() { return alfrescoSource; }
    public void setAlfrescoSource(BrokerConfig alfrescoSource) { this.alfrescoSource = alfrescoSource; }

    public BrokerConfig getMigrationHub() { return migrationHub; }
    public void setMigrationHub(BrokerConfig migrationHub) { this.migrationHub = migrationHub; }
}