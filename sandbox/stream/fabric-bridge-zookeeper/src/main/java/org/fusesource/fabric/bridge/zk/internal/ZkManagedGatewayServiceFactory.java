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

package io.fabric8.bridge.zk.internal;

import io.fabric8.bridge.zk.ZkGatewayConnector;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.springframework.util.StringUtils;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * A {@link ManagedServiceFactory} for creating {@link ZkGatewayConnector ZkGatewayConnectors}.
 *
 * @author Dhiraj Bokde
 */
public class ZkManagedGatewayServiceFactory extends AbstractZkManagedServiceFactory {

    private Map<String, ZkGatewayConnector> gatewayConnectorMap = new ConcurrentHashMap<String, ZkGatewayConnector>();

    @Override
    public void doDestroy() throws Exception {
        // destroy all running gateways
        for (String pid : gatewayConnectorMap.keySet()) {
            deleted(pid);
        }
    }

    @Override
    public String getName() {
        return "Fabric Gateway Server";
    }

    @Override
    public void updated(String pid, Dictionary incoming) throws ConfigurationException {

        if (gatewayConnectorMap.containsKey(pid)) {
            // destroy and recreate gateway connector
            LOG.info("Refreshing Gateway " + pid);
            deleted(pid);
        }

        Dictionary<String,String> properties = new Hashtable<String, String>();
        for (Enumeration keys = incoming.keys(); keys.hasMoreElements(); ) {
            String key = (String) keys.nextElement();
            Object value = incoming.get(key);
            if (value != null) {
                properties.put(key, value.toString());
            }
        }

        // create and add gateway connector
        gatewayConnectorMap.put(pid, createGatewayConnector(pid, properties));
        LOG.info("Started Gateway " + pid);
    }

    @Override
    public void doDeleted(String pid) {

        ZkGatewayConnector gatewayConnector = gatewayConnectorMap.remove(pid);
        if (gatewayConnector != null) {

            try {
                gatewayConnector.destroy();
                LOG.info("Destroyed Gateway " + pid);
            } catch (Exception e) {
                LOG.error("Error destroying gateway " + pid + " : " + e.getMessage(), e);
            }

        } else {
            LOG.error("Gateway " + pid + " not found");
        }

    }

    private ZkGatewayConnector createGatewayConnector(String pid, Dictionary<String, String> properties) throws ConfigurationException {
        ZkGatewayConnector gatewayConnector = new ZkGatewayConnector();
        gatewayConnector.setCurator(getZooKeeper());
        gatewayConnector.setFabricService(getFabricService());
        gatewayConnector.setId(pid);

        // populate gateway properties
        if (StringUtils.hasText(properties.get("versionName"))) {
            gatewayConnector.setVersionName(properties.get("versionName"));
        }
        if (StringUtils.hasText(properties.get("profileName"))) {
            gatewayConnector.setProfileName(properties.get("profileName"));
        }
        if (StringUtils.hasText(properties.get("inboundDestinationsRef"))) {
            gatewayConnector.setInboundDestinations(createDestinationsConfig(pid, properties.get("inboundDestinationsRef")));
        }
        if (StringUtils.hasText(properties.get("outboundDestinationsRef"))) {
            gatewayConnector.setOutboundDestinations(createDestinationsConfig(pid, properties.get("outboundDestinationsRef")));
        }

        gatewayConnector.setLocalBrokerConfig(createBrokerConfig(pid, "localBroker", properties));
        gatewayConnector.setExportedBrokerConfig(createBrokerConfig(pid, "exportedBroker", properties));

        gatewayConnector.setApplicationContext(createApplicationContext(pid));

        try {
            gatewayConnector.afterPropertiesSet();
            gatewayConnector.start();
        } catch (Exception e) {
            String msg = "Error starting gateway " + pid + " : " + e.getMessage();
            LOG.error(msg);
            throw new ConfigurationException("Start", msg, e);
        }

        return gatewayConnector;

    }

}
