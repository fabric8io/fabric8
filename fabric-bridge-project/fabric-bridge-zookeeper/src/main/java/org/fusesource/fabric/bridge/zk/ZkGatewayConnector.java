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

import org.fusesource.fabric.api.*;
import org.fusesource.fabric.bridge.GatewayConnector;
import org.fusesource.fabric.bridge.internal.SourceConnector;
import org.fusesource.fabric.bridge.internal.TargetConnector;
import org.fusesource.fabric.bridge.model.BrokerConfig;
import org.fusesource.fabric.bridge.model.RemoteBridge;
import org.fusesource.fabric.bridge.zk.internal.ZkConfigHelper;
import org.fusesource.fabric.service.FabricServiceImpl;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Connects {@link ZkBridgeConnector BridgeConnectors} on remote brokers to a
 * {@link TargetConnector} on local broker. Also creates {@link SourceConnector
 * SourceConnectors} for every {@link ZkBridgeConnector} based on its
 * {@link RemoteBridge}.
 *
 * @author Dhiraj Bokde
 *
 */
@XmlRootElement(name="zkgateway-connector")
@XmlAccessorType(XmlAccessType.NONE)
public class ZkGatewayConnector extends GatewayConnector implements Runnable, LifecycleListener {

	private static final Logger LOG = LoggerFactory.getLogger(ZkGatewayConnector.class);

	// a different broker configuration for remote ZkBridgeConnectors
	// for example, the local config may use vm transport, but it can't be exported to external ZkBridgeConnectors
	@XmlElement(name="exported-broker")
	private BrokerConfig exportedBrokerConfig;

	// version and name of the profile where the gateway should register its bridge config
	@XmlAttribute
	private String versionName;

	@XmlAttribute(required=true)
	private String profileName;

    @XmlAttribute(required = true)
    private String fabricServiceRef;

    private FabricService fabricService;

    // interval in seconds between zk lookup to update connected bridges
	// default is 10 seconds
	@XmlAttribute
	private long updateInterval = 10L;

	private Profile gatewayProfile;

	private ScheduledExecutorService bridgeLookupExecutor;

	private Map<String, RemoteBridge> agentBridgeMap = new HashMap<String, RemoteBridge>();
    private volatile boolean connected;

    @Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		if (exportedBrokerConfig == null) {

			LOG.warn("The property exportedBrokerConfig is not set, exporting property localBrokerConfig");

        } else if ((exportedBrokerConfig.getBrokerUrl() == null && exportedBrokerConfig.getConnectionFactory() == null) ||
            (exportedBrokerConfig.getBrokerUrl() != null && exportedBrokerConfig.getConnectionFactory() != null)) {

            throw new IllegalArgumentException("Either a exported broker url or connection factory must be provided");

        }

        if (profileName == null) {
			throw new IllegalArgumentException("Property profile must be set");
		}

		if (fabricService == null) {
			throw new IllegalArgumentException("Property fabricService must be set");
		}

        // configure self as a lifecycle listener
        IZKClient zooKeeper = ((FabricServiceImpl) fabricService).getZooKeeper();
        zooKeeper.registerListener(this);
        this.connected = true;

        // create and register a RemoteBridge for this gateway
		RemoteBridge remoteBridge = new RemoteBridge();
		remoteBridge.setRemoteBrokerConfig(exportedBrokerConfig != null ? exportedBrokerConfig : super.getLocalBrokerConfig());
		remoteBridge.setOutboundDestinations(super.getOutboundDestinations());

		// find or create a profile
        if (versionName == null) {
            versionName = fabricService.getDefaultVersion().getName();
        }
		LOG.info("Looking for profile " + profileName + " under version " + versionName);
		Version version = fabricService.getVersion(versionName);
		gatewayProfile = version.getProfile(profileName);

		if (gatewayProfile == null) {
			LOG.info("Creating profile " + profileName + " under version " + versionName);
			gatewayProfile = version.createProfile(profileName);
            // get the default profile and make it the parent

		}

		LOG.info("Registering gateway under profile " + gatewayProfile);
		ZkConfigHelper.registerGateway(gatewayProfile, remoteBridge);

	}

    protected void doStart() {
        super.doStart();

        // start the bridge agent lookup executor
        bridgeLookupExecutor = Executors.newSingleThreadScheduledExecutor();
        bridgeLookupExecutor.scheduleWithFixedDelay(this, 0, updateInterval, TimeUnit.SECONDS);

        LOG.info("Started");
    }

    protected void doStop() {
        // stop the bridge agent lookup executor
        bridgeLookupExecutor.shutdownNow();

        // de-register self as a lifecycle listener
        this.connected = false;
        try {
            IZKClient zooKeeper = ((FabricServiceImpl) fabricService).getZooKeeper();
            zooKeeper.removeListener(this);
        } catch (Exception e) {
            LOG.error("Error removing Gateway Connector as ZooKeeper listener: " + e.getMessage(), e);
        }

        super.doStop();
        LOG.info("Stopped");
    }

    /**
	 * Lookup agents using the bridge profile and update gateway configuration.
	 *
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

        if (!connected) {
            LOG.warn("Gateway is not connected to Fabric Zookeeper service, will retry after " +
                this.updateInterval + " seconds");
            return;
        }

        Agent[] agents;
        try {
            agents = gatewayProfile.getAssociatedAgents();
        } catch (FabricException e) {
            String msg = "Error getting Agents from Fabric: " + e.getMessage();
            LOG.error(msg, e);
            throw new IllegalStateException(msg, e);
        }

        if (agents.length == 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No BridgeConnector agents found");
            }
        }

		for (Agent agent : agents) {

			final String agentId = agent.getId();
			try {
                // get the Bridge configuration for this agent
                RemoteBridge remoteBridge = ZkConfigHelper.getBridgeConfig(
                ((FabricServiceImpl) fabricService).getZooKeeper(),
                agent, applicationContext);

                // check if we have an existing Bridge for this agent
                RemoteBridge oldRemoteBridge = agentBridgeMap.get(agentId);

                if (remoteBridge != null) {
					if (oldRemoteBridge != null) {
						// check if the configuration has changed
						if (!oldRemoteBridge.equals(remoteBridge)) {
                            LOG.info("Refreshing outbound connector for " + agentId);

							// replace the old remote bridge with the new one
							agentBridgeMap.remove(agentId);
							removeRemoteBridge(oldRemoteBridge);

							addRemoteBridge(remoteBridge);
							agentBridgeMap.put(agentId, remoteBridge);

						}
					} else {
						// add the new bridge to this gateway
						try {

							LOG.info("Found Bridge Configuration for " + agentId);
							addRemoteBridge(remoteBridge);

							agentBridgeMap.put(agentId, remoteBridge);
							LOG.info("Added outbound connector for " + agentId);
						
						} catch (Exception ex) {
							LOG.error("Error adding outbound conncetor for [" + agentId + "] : " + ex.getMessage(), ex);
						}
					}
	
				} else {
                    if (oldRemoteBridge != null) {
                        LOG.info("Removing outbound connector for " + agentId);

                        // BridgeConnector went away, remove the old remote bridge
                        agentBridgeMap.remove(agentId);
                        removeRemoteBridge(oldRemoteBridge);
                    } else {
                        // this must be a new BridgeConnector coming up
                        LOG.warn("Agent " + agentId +
                            " uses Profile [" + gatewayProfile.getId() +
                            "] but has not yet registered its Bridge Configuration");
                    }
                }
            } catch (Exception ex) {
				LOG.error ("Error getting Bridge Configuration for agent [" + agentId + "]: " + ex.getMessage(), ex);
			}
		
		}
	}

    @Override
    public void onConnected() {
        LOG.info("Gateway connected to Fabric Zookeeper service");
        this.connected = true;
    }

    @Override
    public void onDisconnected() {
        LOG.warn("Gateway disconnected from Fabric Zookeeper service");
        this.connected = false;
    }

	public void setExportedBrokerConfig(BrokerConfig exportedBrokerConfig) {
		this.exportedBrokerConfig = exportedBrokerConfig;
	}

	public BrokerConfig getExportedBrokerConfig() {
		return exportedBrokerConfig;
	}

	public String getVersionName() {
		return versionName;
	}

	public void setVersionName(String versionName) {
		this.versionName = versionName;
	}

	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

    public String getFabricServiceRef() {
        return fabricServiceRef;
    }

    public void setFabricServiceRef(String fabricServiceRef) {
        this.fabricServiceRef = fabricServiceRef;
    }

    public final FabricService getFabricService() {
        return fabricService;
    }

    public final void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

}
