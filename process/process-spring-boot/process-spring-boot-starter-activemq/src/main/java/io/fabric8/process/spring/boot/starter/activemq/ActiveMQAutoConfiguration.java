package io.fabric8.process.spring.boot.starter.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.jms.ConnectionFactory;

@Configuration
public class ActiveMQAutoConfiguration {

    @Value("${io.fabric8.process.spring.boot.starter.activemq.broker.url:vm://testBroker?broker.persistent=false}")
    String brokerUrl;

    @Value("${io.fabric8.process.spring.boot.starter.activemq.broker.username:}")
    String brokerUsername;

    @Value("${io.fabric8.process.spring.boot.starter.activemq.broker.password:}")
    String brokerPassword;

    @Bean
    ConnectionFactory pooledConnectionFactory() {
        ActiveMQConnectionFactory amqConnectionFactory = new ActiveMQConnectionFactory(brokerUrl);
        if(!brokerUsername.isEmpty()) {
            amqConnectionFactory.setUserName(brokerUsername);
        }
        if(!brokerPassword.isEmpty()) {
            amqConnectionFactory.setPassword(brokerPassword);
        }
        PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory();
        pooledConnectionFactory.setConnectionFactory(amqConnectionFactory);
        return pooledConnectionFactory;
    }

}