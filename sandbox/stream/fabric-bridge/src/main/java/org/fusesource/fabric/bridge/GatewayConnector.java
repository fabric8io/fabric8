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
package io.fabric8.bridge;

import io.fabric8.bridge.internal.AbstractConnector;
import io.fabric8.bridge.internal.SourceConnector;
import io.fabric8.bridge.internal.TargetConnector;
import io.fabric8.bridge.model.BridgeDestinationsConfig;
import io.fabric8.bridge.model.BridgedDestination;
import io.fabric8.bridge.model.BrokerConfig;
import io.fabric8.bridge.model.RemoteBridge;
import org.springframework.jms.JmsException;
import org.springframework.jms.UncategorizedJmsException;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Connects {@link BridgeConnector BridgeConnectors} on remote brokers to a
 * {@link TargetConnector} on local broker. Also creates {@link SourceConnector
 * SourceConnectors} for every {@link BridgeConnector} based on its
 * {@link RemoteBridge}.
 * 
 * @author Dhiraj Bokde
 * 
 */
@XmlRootElement(name="gateway-connector")
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"localBrokerConfig", "inboundDestinations", "outboundDestinations", "remoteBridges"})
public class GatewayConnector extends AbstractConnector {

	@XmlElement(name="local-broker", required=true)
	private BrokerConfig localBrokerConfig;

	@XmlElement(name="inbound-destinations")
	private BridgeDestinationsConfig inboundDestinations;
	
	// shared by all bridge connectors connected to this gateway
	@XmlElement(name="outbound-destinations")
	private BridgeDestinationsConfig outboundDestinations;

    @XmlAttribute
    private String inboundDestinationsRef;

    // shared by all bridge connectors connected to this gateway
    @XmlAttribute
    private String outboundDestinationsRef;

	// default is an empty list of brokers to be added later using addBrokerConfig
	@XmlElement(name="remote-bridge")
	private List<RemoteBridge> remoteBridges = new ArrayList<RemoteBridge>();

	private final Map<RemoteBridge, SourceConnector> outboundConnectors = new HashMap<RemoteBridge, SourceConnector>();
	
	private TargetConnector defaultInboundConnector;

    private final Map<RemoteBridge, TargetConnector> inboundConnectors = new HashMap<RemoteBridge, TargetConnector>();

	@Override
	public void afterPropertiesSet() throws Exception {
		if (localBrokerConfig == null) {
			throw new IllegalArgumentException("Property local-broker must be set");
		}
		if (inboundDestinations == null && outboundDestinations == null) {
			throw new IllegalArgumentException(
                "At least one of inbound-destinations or outbound-destinations must be set");
		}
	}

    @Override
	protected void doInitialize() {
		// create the inbound connector shared by all bridgeconnectors connected to this gateway
		if (inboundDestinations != null && inboundDestinations.isUseStagingQueue()) {
			createDefaultInboundConnector();
        } else if (inboundDestinations != null && !inboundDestinations.isUseStagingQueue()) {
            LOG.info("Inbound connector NOT created for default inbound destinations since staging queue is NOT used");
		} else {
            LOG.info("Inbound connector NOT created since default inbound destinations are NOT specified");
        }
		
		// create outbound connectors for outboundBrokers
		if (remoteBridges != null && !remoteBridges.isEmpty()) {
			createRemoteBridgeConnectors();
		}

		LOG.info("Initialized");
	}

	private void createDefaultInboundConnector() {
		defaultInboundConnector = new TargetConnector();
		defaultInboundConnector.setAutoStartup(isAutoStartup());
		defaultInboundConnector.setPhase(getPhase());
	
		defaultInboundConnector.setLocalBrokerConfig(localBrokerConfig);
		defaultInboundConnector.setInboundDestinations(inboundDestinations);
		try {
			defaultInboundConnector.afterPropertiesSet();
		} catch (Exception e) {
			String msg = "Error creating inbound connector: " + e.getMessage();
			LOG.error(msg, e);
			throw new IllegalArgumentException(msg, e);
		}
	}

	private void createRemoteBridgeConnectors() {
		for (RemoteBridge remoteBridge : remoteBridges) {

            // does the remote bridge use custom inbound destinations??
            // TODO maybe this should only check for staging queue name or location
            if (remoteBridge.getInboundDestinations() != null &&
                !remoteBridge.getInboundDestinations().equals(inboundDestinations)
                && remoteBridge.getInboundDestinations().isUseStagingQueue()) {
                TargetConnector inboundConnector = createInboundConnector(remoteBridge);
                inboundConnectors.put(remoteBridge, inboundConnector);
            } else {
                String reason;
                if (inboundDestinations == null) {
                    reason = ", since it is Unidirectional";
                } else {
                    reason = inboundDestinations.isUseStagingQueue() ?
                        ", since it uses default inbound connector" : ", since staging queue is NOT used";
                }
                LOG.warn("Remote bridge " + remoteBridge + " does NOT require inbound connector" +
                    reason);
            }

            if ((remoteBridge.getOutboundDestinations() != null) ||
                (this.outboundDestinations != null)) {
                SourceConnector outboundConnector = createOutboundConnector(remoteBridge);
                outboundConnectors.put(remoteBridge, outboundConnector);
            }

		}
	}

    private TargetConnector createInboundConnector(RemoteBridge remoteBridge) {
        TargetConnector inboundConnector = new TargetConnector();
        inboundConnector.setId(getId() + "." + remoteBridge.getId() + ".inboundConnector");
        inboundConnector.setAutoStartup(isAutoStartup());
        inboundConnector.setPhase(getPhase());

        inboundConnector.setLocalBrokerConfig(localBrokerConfig);
        // check if the remote broker config should be used
        if (!remoteBridge.getInboundDestinations().isDefaultStagingLocation()) {
            inboundConnector.setRemoteBrokerConfig(remoteBridge.getRemoteBrokerConfig());
        }
        inboundConnector.setInboundDestinations(remoteBridge.getInboundDestinations());

        try {
            inboundConnector.afterPropertiesSet();
        } catch (Exception e) {
            String msg = "Error creating inbound connector for " + remoteBridge + " : " + e.getMessage();
            LOG.error(msg, e);
            throw new IllegalArgumentException(msg, e);
        }
        return inboundConnector;
    }

    private SourceConnector createOutboundConnector(
			RemoteBridge remoteBridge) {
		SourceConnector outboundConnector = new SourceConnector();
        outboundConnector.setId(getId() + "." + remoteBridge.getId() + ".outboundConnector");
		outboundConnector.setAutoStartup(isAutoStartup());
		outboundConnector.setPhase(getPhase());
		
		outboundConnector.setLocalBrokerConfig(localBrokerConfig);
		outboundConnector.setRemoteBrokerConfig(remoteBridge.getRemoteBrokerConfig());
		if (remoteBridge.getOutboundDestinations() == null) {
			// use default destinations
            LOG.warn("Remote bridge " + remoteBridge + " does not specify outbound destinations" +
                (outboundDestinations == null ? ", it is unidirectional!" :
                    ", will load balance messages from default outbound destinations!"));
			outboundConnector.setOutboundDestinations(outboundDestinations);
		} else {
			// completely override outbound destinations for this bridge
			outboundConnector.setOutboundDestinations(remoteBridge.getOutboundDestinations());
		}
		
		try {
			outboundConnector.afterPropertiesSet();
		} catch (Exception e) {
			String msg = "Error creating outbound connector for " + remoteBridge + " : " + e.getMessage();
			LOG.error(msg, e);
			throw new IllegalArgumentException(msg, e);
		}
		return outboundConnector;
	}

	@Override
	protected void doStart() {
		if (defaultInboundConnector != null) {
			// start the shared inbound connector
			try {
				defaultInboundConnector.start();
			} catch (Exception e) {
				String msg = "Error starting inbound connector : " + e.getMessage();
				LOG.error(msg, e);
				throw new IllegalStateException(msg, e);
			}
		}

		// start inbound connectors
		for (Entry<RemoteBridge, TargetConnector> entry : inboundConnectors.entrySet()) {
			RemoteBridge remoteBridge = entry.getKey();
			try {
				entry.getValue().start();
			} catch (Exception e) {
				String msg = "Error starting inbound connector for " + remoteBridge + " : " + e.getMessage();
				LOG.error(msg, e);
				throw new IllegalStateException(msg, e);
			}
		}

		// start outbound connectors
		for (Entry<RemoteBridge, SourceConnector> entry : outboundConnectors.entrySet()) {
			RemoteBridge remoteBridge = entry.getKey();
			try {
				entry.getValue().start();
			} catch (Exception e) {
				String msg = "Error starting outbound connector for " + remoteBridge + " : " + e.getMessage();
				LOG.error(msg, e);
				throw new IllegalStateException(msg, e);
			}
		}

		LOG.info("Started");
	}

	@Override
	protected void doStop() {
		if (defaultInboundConnector != null) {
			// stop the shared inbound connector
			try {
				defaultInboundConnector.stop();
			} catch (Exception e) {
				String msg = "Error stopping inbound connector : " + e.getMessage();
				LOG.error(msg, e);
				throw new IllegalStateException(msg, e);
			}
		}
		
		// stop inbound connectors
		for (Entry<RemoteBridge, TargetConnector> entry : inboundConnectors.entrySet()) {
			RemoteBridge remoteBridge = entry.getKey();
			try {
				entry.getValue().stop();
			} catch (Exception e) {
				String msg = "Error stopping inbound connector for " + remoteBridge + " : " + e.getMessage();
				LOG.error(msg, e);
				throw new IllegalStateException(msg, e);
			}
		}

		// stop outbound connectors
		for (Entry<RemoteBridge, SourceConnector> entry : outboundConnectors.entrySet()) {
			RemoteBridge remoteBridge = entry.getKey();
			try {
				entry.getValue().stop();
			} catch (Exception e) {
				String msg = "Error stopping outbound connector for " + remoteBridge + " : " + e.getMessage();
				LOG.error(msg, e);
				throw new IllegalStateException(msg, e);
			}
		}

		LOG.info("Stopped");
	}

	@Override
	protected void doDestroy() throws Exception {
		if (defaultInboundConnector != null) {
			// destroy the shared inbound connector
			try {
				defaultInboundConnector.destroy();
			} catch (Exception e) {
				String msg = "Error destroying inbound connector : " + e.getMessage();
				LOG.error(msg, e);
				throw new IllegalStateException(msg, e);
			}
            defaultInboundConnector = null;
		}
		
		// destroy inbound connectors
		for (Entry<RemoteBridge, TargetConnector> entry : inboundConnectors.entrySet()) {
			RemoteBridge remoteBridge = entry.getKey();
			try {
				entry.getValue().destroy();
			} catch (Exception e) {
				String msg = "Error destroying inbound connector for " + remoteBridge + " : " + e.getMessage();
				LOG.error(msg, e);
				throw new IllegalStateException(msg, e);
			}
		}
        inboundConnectors.clear();

		// destroy outbound connectors
		for (Entry<RemoteBridge, SourceConnector> entry : outboundConnectors.entrySet()) {
			RemoteBridge remoteBridge = entry.getKey();
			try {
				entry.getValue().destroy();
			} catch (Exception e) {
				String msg = "Error destroying outbound connector for " + remoteBridge + " : " + e.getMessage();
				LOG.error(msg, e);
				throw new IllegalStateException(msg, e);
			}
		}
        outboundConnectors.clear();

		LOG.info("Destroyed");
	}

	@Override
	public BridgeDestinationsConfig getDestinationsConfig() throws JmsException {
		synchronized (lifecycleMonitor) {
			return outboundDestinations;
		}
	}

	@Override
	public void setDestinationsConfig(
			BridgeDestinationsConfig destinationsConfig) throws JmsException {
		synchronized (lifecycleMonitor) {
	
			if (destinationsConfig == null || destinationsConfig.getDestinations() == null) {
				throw new UncategorizedJmsException("Invalid destinations config");
			}
	
			// remember current state
			boolean wasRunning = isRunning();
	
			// destroy and recreate the connector
			try {
				destroy();
			} catch (Exception e) {
				LOG.error("Error destorying connector: " + e.getMessage(), e);
				// ignore and keep going??
			}
	
			this.outboundDestinations = destinationsConfig;
			
			try {
				afterPropertiesSet();
			} catch (Exception e) {
				throw new UncategorizedJmsException(e.getMessage(), e);
			}
			
			if (wasRunning) {
				start();
			}
			
			LOG.info("Outbound destinations set to " + destinationsConfig);
		}
	}

	@Override
	public void addDestinations(List<BridgedDestination> destinations)
			throws JmsException {
		synchronized (lifecycleMonitor) {
			
			if (destinations == null || destinations.isEmpty()) {
				throw new IllegalArgumentException("Null or empty destinations");
			}
			
			boolean updated = false;
			for (SourceConnector connector : outboundConnectors.values()) {
				connector.addDestinations(destinations);
				if (!updated) {
					outboundDestinations = connector.getOutboundDestinations();
					updated = true;
				}
			}
		}
	}

	@Override
	public void removeDestinations(List<BridgedDestination> destinations)
			throws JmsException {
		synchronized (lifecycleMonitor) {
			
			if (destinations == null || destinations.isEmpty()) {
				throw new IllegalArgumentException("Null or empty destinations");
			}
			
			boolean updated = false;
			for (SourceConnector connector : outboundConnectors.values()) {
				connector.removeDestinations(destinations);
				if (!updated) {
					outboundDestinations = connector.getOutboundDestinations();
					updated = true;
				}
			}
		}
	}

	public void addRemoteBridge(RemoteBridge remoteBridge) {
		synchronized (lifecycleMonitor) {

			if (remoteBridges.contains(remoteBridge)) {
				String msg = "Remote bridge " + remoteBridge + " already exists in Gateway";
				LOG.error(msg);
                throw new UncategorizedJmsException(msg);
            }

            remoteBridges.add(remoteBridge);

            // create connectors if initialized
            if (isInitialized()) {

                // check if the bridge needs an inbound connector or not
                if ((remoteBridge.getInboundDestinations() != null) &&
                    !remoteBridge.getInboundDestinations().equals(inboundDestinations)) {

                    TargetConnector inboundConnector;
                    try {
                        // create inbound connector
                        inboundConnector = createInboundConnector(remoteBridge);
                    } catch (Exception e) {
                        String msg = "Error creating inbound connector for " + remoteBridge + " : " + e.getMessage();
                        LOG.error(msg, e);
                        throw new UncategorizedJmsException(msg , e);
                    }

                    inboundConnectors.put(remoteBridge, inboundConnector);

                    // start connector if running
                    if (isRunning()) {
                        try {
                            inboundConnector.start();
                        } catch (Exception e) {
                            String msg = "Error starting inbound connector for " + remoteBridge + " : " + e.getMessage();
                            LOG.error(msg, e);
                            throw new UncategorizedJmsException(msg , e);
                        }
                    }
                } else {
                    LOG.warn("No custom inbound destinations in Bridge, " +
                        "no inbound connector will be created for " + remoteBridge);
                }

                // check if the bridge needs an outbound connector or not
                if ((remoteBridge.getOutboundDestinations() != null) ||
                    (this.outboundDestinations != null)) {

                    SourceConnector outboundConnector;
                    try {
                        // create outbound connector
                        outboundConnector = createOutboundConnector(remoteBridge);
                    } catch (Exception e) {
                        String msg = "Error creating outbound connector for " + remoteBridge + " : " + e.getMessage();
                        LOG.error(msg, e);
                        throw new UncategorizedJmsException(msg , e);
                    }

                    outboundConnectors.put(remoteBridge, outboundConnector);

                    // start connector if running
                    if (isRunning()) {
                        try {
                            outboundConnector.start();
                        } catch (Exception e) {
                            String msg = "Error starting outbound connector for " + remoteBridge + " : " + e.getMessage();
                            LOG.error(msg, e);
                            throw new UncategorizedJmsException(msg , e);
                        }
                    }
                } else {
                    LOG.warn("No outbound destinations in Bridge or Gateway, " +
                        "connection is unidirectional so no connector will be created for " + remoteBridge);
                }
            }
		}
	}
	
	public void removeRemoteBridge(RemoteBridge remoteBridge) {
		synchronized (lifecycleMonitor) {
            if (!remoteBridges.remove(remoteBridge)) {
                throw new UncategorizedJmsException("Not found remote bridge " + remoteBridge);
            }

            // remove and destroy connectors if initialized
            if (isInitialized()) {
                TargetConnector inboundConnector = inboundConnectors.remove(remoteBridge);
                if (inboundConnector != null) {
                    try {
                        inboundConnector.destroy();
                    } catch (Exception e) {
                        String msg = "Error removing inbound connector for " + remoteBridge + " : " + e.getMessage();
                        LOG.error(msg, e);
                        throw new UncategorizedJmsException(msg , e);
                    }
                }

                SourceConnector outboundConnector = outboundConnectors.remove(remoteBridge);
                if (outboundConnector != null) {
                    try {
                        outboundConnector.destroy();
                    } catch (Exception e) {
                        String msg = "Error removing outbound connector for " + remoteBridge + " : " + e.getMessage();
                        LOG.error(msg, e);
                        throw new UncategorizedJmsException(msg , e);
                    }
                }
            }
		}
	}
	
	public final BrokerConfig getLocalBrokerConfig() {
		return localBrokerConfig;
	}

	public final void setLocalBrokerConfig(BrokerConfig localBrokerConfig) {
		this.localBrokerConfig = localBrokerConfig;
	}

	public final BridgeDestinationsConfig getInboundDestinations() {
		return inboundDestinations;
	}

	public final void setInboundDestinations(
			BridgeDestinationsConfig inboundDestinations) {
		this.inboundDestinations = inboundDestinations;
	}

	public final BridgeDestinationsConfig getOutboundDestinations() {
		return outboundDestinations;
	}

	public final void setOutboundDestinations(
			BridgeDestinationsConfig outboundDestinations) {
		this.outboundDestinations = outboundDestinations;
	}

	public final List<RemoteBridge> getRemoteBridges() {
		return remoteBridges;
	}

	public final void setRemoteBridges(List<RemoteBridge> remoteBridges) {
		this.remoteBridges = remoteBridges;
	}

    public String getInboundDestinationsRef() {
        return inboundDestinationsRef;
    }

    public void setInboundDestinationsRef(String inboundDestinationsRef) {
        this.inboundDestinationsRef = inboundDestinationsRef;
    }

    public String getOutboundDestinationsRef() {
        return outboundDestinationsRef;
    }

    public void setOutboundDestinationsRef(String outboundDestinationsRef) {
        this.outboundDestinationsRef = outboundDestinationsRef;
    }

}
