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
package io.fabric8.bridge.model;

import javax.xml.bind.annotation.*;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import io.fabric8.bridge.BridgeConnector;
import io.fabric8.bridge.GatewayConnector;

/**
 * Represents a remote {@link BridgeConnector} connected to a
 * {@link GatewayConnector}. Allows the {@link GatewayConnector} to connect back
 * to the {@link BridgeConnector}
 * 
 * @author Dhiraj Bokde
 * 
 */
@XmlRootElement(name="remote-bridge")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"remoteBrokerConfig", "inboundDestinations", "outboundDestinations"})
public class RemoteBridge extends IdentifiedType {
	
	@XmlElement(name="remote-broker")
	private BrokerConfig remoteBrokerConfig;
	
	@XmlAttribute
	private String remoteBrokerRef;

    // inbound to gateway
    @XmlElement(name="inbound-destinations")
    private BridgeDestinationsConfig inboundDestinations;

    // outbound from gateway
	@XmlElement(name="outbound-destinations")
	private BridgeDestinationsConfig outboundDestinations;

    @XmlAttribute
    private String inboundDestinationsRef;

	@XmlAttribute
	private String outboundDestinationsRef;

	public final BrokerConfig getRemoteBrokerConfig() {
		return remoteBrokerConfig;
	}

	public final void setRemoteBrokerConfig(BrokerConfig remoteBrokerConfig) {
		this.remoteBrokerConfig = remoteBrokerConfig;
	}

	public String getRemoteBrokerRef() {
		return remoteBrokerRef;
	}

	public void setRemoteBrokerRef(String remoteBrokerRef) {
		this.remoteBrokerRef = remoteBrokerRef;
	}

    public BridgeDestinationsConfig getInboundDestinations() {
        return inboundDestinations;
    }

    public void setInboundDestinations(BridgeDestinationsConfig inboundDestinations) {
        this.inboundDestinations = inboundDestinations;
    }

	public final BridgeDestinationsConfig getOutboundDestinations() {
		return outboundDestinations;
	}

	public final void setOutboundDestinations(
			BridgeDestinationsConfig outboundDestinations) {
		this.outboundDestinations = outboundDestinations;
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

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this,ToStringStyle.SHORT_PREFIX_STYLE);
	}

	@Override
	public int hashCode() {
		return remoteBrokerConfig.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof RemoteBridge) {
			RemoteBridge bridge = (RemoteBridge) obj;
			return (remoteBrokerConfig != null ? remoteBrokerConfig.equals(bridge.remoteBrokerConfig) : bridge.remoteBrokerConfig == null) &&
				(remoteBrokerRef != null ? remoteBrokerRef.equals(bridge.remoteBrokerRef) : bridge.remoteBrokerRef == null) && 
                (inboundDestinations != null ? inboundDestinations.equals(bridge.inboundDestinations) : bridge.inboundDestinations == null) &&
                (inboundDestinationsRef != null ? inboundDestinationsRef.equals(bridge.inboundDestinationsRef) : bridge.inboundDestinationsRef == null) &&
				(outboundDestinations != null ? outboundDestinations.equals(bridge.outboundDestinations) : bridge.outboundDestinations == null) &&
				(outboundDestinationsRef != null ? outboundDestinationsRef.equals(bridge.outboundDestinationsRef) : bridge.outboundDestinationsRef == null);
		}
		return false;
	}

}
