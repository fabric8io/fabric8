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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jms.JmsConfiguration;

/**
 * A Camel component for A-MQ which uses the Fabric MQ {@link ActiveMQConnectionFactory} service
 * for connecting to the correct broker group in the fabric.
 */
public class AMQComponent extends ActiveMQComponent {

    public AMQComponent() {
    }

    public AMQComponent(CamelContext camelContext) {
        super(camelContext);
    }

    public String getServiceName() {
        return getConfiguration().getServiceName();
    }

    public void setServiceName(String serviceName) {
        getConfiguration().setServiceName(serviceName);
    }

    public String getFailoverUrlParameters() {
        return getConfiguration().getFailoverUrlParameters();
    }

    public void setFailoverUrlParameters(String failoverUrlParameters) {
        getConfiguration().setFailoverUrlParameters(failoverUrlParameters);
    }

    @Override
    protected JmsConfiguration createConfiguration() {
        return new AMQConfiguration(this);
    }

    @Override
    public AMQConfiguration getConfiguration() {
        return (AMQConfiguration) super.getConfiguration();
    }

    @Override
    public void setConfiguration(JmsConfiguration configuration) {
        if (configuration instanceof AMQConfiguration) {
            super.setConfiguration(configuration);
        } else {
            throw new IllegalArgumentException("Must be an instanceof of AMQConfiguration");
        }
    }
}
