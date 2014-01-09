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
package org.fusesource.mq.fabric.cf;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.felix.scr.annotations.*;
import org.fusesource.common.util.Maps;
import io.fabric8.api.FabricService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


/**
 * An SCR service which creates an {@link org.apache.activemq.ActiveMQConnectionFactory} instance and registers it into the OSGi
 * registry so that it can be used from JMS or Camel
 */
@Service(ActiveMQConnectionFactory.class)
@Component(name = "org.fusesource.mq.fabric.cf", label = "Fabric8 ActiveMQ Connection Factory Service", immediate = true, configurationFactory = true, metatype = true)
public class ActiveMQConnectionFactoryService extends ActiveMQConnectionFactory {
    private static final transient Logger LOG = LoggerFactory.getLogger(ActiveMQConnectionFactoryService.class);

    @Reference(referenceInterface = FabricService.class, cardinality=ReferenceCardinality.OPTIONAL_UNARY)
    private FabricService fabricService;

    @Activate
    void activate(Map<String, ?> properties) throws Exception {
       updated(properties);
    }

    @Modified
    void updated(Map<String, ?> properties) throws Exception {
        // lets set the properties from config admin
        String group = Maps.stringValue(properties, "group", "default");
        String brokerUrl = Maps.stringValue(properties, "brokerUrl");
        if (brokerUrl == null) {
            brokerUrl = "discovery:(fabric:" + group + ")";
        }
        setBrokerURL(brokerUrl);

        // TODO should be able to find the ZK user using an API too!
        String user = Maps.stringValue(properties, "user", "admin");
        String password = Maps.stringValue(properties, "password",  (fabricService != null ? fabricService.getZookeeperPassword() : "admin"));

        setUserName(user);
        setPassword(password);

        LOG.info("Configuring " + this + " with brokerUrl: " + brokerUrl + " and user: " + user);
    }

    @Deactivate
    void deactivate() {
    }
}
