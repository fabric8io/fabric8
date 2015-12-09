/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.mq.core;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;

/**
 * A replacement for the {@link ActiveMQConnectionFactory} which is configured with the
 * Kubernetes service name {@link #setServiceName(String)} to connect to different broker groups
 * and it then discovers the broker to connect to via Kubernetes services.
 */
public class MQConnectionFactory extends ActiveMQConnectionFactory {
    private String serviceName;
    private String failoverUrlParameters;

    public MQConnectionFactory() {
    }

    public MQConnectionFactory(String userName, String password) {
        this();
        setUserName(userName);
        setPassword(password);
    }

    @Override
    public String getBrokerURL() {
        return MQs.getBrokerURL(getServiceName(), getFailoverUrlParameters());
    }

    @Override
    public Connection createConnection() throws JMSException {
        // make sure brokerUrl is set because ActiveMQ expect its set using the setBrokerUrl method
        String url = getBrokerURL();
        setBrokerURL(url);
        return super.createActiveMQConnection();
    }

    @Override
    public Connection createConnection(String userName, String password) throws JMSException {
        // make sure brokerUrl is set because ActiveMQ expect its set using the setBrokerUrl method
        String url = getBrokerURL();
        setBrokerURL(url);
        return super.createActiveMQConnection(userName, password);
    }

    public String getServiceName() {
        return serviceName;
    }

    /**
     * Sets the Kubernetes service name used to resolve against the <code>$serviceName_SERVICE_HOST</code> and
     * <code>$serviceName_SERVICE_PORT</code> environment variables to find the broker group to connect t.
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getFailoverUrlParameters() {
        return failoverUrlParameters;
    }

    /**
     * Allows any failover parameters to be specified after the <code>failover://host:port/</code> part of the brokerURL.
     */
    public void setFailoverUrlParameters(String failoverUrlParameters) {
        this.failoverUrlParameters = failoverUrlParameters;
    }
}
