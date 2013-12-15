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
package io.fabric8.bridge.zk;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.bridge.BridgeConnector;
import io.fabric8.bridge.model.BrokerConfig;
import io.fabric8.bridge.model.RemoteBridge;
import io.fabric8.bridge.zk.internal.ZkConfigHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextAware;

/**
 * Bi-directional connector for network bridges. Creates a bridge connected to a {@link ZkGatewayConnector} . 
 * Creates a {@link BridgeConnector} from the {@link RemoteBridge} registered in ZK by the {@link ZkGatewayConnector}. 
 *  
 * @author Dhiraj Bokde
 *
 */
@XmlRootElement(name="zkbridge-connector")
@XmlAccessorType(XmlAccessType.NONE)
public class ZkBridgeConnector extends BridgeConnector implements ConnectionStateListener, ApplicationContextAware {
	
	private static final Logger LOG = LoggerFactory.getLogger(ZkBridgeConnector.class);

	private static final String BRIDGE_PID = "io.fabric8.bridge";

    // a different broker configuration for remote ZkGatewayConnectors
	// for example, the local config may use vm transport, but it can't be exported to external ZkGatewayConnectors
	@XmlElement(name="exported-broker")
	private BrokerConfig exportedBrokerConfig;

	// version and name of the profile where the gateway has registered its bridge config
	@XmlAttribute
	private String versionName;

    @XmlAttribute(required = true)
    private String gatewayProfileName;

    @XmlAttribute
    private int gatewayStartupDelay = 10;

    @XmlAttribute
    private int gatewayConnectRetries = 5;

    @XmlAttribute(required = true)
    private String zooKeeperRef;

    private CuratorFramework curator;

    @XmlAttribute(required = true)
    private String fabricServiceRef;

    private FabricService fabricService;

    private transient boolean connected;

    private Profile gatewayProfile;

    private Container container;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (gatewayProfileName == null) {
			throw new IllegalArgumentException("Property profile must be set");
		}
        if (curator == null) {
            throw new IllegalArgumentException("Property curator must be set");
        }
        if (fabricService == null) {
            throw new IllegalArgumentException("Property fabricService must be set");
        }

        // configure self as a lifecycle listener
        this.connected = true;

        // validate properties
        if (exportedBrokerConfig == null) {
            LOG.warn("The property exportedBrokerConfig is not set, exporting property localBrokerConfig");
        } else if ((exportedBrokerConfig.getBrokerUrl() == null && exportedBrokerConfig.getConnectionFactory() == null) ||
            (exportedBrokerConfig.getBrokerUrl() != null && exportedBrokerConfig.getConnectionFactory() != null)) {
            throw new IllegalArgumentException("Either a exported broker url or connection factory must be provided");
        }

        // get gateway profile
        if (versionName == null) {
            versionName = fabricService.getDefaultVersion().getId();
        }
		LOG.info("Looking for profile " + gatewayProfileName + " under version " + versionName);
		Version version = fabricService.getVersion(versionName);
		gatewayProfile = version.getProfile(gatewayProfileName);
		if (gatewayProfile == null) {
			throw new IllegalArgumentException("Gateway connector profile " + gatewayProfileName + " does not exist");
		}

        // get configuration bits from zk to populate connector properties
        RemoteBridge gatewayBridge;
        int attempts = gatewayConnectRetries;
        do {
            gatewayBridge = ZkConfigHelper.getGatewayConfig(gatewayProfile, applicationContext);
            if (gatewayBridge == null) {
                LOG.warn("Gateway configuration not found in profile " + gatewayProfileName +
                    ", waiting for " + gatewayStartupDelay + " seconds, retries remaining " + attempts);
                Thread.sleep(gatewayStartupDelay * 1000L);
            }
        } while (--attempts > 0);
        if (gatewayBridge == null) {
            String msg = "Gateway configuration not found in profile " + gatewayProfileName;
            LOG.error(msg);
            throw new IllegalStateException(msg);
        }

        // populate container config with bridge config
        super.setRemoteBrokerConfig(gatewayBridge.getRemoteBrokerConfig());
        // set bridge inbound destinations from either the bridge or the gateway
        if (this.getInboundDestinations() != null) {
            if (gatewayBridge.getOutboundDestinations() == null) {
                LOG.info("Using inbound destinations from Bridge, " +
                    "Gateway has no default destinations");
            } else {
                LOG.warn("Using inbound destinations from Bridge, " +
                    "Gateway default destinations will be ignored");
            }
        } else {
            if (gatewayBridge.getOutboundDestinations() != null) {
                LOG.info("No inbound destinations in Bridge, " +
                    "Gateway destinations will be used");
            } else {
                LOG.warn("No inbound destinations in Bridge or Gateway, " +
                    "Bridge will be unidirectional from Bridge To Gateway");
            }
        }
        super.setInboundDestinations(this.getInboundDestinations() != null ?
            this.getInboundDestinations() : gatewayBridge.getOutboundDestinations());

        super.afterPropertiesSet();
    }

    @Override
    protected void doInitialize() {
        super.doInitialize();

        // register the bridge in Zookeeper
        container = fabricService.getContainer(System.getProperty("karaf.name"));

        RemoteBridge remoteBridge = new RemoteBridge();
        remoteBridge.setRemoteBrokerConfig(this.exportedBrokerConfig != null ?
            this.exportedBrokerConfig : super.getLocalBrokerConfig());

        // set the Bridge outbound destinations as the remote Gateway inbound destinations
        remoteBridge.setInboundDestinations(super.getOutboundDestinations());
        // set the Bridge inbound destinations as the remote Gateway outbound destinations
        remoteBridge.setOutboundDestinations(super.getInboundDestinations());

        ZkConfigHelper.registerBridge(curator, container, remoteBridge);
    }

    protected void doStop() {
        // de-register self as a lifecycle listener
        if (this.connected) {
            try {
                this.connected = false;
            } catch (Exception e) {
                LOG.error("Error removing Bridge Connector as ZooKeeper listener: " + e.getMessage(), e);
            }
        }

        super.doStop();
        LOG.info("Stopped");
    }

    @Override
    protected void doDestroy() throws Exception {
        try {
            super.doDestroy();
        } catch (Exception e) {
            LOG.warn("Error destroying Bridge: " + e.getMessage(), e);
        }

        // remove the bridge from ZK
        if (container != null) {
            if (this.connected) {
                ZkConfigHelper.removeBridge(curator, container);
            } else {
                LOG.error("Bridge disconnected from Fabric Zookeeper service, " +
                    "unable to remove Bridge runtime configuration");
            }
        }
    }

    @Override
    public void stateChanged(CuratorFramework client, ConnectionState newState) {
        switch (newState) {
            case CONNECTED:
            case RECONNECTED:
                this.curator = client;
                onConnected();
                break;
            default:
                onDisconnected();
        }
    }

    public void onConnected() {
        LOG.info("Bridge connected to Fabric Zookeeper service");
        this.connected = true;
    }

    public void onDisconnected() {
        LOG.warn("Bridge disconnected from Fabric Zookeeper service");
        this.connected = false;
    }

    public BrokerConfig getExportedBrokerConfig() {
        return exportedBrokerConfig;
    }

    public void setExportedBrokerConfig(BrokerConfig exportedBrokerConfig) {
        this.exportedBrokerConfig = exportedBrokerConfig;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getGatewayProfileName() {
        return gatewayProfileName;
    }

    /**
     * Set the name of the profile to use for looking up gateway configuration.
     * If set to default <code>null</code>, all profiles are searched.
     *
     * @param gatewayProfileName
     */
    public void setGatewayProfileName(String gatewayProfileName) {
        this.gatewayProfileName = gatewayProfileName;
    }

    public String getZooKeeperRef() {
        return zooKeeperRef;
    }

    public void setZooKeeperRef(String zooKeeperRef) {
        this.zooKeeperRef = zooKeeperRef;
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public String getFabricServiceRef() {
        return fabricServiceRef;
    }

    public void setFabricServiceRef(String fabricServiceRef) {
        this.fabricServiceRef = fabricServiceRef;
    }

    public int getGatewayStartupDelay() {
        return gatewayStartupDelay;
    }

    public void setGatewayStartupDelay(int gatewayStartupDelay) {
        this.gatewayStartupDelay = gatewayStartupDelay;
    }

    public int getGatewayConnectRetries() {
        return gatewayConnectRetries;
    }

    public void setGatewayConnectRetries(int gatewayConnectRetries) {
        this.gatewayConnectRetries = gatewayConnectRetries;
    }
}
