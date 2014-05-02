/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.process.spring.boot.starter.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.jms.ConnectionFactory;

@Configuration
public class ActiveMQAutoConfiguration {

    // Broker URL

    @Value("${io.fabric8.process.spring.boot.starter.activemq.broker.url:}")
    String brokerUrl;

    @Autowired
    BrokerUrlResolver brokerUrlResolver;

    @ConditionalOnMissingClass({org.junit.runner.Runner.class,io.fabric8.mq.fabric.FabricDiscoveryAgent.class})
    @Bean
    BrokerUrlResolver defaultBrokerUrlResolver() {
        return new DefaultBrokerUrlResolver();
    }

    @ConditionalOnClass(org.junit.runner.Runner.class)
    @Bean
    BrokerUrlResolver testBrokerUrlResolver() {
        return new TestBrokerUrlResolver();
    }

    @ConditionalOnClass(io.fabric8.mq.fabric.FabricDiscoveryAgent.class)
    @ConditionalOnMissingClass(org.junit.runner.Runner.class)
    @Bean
    BrokerUrlResolver fabricDiscoveryBrokerUrlResolver() {
        return new FabricDiscoveryBrokerUrlResolver();
    }

    private String resolveBrokerUrl() {
        String resolvedBrokerUrl;
        if(!brokerUrl.isEmpty()) {
            resolvedBrokerUrl = brokerUrl;
        } else {
            resolvedBrokerUrl = brokerUrlResolver.brokerUrl();
        }
        return resolvedBrokerUrl;
    }

    // Broker authentication

    @Value("${io.fabric8.process.spring.boot.starter.activemq.broker.username:}")
    String brokerUsername;

    @Value("${io.fabric8.process.spring.boot.starter.activemq.broker.password:}")
    String brokerPassword;

    // Connection factory

    @Bean
    ConnectionFactory pooledConnectionFactory() {
        ActiveMQConnectionFactory amqConnectionFactory = new ActiveMQConnectionFactory(resolveBrokerUrl());
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