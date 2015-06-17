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

import org.apache.camel.CamelContext;
import org.apache.camel.spi.ComponentResolver;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

/**
 * A {@link ComponentResolver} for the {@link AMQComponent}
 */
@Service(ComponentResolver.class)
@Property(name = "component", value = "amq")
@Component(name = "io.fabric8.mq.camel.resolver", label = "Fabric8 MQ Camel Component Resolver", immediate = true, metatype = false)
public class AMQComponentResolver implements ComponentResolver {

    @Activate
    void activate() throws Exception {
    }

    @Deactivate
    void deactivate() {
    }

    @Override
    public org.apache.camel.Component resolveComponent(String name, CamelContext camelContext) throws Exception {
        if (name.equals("amq") || name.equals("activemq")) {
            return new AMQComponent(camelContext);
        }
        return null;
    }

}
