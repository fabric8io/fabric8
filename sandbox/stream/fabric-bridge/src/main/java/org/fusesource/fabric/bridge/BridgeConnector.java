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
import org.springframework.util.StringUtils;

import javax.xml.bind.annotation.*;
import java.util.List;

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
		} else {
            LOG.info("Outbound connector NOT created since outbound destinations are not configured");
        }
	}

	private void createTargetConnector() {
		if (inboundDestinations != null && inboundDestinations.isUseStagingQueue()) {
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
		} else if (inboundDestinations != null && !inboundDestinations.isUseStagingQueue()) {
            LOG.info("Inbound connector NOT created since a staging queue is not used");
        } else if (inboundDestinations == null) {
            LOG.info("Inbound connector NOT created since inbound destinations are not configured");
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
	 * @see io.fabric8.bridge.internal.AbstractConnector#getDestinationsConfig()
	 */
	@Override
	public BridgeDestinationsConfig getDestinationsConfig() {
		synchronized (lifecycleMonitor) {
			return outboundDestinations;
		}
	}

	/**
	 * Only outbound destinations can be configured at runtime
	 * @see io.fabric8.bridge.internal.AbstractConnector#setDestinationsConfig(io.fabric8.bridge.model.BridgeDestinationsConfig)
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
