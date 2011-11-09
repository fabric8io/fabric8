/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.bridge.zk;

import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.bridge.BridgeConnector;
import org.fusesource.fabric.bridge.model.BrokerConfig;
import org.fusesource.fabric.bridge.model.RemoteBridge;
import org.fusesource.fabric.bridge.zk.internal.ZkConfigHelper;
import org.fusesource.fabric.service.FabricServiceImpl;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextAware;

import javax.xml.bind.annotation.*;

/**
 * Bi-directional connector for network bridges. Creates a bridge connected to a {@link ZkGatewayConnector} . 
 * Creates a {@link BridgeConnector} from the {@link RemoteBridge} registered in ZK by the {@link ZkGatewayConnector}. 
 *  
 * @author Dhiraj Bokde
 *
 */
@XmlRootElement(name="zkbridge-connector")
@XmlAccessorType(XmlAccessType.NONE)
public class ZkBridgeConnector extends BridgeConnector implements LifecycleListener, ApplicationContextAware {
	
	private static final Logger LOG = LoggerFactory.getLogger(ZkBridgeConnector.class);

	private static final String BRIDGE_PID = "org.fusesource.fabric.bridge";

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
    private String fabricServiceRef;

    private FabricService fabricService;

    private transient boolean connected;

    private Profile gatewayProfile;

    private Agent agent;

    @Override
    public void afterPropertiesSet() throws Exception {
        // configure self as a lifecycle listener
        IZKClient zooKeeper = ((FabricServiceImpl) fabricService).getZooKeeper();
        zooKeeper.registerListener(this);
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
            versionName = fabricService.getDefaultVersion().getName();
        }
		LOG.info("Looking for profile " + gatewayProfileName + " under version " + versionName);
		Version version = fabricService.getVersion(versionName);
		gatewayProfile = version.getProfile(gatewayProfileName);
		if (gatewayProfile == null) {
			throw new IllegalArgumentException("Gateway connector profile " + gatewayProfileName + " does not exist");
		}

        // get configuration bits from zk to populate connector properties
        RemoteBridge gatewayBridge;
        int attempts = 0;
        do {
            gatewayBridge = ZkConfigHelper.getGatewayConfig(gatewayProfile, applicationContext);
            if (gatewayBridge == null) {
                LOG.warn("Gateway configuration not found in profile " + gatewayProfileName +
                    ", waiting for " + gatewayStartupDelay + " seconds");
                Thread.sleep(gatewayStartupDelay * 1000L);
            }
        } while (++attempts < gatewayConnectRetries);
        if (gatewayBridge == null) {
            String msg = "Gateway configuration not found in profile " + gatewayProfileName;
            LOG.error(msg);
            throw new IllegalStateException(msg);
        }

        // populate agent config with bridge config
        super.setRemoteBrokerConfig(gatewayBridge.getRemoteBrokerConfig());
        // set bridge inbound destinations from either the bridge or the gateway
        if (this.getInboundDestinations() != null) {
            if (gatewayBridge.getOutboundDestinations() != null) {
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
        agent = fabricService.getAgent(System.getProperty("karaf.name"));

        RemoteBridge remoteBridge = new RemoteBridge();
        remoteBridge.setRemoteBrokerConfig(this.exportedBrokerConfig != null ?
            this.exportedBrokerConfig : super.getLocalBrokerConfig());

        // set the Bridge outbound destinations as the remote Gateway inbound destinations
        remoteBridge.setInboundDestinations(super.getOutboundDestinations());
        // set the Bridge inbound destinations as the remote Gateway outbound destinations
        remoteBridge.setOutboundDestinations(super.getInboundDestinations());

        ZkConfigHelper.registerBridge(((FabricServiceImpl)fabricService).getZooKeeper(), agent, remoteBridge);
    }

    @Override
    protected void doDestroy() throws Exception {
        try {
            super.doDestroy();
        } catch (Exception e) {
            LOG.warn("Error destroying Bridge: " + e.getMessage(), e);
        }

        // remove the bridge from ZK
        if (agent != null) {
            ZkConfigHelper.removeBridge(((FabricServiceImpl)fabricService).getZooKeeper(), agent);
        }
    }

    @Override
    public void onConnected() {
        LOG.info("Bridge connected to Fabric Zookeeper service");
        this.connected = true;
    }

    @Override
    public void onDisconnected() {
        LOG.warn("Bridge disconnected from Fabric Zookeeper service");
        this.connected = false;
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
