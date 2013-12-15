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

import io.fabric8.bridge.zk.ZkBridgeConnector;
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
 * A {@link ManagedServiceFactory} for creating {@link ZkBridgeConnector ZkBridgeConnectors}.
 *
 * @author Dhiraj Bokde
 */
public class ZkManagedBridgeServiceFactory extends AbstractZkManagedServiceFactory {

    private Map<String, ZkBridgeConnector> bridgeConnectorMap = new ConcurrentHashMap<String, ZkBridgeConnector>();

    @Override
    public void doDestroy() throws Exception {
        // destroy all running bridges
        for (String pid : bridgeConnectorMap.keySet()) {
            deleted(pid);
        }
    }

    @Override
    public String getName() {
        return "Fabric Bridge Server";
    }

    @Override
    public void updated(String pid, Dictionary incoming) throws ConfigurationException {

        if (bridgeConnectorMap.containsKey(pid)) {
            // destroy and recreate bridge connector
            LOG.info("Refreshing Bridge " + pid);
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

        // create and add bridge connector
        bridgeConnectorMap.put(pid, createBridgeConnector(pid, properties));
        LOG.info("Started Bridge " + pid);
    }

    @Override
    public void doDeleted(String pid) {

        ZkBridgeConnector bridgeConnector = bridgeConnectorMap.remove(pid);
        if (bridgeConnector != null) {

            try {
                bridgeConnector.destroy();
                LOG.info("Destroyed Bridge " + pid);
            } catch (Exception e) {
                LOG.error("Error destroying bridge " + pid + " : " + e.getMessage(), e);
            }

        } else {
            LOG.error("Bridge " + pid + " not found");
        }

    }

    private ZkBridgeConnector createBridgeConnector(String pid, Dictionary<String, String> properties) throws ConfigurationException {
        ZkBridgeConnector bridgeConnector = new ZkBridgeConnector();
        bridgeConnector.setCurator(getZooKeeper());
        bridgeConnector.setFabricService(getFabricService());
        bridgeConnector.setId(pid);

        // populate bridge properties
        if (StringUtils.hasText(properties.get("versionName"))) {
            bridgeConnector.setVersionName(properties.get("versionName"));
        }
        if (StringUtils.hasText(properties.get("gatewayProfileName"))) {
            bridgeConnector.setGatewayProfileName(properties.get("gatewayProfileName"));
        }
        if (StringUtils.hasText(properties.get("gatewayConnectRetries"))) {
            bridgeConnector.setGatewayConnectRetries(Integer.parseInt(properties.get("gatewayConnectRetries")));
        }
        if (StringUtils.hasText(properties.get("gatewayStartupDelay"))) {
            bridgeConnector.setGatewayStartupDelay(Integer.parseInt(properties.get("gatewayStartupDelay")));
        }
        if (StringUtils.hasText(properties.get("inboundDestinationsRef"))) {
            bridgeConnector.setInboundDestinations(createDestinationsConfig(pid, properties.get("inboundDestinationsRef")));
        }
        if (StringUtils.hasText(properties.get("outboundDestinationsRef"))) {
            bridgeConnector.setOutboundDestinations(createDestinationsConfig(pid, properties.get("outboundDestinationsRef")));
        }

        bridgeConnector.setLocalBrokerConfig(createBrokerConfig(pid, "localBroker", properties));
        bridgeConnector.setExportedBrokerConfig(createBrokerConfig(pid, "exportedBroker", properties));

        bridgeConnector.setApplicationContext(createApplicationContext(pid));

        try {
            bridgeConnector.afterPropertiesSet();
            bridgeConnector.start();
        } catch (Exception e) {
            String msg = "Error starting bridge " + pid + " : " + e.getMessage();
            LOG.error(msg);
            throw new ConfigurationException("Start", msg, e);
        }

        return bridgeConnector;

    }

}
