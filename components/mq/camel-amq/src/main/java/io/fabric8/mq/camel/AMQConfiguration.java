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
package io.fabric8.mq.camel;

import io.fabric8.mq.core.MQConnectionFactory;
import io.fabric8.mq.core.MQs;
import org.apache.activemq.camel.component.ActiveMQConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A configuration object for the {@link AMQComponent} which uses Kubernetes service wiring
 * to find the correct ActiveMQ broker service to communicate with
 */
public class AMQConfiguration extends ActiveMQConfiguration {
    private static final transient Logger LOG = LoggerFactory.getLogger(AMQConfiguration.class);

    private String serviceName;
    private String failoverUrlParameters;

    public AMQConfiguration() {
        // use MQConnectionFactory as the out of the box connection factory
        // as it can lookup the ActiveMQ broker using kubernetes services and the ENV variables
        setConnectionFactory(new MQConnectionFactory());
    }

    public AMQConfiguration(AMQComponent component) {
        super.setActiveMQComponent(component);
        // use MQConnectionFactory as the out of the box connection factory
        // as it can lookup the ActiveMQ broker using kubernetes services and the ENV variables
        setConnectionFactory(new MQConnectionFactory());
    }

    @Override
    public String getBrokerURL() {
        return MQs.getBrokerURL(getServiceName(), getFailoverUrlParameters());
    }

    @Override
    public void setBrokerURL(String brokerURL) {
        // noop
        // the broker is using kubernetes services, so its always resolved in the getBrokerURL method
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


    // TODO if ever ActiveMQConfiguration provides a template method
    // to create a vanilla ActiveMQConnectionFactory before its wrapped in pooling
    // we could override that here!
}
