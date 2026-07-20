package contain.opensource.ils.bs.taxonomycacher.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

//@Configuration
@ConfigurationProperties(prefix = "spring.redis")
public class RedisConfigProperties {

    private String host;
    private int port;

    // Getters and setters
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}