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
package io.fabric8.mq.fabric.camel;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQConfiguration;

/**
 * A configuration object for the {@link AMQComponent} which makes it easy to use
 * the ActiveMQConnectionFactory service
 */
public class AMQConfiguration extends ActiveMQConfiguration {
    private final ActiveMQConnectionFactory activeMQConnectionFactory;

    public AMQConfiguration(ActiveMQConnectionFactory activeMQConnectionFactory) {
        this.activeMQConnectionFactory = activeMQConnectionFactory;
        setBrokerURL(activeMQConnectionFactory.getBrokerURL());
        setUserName(activeMQConnectionFactory.getUserName());
        setPassword(activeMQConnectionFactory.getPassword());
    }

    public ActiveMQConnectionFactory getActiveMQConnectionFactory() {
        return activeMQConnectionFactory;
    }

    // TODO if ever ActiveMQConfiguration provides a template method
    // to create a vanilla ActiveMQConnectionFactory before its wrapped in pooling
    // we could override that here!
}
