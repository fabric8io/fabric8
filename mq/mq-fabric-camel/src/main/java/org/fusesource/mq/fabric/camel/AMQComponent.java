/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.mq.fabric.camel;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.activemq.camel.component.ActiveMQConfiguration;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.util.ObjectHelper;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import javax.jms.JMSException;
import java.util.Map;

/**
 * A Camel component for A-MQ which uses the Fabric MQ {@link ActiveMQConnectionFactory} service
 * for connecting to the correct broker group in the fabric.
 */
public class AMQComponent extends ActiveMQComponent {

    public AMQComponent(CamelContext camelContext, ActiveMQConnectionFactory connectionFactory) {
        super(camelContext);
        setConfiguration(new AMQConfiguration(connectionFactory));
    }

    @Activate
    void activate() throws Exception {
    }

    @Deactivate
    void deactivate() {
    }


}
