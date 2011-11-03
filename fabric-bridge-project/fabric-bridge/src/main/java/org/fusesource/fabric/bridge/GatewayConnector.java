/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.bridge;

import java.rmi.Remote;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.*;

import org.fusesource.fabric.bridge.internal.AbstractConnector;
import org.fusesource.fabric.bridge.internal.SourceConnector;
import org.fusesource.fabric.bridge.internal.TargetConnector;
import org.fusesource.fabric.bridge.model.BridgeDestinationsConfig;
import org.fusesource.fabric.bridge.model.BridgedDestination;
import org.fusesource.fabric.bridge.model.BrokerConfig;
import org.fusesource.fabric.bridge.model.RemoteBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.JmsException;
import org.springframework.jms.UncategorizedJmsException;

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

	private static final Logger LOG = LoggerFactory.getLogger(GatewayConnector.class);

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
	
	private TargetConnector inboundConnector;

	@Override
	public void afterPropertiesSet() throws Exception {
		if (localBrokerConfig == null) {
			throw new IllegalArgumentException("Property local-broker must be set");
		}
		if (inboundDestinations == null && outboundDestinations == null) {
			throw new IllegalArgumentException("At least one of inbound-destinations or outbound-destinations must be set");
		}
	}

	@Override
	protected void doInitialize() {
		// create the inbound connector shared by all bridgeconnectors connected to this gateway
		if (inboundDestinations != null) {
			createInboundConnector();
		}
		
		// create outbound connectors for outboundBrokers
		if (outboundDestinations != null) {
			createOutboundConnectors();
		}

		LOG.info("Initialized");
	}

	private void createInboundConnector() {
		inboundConnector = new TargetConnector();
		inboundConnector.setAutoStartup(isAutoStartup());
		inboundConnector.setPhase(getPhase());
	
		inboundConnector.setLocalBrokerConfig(localBrokerConfig);
		inboundConnector.setInboundDestinations(inboundDestinations);
		try {
			inboundConnector.afterPropertiesSet();
		} catch (Exception e) {
			String msg = "Error creating inbound connector: " + e.getMessage();
			LOG.error(msg, e);
			throw new IllegalArgumentException(msg, e);
		}
	}

	private void createOutboundConnectors() {
		for (RemoteBridge remoteBridge : remoteBridges) {
			SourceConnector outboundConnector = createOutboundConnector(remoteBridge);
			outboundConnectors.put(remoteBridge, outboundConnector);
		}
	}

	private SourceConnector createOutboundConnector(
			RemoteBridge remoteBridge) {
		SourceConnector outboundConnector = new SourceConnector();
		outboundConnector.setAutoStartup(isAutoStartup());
		outboundConnector.setPhase(getPhase());
		
		outboundConnector.setLocalBrokerConfig(localBrokerConfig);
		outboundConnector.setRemoteBrokerConfig(remoteBridge.getRemoteBrokerConfig());
		if (remoteBridge.getOutboundDestinations() == null) {
			// use default destinations
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
		if (inboundConnector != null) {
			// start the shared inbound connector
			try {
				inboundConnector.start();
			} catch (Exception e) {
				String msg = "Error starting inbound connector : " + e.getMessage();
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
		if (inboundConnector != null) {
			// stop the shared inbound connector
			try {
				inboundConnector.stop();
			} catch (Exception e) {
				String msg = "Error stopping inbound connector : " + e.getMessage();
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
		if (inboundConnector != null) {
			// destroy the shared inbound connector
			try {
				inboundConnector.destroy();
			} catch (Exception e) {
				String msg = "Error destroying inbound connector : " + e.getMessage();
				LOG.error(msg, e);
				throw new IllegalStateException(msg, e);
			}
            inboundConnector = null;
		}
		
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

            // create connector if initialized
            if (isInitialized()) {

                // check if the bridge needs a connector or not
                if ((remoteBridge.getOutboundDestinations() != null) ||
                    (this.outboundDestinations == null)) {

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

            // remove and destroy connector if initialized
            if (isInitialized()) {
                SourceConnector outboundConnector = outboundConnectors.remove(remoteBridge);
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
