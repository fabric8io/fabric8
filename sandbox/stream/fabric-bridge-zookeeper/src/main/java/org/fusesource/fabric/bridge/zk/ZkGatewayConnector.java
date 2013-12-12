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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import io.fabric8.api.Container;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.bridge.GatewayConnector;
import io.fabric8.bridge.internal.SourceConnector;
import io.fabric8.bridge.internal.TargetConnector;
import io.fabric8.bridge.model.BrokerConfig;
import io.fabric8.bridge.model.RemoteBridge;
import io.fabric8.bridge.zk.internal.ZkConfigHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class ZkGatewayConnector extends GatewayConnector implements Runnable, ConnectionStateListener {

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
    private String zooKeeperRef;

    private CuratorFramework curator;

    @XmlAttribute(required = true)
    private String fabricServiceRef;

    private FabricService fabricService;

    // interval in seconds between zk lookup to update connected bridges
	// default is 10 seconds
	@XmlAttribute
	private long updateInterval = 10L;

	private Profile gatewayProfile;

	private ScheduledExecutorService bridgeLookupExecutor;

	private Map<String, RemoteBridge> containerBridgeMap = new HashMap<String, RemoteBridge>();
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
			throw new IllegalArgumentException("Property profileName must be set");
		}
        if (curator == null) {
            throw new IllegalArgumentException("Property curator must be set");
        }
		if (fabricService == null) {
			throw new IllegalArgumentException("Property fabricService must be set");
		}

        // configure self as a lifecycle listener
        curator.getConnectionStateListenable().addListener(this);
        this.connected = true;

        // create and register a RemoteBridge for this gateway
		RemoteBridge remoteBridge = new RemoteBridge();
		remoteBridge.setRemoteBrokerConfig(exportedBrokerConfig != null ? exportedBrokerConfig : super.getLocalBrokerConfig());
        remoteBridge.setInboundDestinations(super.getInboundDestinations());
        remoteBridge.setOutboundDestinations(super.getOutboundDestinations());

		// find or create a profile
        if (versionName == null) {
            versionName = fabricService.getDefaultVersion().getId();
        }
		LOG.info("Looking for profile " + profileName + " under version " + versionName);
		Version version = fabricService.getVersion(versionName);
		gatewayProfile = version.getProfile(profileName);

		if (gatewayProfile == null) {
			LOG.info("Creating profile " + profileName + " under version " + versionName);
			gatewayProfile = version.createProfile(profileName);
            // TODO get the default profile and make it the parent
		}

		LOG.info("Registering gateway under profile " + gatewayProfile);
		ZkConfigHelper.registerGateway(gatewayProfile, remoteBridge);

	}

    protected void doStart() {
        super.doStart();

        // start the bridge container lookup executor
        bridgeLookupExecutor = Executors.newSingleThreadScheduledExecutor();
        bridgeLookupExecutor.scheduleWithFixedDelay(this, 0, updateInterval, TimeUnit.SECONDS);

        LOG.info("Started");
    }

    protected void doStop() {
        // stop the bridge container lookup executor
        bridgeLookupExecutor.shutdown();

        // de-register self as a lifecycle listener
        if (this.connected) {
            try {
                curator.getConnectionStateListenable().removeListener(this);
                this.connected = false;
            } catch (Exception e) {
                LOG.error("Error removing Gateway Connector as ZooKeeper listener: " + e.getMessage(), e);
            }
        }

        super.doStop();
        LOG.info("Stopped");
    }

    /**
	 * Lookup containers using the bridge profile and update gateway configuration.
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

        Container[] containers;
        try {
            containers = gatewayProfile.getAssociatedContainers();
        } catch (FabricException e) {
            String msg = "Error getting Containers from Fabric: " + e.getMessage();
            LOG.error(msg, e);
            throw new IllegalStateException(msg, e);
        }

        if (containers.length == 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No BridgeConnector containers found");
            }
        }

		for (Container container : containers) {

			final String containerId = container.getId();
			try {
                // get the Bridge configuration for this container
                RemoteBridge remoteBridge = ZkConfigHelper.getBridgeConfig(
                        curator, container, applicationContext);

                // check if we have an existing Bridge for this container
                RemoteBridge oldRemoteBridge = containerBridgeMap.get(containerId);

                if (remoteBridge != null) {
					if (oldRemoteBridge != null) {
						// check if the configuration has changed
						if (!oldRemoteBridge.equals(remoteBridge)) {
                            LOG.info("Refreshing outbound connector for " + containerId);

							// replace the old remote bridge with the new one
							containerBridgeMap.remove(containerId);
							removeRemoteBridge(oldRemoteBridge);

							addRemoteBridge(remoteBridge);
							containerBridgeMap.put(containerId, remoteBridge);

						}
					} else {
						// add the new bridge to this gateway
						try {

							LOG.info("Found Bridge Configuration for " + containerId);
							addRemoteBridge(remoteBridge);

							containerBridgeMap.put(containerId, remoteBridge);
							LOG.info("Added outbound connector for " + containerId);

						} catch (Exception ex) {
							LOG.error("Error adding outbound conncetor for [" + containerId + "] : " + ex.getMessage(), ex);
						}
					}

				} else {
                    if (oldRemoteBridge != null) {
                        LOG.info("Removing outbound connector for " + containerId);

                        // BridgeConnector went away, remove the old remote bridge
                        containerBridgeMap.remove(containerId);
                        removeRemoteBridge(oldRemoteBridge);
                    } else {
                        // this must be a new BridgeConnector coming up
                        LOG.warn("Container " + containerId +
                            " uses Profile [" + gatewayProfile.getId() +
                            "] but has not yet registered its Bridge Configuration");
                    }
                }
            } catch (Exception ex) {
				LOG.error ("Error getting Bridge Configuration for container [" + containerId + "]: " + ex.getMessage(), ex);
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
        LOG.info("Gateway connected to Fabric Zookeeper service");
        this.connected = true;
    }

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
