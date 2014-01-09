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
import org.apache.camel.CamelContext;
import org.apache.camel.spi.ComponentResolver;
import org.apache.camel.util.ObjectHelper;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

/**
 * A {@link ComponentResolver} for the {@link AMQComponent}
 */
@Service(ComponentResolver.class)
@Property(name = "component", value = "amq")
@Component(name = "org.fusesource.mq.fabric.camel.resolver", label = "JBoss A-MQ Fabric Camel Component Resolver", immediate = true, metatype = false)
public class AMQComponentResolver implements ComponentResolver {
    @Reference(referenceInterface = ActiveMQConnectionFactory.class)
    private ActiveMQConnectionFactory connectionFactory;

    @Activate
    void activate() throws Exception {
        ObjectHelper.notNull(connectionFactory, "connectionFactory", this);
    }
    @Deactivate
    void deactivate() {
    }

    @Override
    public org.apache.camel.Component resolveComponent(String name, CamelContext camelContext) throws Exception {
        if (name.equals("amq") || name.equals("activemq")) {
            System.out.println("Creating an instance of the AMQComponent");
            return new AMQComponent(camelContext, connectionFactory);
        }
        return null;
    }

}
