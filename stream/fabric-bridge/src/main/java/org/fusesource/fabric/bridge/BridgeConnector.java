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

import java.util.List;

import javax.xml.bind.annotation.*;

import org.fusesource.fabric.bridge.internal.AbstractConnector;
import org.fusesource.fabric.bridge.internal.SourceConnector;
import org.fusesource.fabric.bridge.internal.TargetConnector;
import org.fusesource.fabric.bridge.model.BridgeDestinationsConfig;
import org.fusesource.fabric.bridge.model.BridgedDestination;
import org.fusesource.fabric.bridge.model.BrokerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Bi-directional connector for network bridges. Connects a source broker to a target broker. 
 * Its a thin wrapper around {@link SourceConnector} and {@link TargetConnector}. 
 *  
 * @author Dhiraj Bokde
 *
 */
@XmlRootElement(name="bridge-connector")
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"localBrokerConfig", "remoteBrokerConfig", "outboundDestinations", "inboundDestinations"})
public class BridgeConnector extends AbstractConnector {
	
	private static final Logger LOG = LoggerFactory.getLogger(BridgeConnector.class);

	// required
	@XmlElement(name="local-broker", required=true)
	private BrokerConfig localBrokerConfig;

	// may be null if outbound staging queue is local
	@XmlElement(name="remote-broker")
	private BrokerConfig remoteBrokerConfig;
	
	// may be null for inbound only connector
	@XmlElement(name="outbound-destinations")
	private BridgeDestinationsConfig outboundDestinations;
	
	// may be null for outbound only connector
	@XmlElement(name="inbound-destinations")
	private BridgeDestinationsConfig inboundDestinations;

    // may be null for inbound only connector
	@XmlAttribute
	private String outboundDestinationsRef;

	// may be null for outbound only connector
	@XmlAttribute
	private String inboundDestinationsRef;

	private SourceConnector outboundConnector;

	private TargetConnector inboundConnector;

	@Override
	protected void doInitialize() {
		// create source and target connector
		createSourceConnector();
		createTargetConnector();
		LOG.info("Initialized");
	}

	private void createSourceConnector() {
		// do we need an outbound connector
		if (outboundDestinations != null) {
			outboundConnector = new SourceConnector();
			outboundConnector.setAutoStartup(isAutoStartup());
			outboundConnector.setPhase(getPhase());
			
			outboundConnector.setOutboundDestinations(outboundDestinations);
			outboundConnector.setLocalBrokerConfig(localBrokerConfig);
			
			// check if the staging queue is on the remote broker
			if (outboundDestinations.isDefaultStagingLocation()) {
				outboundConnector.setRemoteBrokerConfig(remoteBrokerConfig);
			}
			
			try {
				outboundConnector.afterPropertiesSet();
			} catch (Exception e) {
				String msg = "Error creating outbound connector: " + e.getMessage();
				LOG.error(msg, e);
				throw new IllegalArgumentException(msg, e);
			}
			LOG.info("Outbound connector created");
		}
	}

	private void createTargetConnector() {
		if (inboundDestinations != null) {
			inboundConnector = new TargetConnector();
			inboundConnector.setAutoStartup(isAutoStartup());
			inboundConnector.setPhase(getPhase());

			inboundConnector.setInboundDestinations(inboundDestinations);
			inboundConnector.setLocalBrokerConfig(localBrokerConfig);

			// check if the staging queue is on the remote broker
			if (!inboundDestinations.isDefaultStagingLocation()) {
				inboundConnector.setRemoteBrokerConfig(remoteBrokerConfig);
			}

			try {
				inboundConnector.afterPropertiesSet();
			} catch (Exception e) {
				String msg = "Error creating inbound connector: " + e.getMessage();
				LOG.error(msg, e);
				throw new IllegalArgumentException(msg, e);
			}
			LOG.info("Inbound connector created");
		}
	}

	@Override
	protected void doStart() {
		if (outboundConnector != null) {
			outboundConnector.start();
		}
		if (inboundConnector != null) {
			inboundConnector.start();
		}
		LOG.info("Started");
	}

	@Override
	protected void doStop() {
		if (outboundConnector != null) {
			outboundConnector.stop();
		}
		if (inboundConnector != null) {
			inboundConnector.stop();
		}
		LOG.info("Stopped");
	}

	@Override
	protected void doDestroy() throws Exception {
		if (outboundConnector != null) {
			outboundConnector.destroy();
		}
		if (inboundConnector != null) {
			inboundConnector.destroy();
		}
		LOG.info("Destroyed");
	}

	/** 
	 * Only outbound destinations can be configured at runtime
	 * @see org.fusesource.fabric.bridge.internal.AbstractConnector#getDestinationsConfig()
	 */
	@Override
	public BridgeDestinationsConfig getDestinationsConfig() {
		synchronized (lifecycleMonitor) {
			return outboundDestinations;
		}
	}

	/**
	 * Only outbound destinations can be configured at runtime
	 * @see org.fusesource.fabric.bridge.internal.AbstractConnector#setDestinationsConfig(org.fusesource.fabric.bridge.model.BridgeDestinationsConfig)
	 */
	@Override
	public void setDestinationsConfig(BridgeDestinationsConfig destinationsConfig) {
		synchronized (lifecycleMonitor) {
			if (outboundConnector != null) {
				outboundConnector.setDestinationsConfig(destinationsConfig);
				outboundDestinations = destinationsConfig;
			}
		}
	}

	@Override
	public void addDestinations(List<BridgedDestination> destinations) {
		synchronized (lifecycleMonitor) {
			if (!isInitialized()) {
				throw new IllegalStateException("Connector not initialized");
			}
			if (outboundConnector != null) {
				outboundConnector.addDestinations(destinations);
				outboundDestinations = outboundConnector.getOutboundDestinations();
			}
		}
	}

	@Override
	public void removeDestinations(List<BridgedDestination> destinations) {
		synchronized (lifecycleMonitor) {
			if (!isInitialized()) {
				throw new IllegalStateException("Connector not initialized");
			}
			if (outboundConnector != null) {
				outboundConnector.removeDestinations(destinations);
				outboundDestinations = outboundConnector.getOutboundDestinations();
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (localBrokerConfig == null) {
			throw new IllegalArgumentException("The property localBrokerConfig is required");
		}
		if (outboundDestinations == null && inboundDestinations == null) {
			throw new IllegalArgumentException("At least one of inboundDestinations or outboundDestinations property must be provided");
		}

        // check whether a messageSelector is provided or not
        if (inboundDestinations != null &&
            !StringUtils.hasText(inboundDestinations.getDispatchPolicy().getMessageSelector())) {
            LOG.warn("NOTE: Inbound destinations do not use a message selector, " +
                "this configuration only works with a single BridgeConnector connected to another BridgeConnector or GatewayConnector");
        }
		// inbound and outbound connector property validation is delayed until doInitialize()
	}

	public final BrokerConfig getLocalBrokerConfig() {
		return localBrokerConfig;
	}

	public final void setLocalBrokerConfig(BrokerConfig localBrokerConfig) {
		this.localBrokerConfig = localBrokerConfig;
	}

	public final BrokerConfig getRemoteBrokerConfig() {
		return remoteBrokerConfig;
	}

	public final void setRemoteBrokerConfig(BrokerConfig remoteBrokerConfig) {
		this.remoteBrokerConfig = remoteBrokerConfig;
	}

	public final BridgeDestinationsConfig getOutboundDestinations() {
		return outboundDestinations;
	}

	public final void setOutboundDestinations(
			BridgeDestinationsConfig outboundDestinations) {
		this.outboundDestinations = outboundDestinations;
	}

	public final BridgeDestinationsConfig getInboundDestinations() {
		return inboundDestinations;
	}

	public final void setInboundDestinations(
			BridgeDestinationsConfig inboundDestinations) {
		this.inboundDestinations = inboundDestinations;
	}

    public String getOutboundDestinationsRef() {
        return outboundDestinationsRef;
    }

    public void setOutboundDestinationsRef(String outboundDestinationsRef) {
        this.outboundDestinationsRef = outboundDestinationsRef;
    }

    public String getInboundDestinationsRef() {
        return inboundDestinationsRef;
    }

    public void setInboundDestinationsRef(String inboundDestinationsRef) {
        this.inboundDestinationsRef = inboundDestinationsRef;
    }

}
