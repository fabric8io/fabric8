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

import javax.jms.ConnectionFactory;
import javax.naming.NamingException;
import javax.naming.Referenceable;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import io.fabric8.bridge.internal.ConnectionFactoryAdapter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;

/**
 * @author Dhiraj Bokde
 *
 */
@XmlRootElement(name="broker-config")
@XmlAccessorType(XmlAccessType.NONE)
public class BrokerConfig extends IdentifiedType {

	public static final int DEFAULT_MAX_CONNECTIONS = 10;

	@XmlAttribute
	private String brokerUrl;

	// number of connections for default connection factory created using brokerUrl
	@XmlAttribute
	private int maxConnections = DEFAULT_MAX_CONNECTIONS;

	// use the bean name to lookup in BeanFactory
	// represents the exported connection factory for this bridge
	@XmlElement(name="exportedConnectionFactory")
	@XmlJavaTypeAdapter(ConnectionFactoryAdapter.class)
	@XmlMimeType("application/octet-stream")
	private ConnectionFactory connectionFactory;

	// place holder for Spring bean definition parser
	@XmlAttribute
	private String connectionFactoryRef;
	
	@XmlAttribute
	private String userName;
	
	@XmlAttribute
	private String password;

	@XmlAttribute
	private String clientId;
	
	// use a bean name for marshaling to a remote broker
	private DestinationResolver destinationResolver = new DynamicDestinationResolver();

	@XmlAttribute
	private String destinationResolverRef;

	public final String getBrokerUrl() {
		return brokerUrl;
	}

	public final void setBrokerUrl(String BrokerUrl) {
		this.brokerUrl = BrokerUrl;
	}

	public final void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	public final int getMaxConnections() {
		return maxConnections;
	}

	public final ConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}

	public final void setConnectionFactory(
			ConnectionFactory ConnectionFactory) {
		this.connectionFactory = ConnectionFactory;
	}

	public void setConnectionFactoryRef(String connectionFactoryRef) {
		this.connectionFactoryRef = connectionFactoryRef;
	}

	public String getConnectionFactoryRef() {
		return connectionFactoryRef;
	}

	public final String getUserName() {
		return userName;
	}

	public final void setUserName(String UserName) {
		this.userName = UserName;
	}

	public final String getPassword() {
		return password;
	}

	public final void setPassword(String Password) {
		this.password = Password;
	}

	public final String getClientId() {
		return clientId;
	}

	public final void setClientId(String ClientId) {
		this.clientId = ClientId;
	}
	
	public void setDestinationResolver(DestinationResolver destinationResolver) {
		this.destinationResolver = destinationResolver;
	}

	public DestinationResolver getDestinationResolver() {
		return destinationResolver;
	}

	public String getDestinationResolverRef() {
		return destinationResolverRef;
	}

	public void setDestinationResolverRef(
			String destinationResolverRef) {
		this.destinationResolverRef = destinationResolverRef;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this,ToStringStyle.SHORT_PREFIX_STYLE);
	}

	@Override
	public int hashCode() {
		int val = 0;
		val += (brokerUrl != null) ? brokerUrl.hashCode() : 0;
		val += maxConnections;
		val += (userName != null) ? userName.hashCode() : 0;
		val += (password != null) ? password.hashCode() : 0;
		val += (clientId != null) ? clientId.hashCode() : 0;
		val += (connectionFactoryRef != null) ? connectionFactoryRef.hashCode() : 0;
		val += (destinationResolverRef != null) ? destinationResolverRef.hashCode() : 0;
		if (connectionFactory != null) {
			try {
				val += ((Referenceable)connectionFactory).getReference().hashCode();
			} catch (NamingException e) {
				throw new IllegalArgumentException("Could not get Reference from ConnectionFactory: "
								+ e.getMessage(), e);
			}
		}
		return val;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof BrokerConfig) {
			
			BrokerConfig config = (BrokerConfig) obj;
			// ignore destinationResolver
			boolean retVal = 
				(this.brokerUrl != null  ? this.brokerUrl.equals(config.brokerUrl) : config.brokerUrl == null)
					&& this.maxConnections == config.maxConnections
					&& (this.userName != null ? this.userName.equals(config.userName) : config.userName == null)
					&& (this.password != null ? this.password.equals(config.password) : config.password == null)
					&& (this.clientId != null ? this.clientId.equals(config.clientId) : config.clientId == null)
					&& (this.connectionFactoryRef != null ? this.connectionFactoryRef.equals(config.connectionFactoryRef)
							: config.connectionFactoryRef == null)
					&& (this.destinationResolverRef != null ? this.destinationResolverRef.equals(config.destinationResolverRef)
							: config.destinationResolverRef == null);

			if (retVal && connectionFactory != null) {
				if (config.connectionFactory == null) {
					retVal = false;
				} else {
					try {
						retVal = ((Referenceable) connectionFactory).getReference().equals(
								((Referenceable) config.connectionFactory).getReference());
					} catch (NamingException e) {
						throw new IllegalArgumentException("Could not get Reference from ConnectionFactory: "
								+ e.getMessage(), e);
					}
				}
			} else if (retVal) {
				retVal = (config.connectionFactory == null);
			}

			return retVal;
		}
		
		return false;
	}

}