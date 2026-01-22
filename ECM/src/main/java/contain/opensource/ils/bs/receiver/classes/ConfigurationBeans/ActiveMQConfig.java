package contain.opensource.ils.bs.receiver.classes.ConfigurationBeans;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import contain.opensource.ils.bs.receiver.classes.ConfigurationProperties.ActiveMQProperties;
import jakarta.annotation.PostConstruct;

@Configuration
@EnableConfigurationProperties(ActiveMQProperties.class)
public class ActiveMQConfig {

    private final ActiveMQProperties props;

    public ActiveMQConfig(ActiveMQProperties props) {
        this.props = props;
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        return new ActiveMQConnectionFactory(
                props.getUser(),
                props.getPassword(),
                props.getBrokerUrl());
    }

    @PostConstruct
    public void checkProps() {
        System.out.println("ActiveMQ brokerUrl=" + props.getBrokerUrl());
        System.out.println("ActiveMQ user=" + props.getUser());
        System.out.println("ActiveMQ password=" + props.getPassword());
    }
}
