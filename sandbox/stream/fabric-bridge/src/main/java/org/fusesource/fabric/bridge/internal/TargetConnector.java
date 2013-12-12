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
package io.fabric8.bridge.internal;

import org.apache.activemq.pool.PooledConnectionFactory;
import io.fabric8.bridge.model.BridgeDestinationsConfig;
import io.fabric8.bridge.model.BridgedDestination;
import io.fabric8.bridge.model.BrokerConfig;
import io.fabric8.bridge.model.DispatchPolicy;
import org.springframework.jms.JmsException;
import org.springframework.jms.UncategorizedJmsException;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import javax.jms.*;
import java.lang.IllegalStateException;
import java.util.List;

/**
 * Connects a staging queue on remote broker to destinations on local broker. 
 * 
 * @author Dhiraj Bokde
 *
 */
public class TargetConnector extends AbstractConnector {
	
	private BrokerConfig localBrokerConfig;
	
	private BrokerConfig remoteBrokerConfig;
	
	private BridgeDestinationsConfig inboundDestinations;
	
	private boolean reuseSession;
	
	private boolean reuseMessage;
	
	private ConnectionFactory localConnectionFactory;
	
	private ConnectionFactory remoteConnectionFactory;
	
	private AbstractMessageListenerContainer listenerContainer;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		// required properties
		if (inboundDestinations == null || localBrokerConfig == null) {
			throw new IllegalArgumentException("Both inboundDestinations and localBrokerConfig properties must be set");
		}
		if (remoteBrokerConfig == null && !inboundDestinations.isDefaultStagingLocation()) {
			throw new IllegalArgumentException("Property remoteBrokerConfig is missing but property defaultStagingLocation is false");
		}
		if (remoteBrokerConfig != null && inboundDestinations.isDefaultStagingLocation()) {
			throw new IllegalArgumentException("Property remoteBrokerConfig is set but property defaultStagingLocation is true");
		}
		if (remoteBrokerConfig != null) {
			if ((remoteBrokerConfig.getBrokerUrl() == null && remoteBrokerConfig.getConnectionFactory() == null) || 
					(remoteBrokerConfig.getBrokerUrl() != null && remoteBrokerConfig.getConnectionFactory() != null)) {
				throw new IllegalArgumentException("Either a remote broker url or connection factory must be provided");
			}
		}
		if ((localBrokerConfig.getBrokerUrl() == null && localBrokerConfig.getConnectionFactory() == null) || 
				(localBrokerConfig.getBrokerUrl() != null && localBrokerConfig.getConnectionFactory() != null)) {
			throw new IllegalArgumentException("Either a local broker url or connection factory must be provided");
		}
		
		// warn about ignored destinations list
		if (inboundDestinations.getDestinations() != null && !inboundDestinations.getDestinations().isEmpty()) {
			LOG.warn("Ignoring destinations for connector: " + inboundDestinations.getDestinations());
		}
	}

	@Override
	protected void doInitialize() {
		// get/create connection factories
		localConnectionFactory = getConnectionFactory(localBrokerConfig);
		LOG.debug("Using local connection factory " + localConnectionFactory);

		if (remoteBrokerConfig == null && inboundDestinations.isDefaultStagingLocation()) {
			remoteConnectionFactory = localConnectionFactory;
			reuseSession = true;
		} else {
			// Sub-optimal configuration with staging queue on remote broker
			remoteConnectionFactory = getConnectionFactory(remoteBrokerConfig);
			LOG.debug("Using remote connection factory " + remoteConnectionFactory);
			inboundDestinations.setDefaultStagingLocation(false);
			reuseSession = false;
		}
		
		reuseMessage = false;
        Connection remoteConnection = null;
        Connection localConnection = null;
        try {
			
			// check if messages need to be copied for different JMS providers
			remoteConnection = remoteConnectionFactory.createConnection();
			Session remoteSession = remoteConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Message remoteMessage = remoteSession.createMessage();
			
			localConnection = localConnectionFactory.createConnection();
			Session localSession = localConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Message localMessage = localSession.createMessage();
			
			if (remoteMessage.getClass().isInstance(localMessage)) {
				reuseMessage  = true;
			}
			
		} catch (JMSException e) {
			String msg = "Error checking whether remote and local broker providers are the same: " + e.getMessage();
			LOG.error(msg, e);
			throw new IllegalStateException(msg, e);
		} finally {
            if (remoteConnection != null) {
                try {
                    remoteConnection.close();
                } catch (JMSException e) {}
            }
            if (localConnection != null) {
                try {
                    localConnection.close();
                } catch (JMSException e) {}
            }
        }

		// create listener container for staging queue
		createListenerContainer();
		LOG.info("Initialized");
	}

	@Override
	protected void doStart() {
		if (!listenerContainer.isRunning()) {
			try {
				listenerContainer.start();
			} catch (JmsException ex) {
				LOG.error("Error starting message listener container: " + ex.getMessage(), ex);
				throw ex;
			}
			LOG.info("Started");
		}
	}

	@Override
	protected void doStop() {
		if (listenerContainer.isRunning()) {
			try {
				listenerContainer.stop();
			} catch (JmsException ex) {
				LOG.error("Error stopping message listener container: " + ex.getMessage(), ex);
				throw ex;
			}
			LOG.info("Stopped");
		}
	}

	@Override
	protected void doDestroy() {
		if (listenerContainer.isActive()) {
			try {
				listenerContainer.destroy();
			} catch (JmsException ex) {
				LOG.error("Error destroying message listener container: " + ex.getMessage(), ex);
				throw ex;
			}
		}

		// check if we created pooled connection factories
        if (localBrokerConfig.getConnectionFactory() == null && localConnectionFactory != null) {
			((PooledConnectionFactory)localConnectionFactory).stop();
		}
		if (remoteBrokerConfig != null && remoteBrokerConfig.getConnectionFactory() == null
            && remoteConnectionFactory != null) {
			((PooledConnectionFactory)remoteConnectionFactory).stop();
		}

        LOG.info("Destroyed");
	}

	protected void createListenerContainer() {
		// create listener container using remoteConnectionFactory
		// and DeliveryHandler using localConnectionFactory
		final DispatchPolicy dispatchPolicy = inboundDestinations.getDispatchPolicy();
		if (LOG.isDebugEnabled()) {
			LOG.debug("Using delivery policy: " + dispatchPolicy);
		}

		// both batch size and timeout are required to enable batch listener
		// set them both to <= zero to use the Spring default listener
		final AbstractDeliveryHandler deliveryHandler = createDeliveryHandler(dispatchPolicy);

		if (dispatchPolicy.getBatchSize() <= 0 || dispatchPolicy.getBatchTimeout() <= 0) {

			if (LOG.isDebugEnabled()) {
				LOG.debug("Creating default message listener container");
			}

			listenerContainer = new DefaultMessageListenerContainer();
			configureListenerContainer(
					(DefaultMessageListenerContainer) listenerContainer,
					dispatchPolicy,
					remoteBrokerConfig == null,
					(remoteBrokerConfig == null ? localBrokerConfig.getDestinationResolver() 
							: remoteBrokerConfig.getDestinationResolver()));
			listenerContainer.setMessageListener(deliveryHandler);

		} else {

			if (LOG.isDebugEnabled()) {
				LOG.debug("Creating batch message listener container");
			}
			
			listenerContainer = new BatchMessageListenerContainer();
			configureListenerContainer(
					(BatchMessageListenerContainer) listenerContainer,
					dispatchPolicy,
					remoteBrokerConfig == null,
					(remoteBrokerConfig == null ? localBrokerConfig.getDestinationResolver() 
							: remoteBrokerConfig.getDestinationResolver()));
			((BatchMessageListenerContainer) listenerContainer).setBatchMessageListener(deliveryHandler);
		
		}
		
		listenerContainer.setConnectionFactory(remoteConnectionFactory);
		listenerContainer.setDestinationName(inboundDestinations.getStagingQueueName());
		listenerContainer.setMessageSelector(dispatchPolicy.getMessageSelector());
		listenerContainer.setClientId((remoteBrokerConfig != null) ? 
						remoteBrokerConfig.getClientId() : localBrokerConfig.getClientId());
		
		// initialize the listener
		listenerContainer.afterPropertiesSet();
	}

	protected AbstractDeliveryHandler createDeliveryHandler(DispatchPolicy resolvedPolicy) {

		TargetDeliveryHandler deliveryHandler = new TargetDeliveryHandler();
		
		deliveryHandler.setDispatchPolicy(resolvedPolicy);
		deliveryHandler.setDestinationNameHeader(inboundDestinations.getDestinationNameHeader());
		deliveryHandler.setDestinationTypeHeader(inboundDestinations.getDestinationTypeHeader());
		deliveryHandler.setReuseSession(reuseSession);
		deliveryHandler.setReuseMessage(reuseMessage);
		deliveryHandler.setTargetConnectionFactory(localConnectionFactory);
		deliveryHandler.setDestinationResolver(localBrokerConfig.getDestinationResolver());
		
		return deliveryHandler;
	}

	@Override
	public BridgeDestinationsConfig getDestinationsConfig() {
		synchronized (lifecycleMonitor) {
			return inboundDestinations;
		}
	}

	@Override
	public void setDestinationsConfig(
			BridgeDestinationsConfig destinationsConfig) throws JmsException {
		synchronized (lifecycleMonitor) {

			if (destinationsConfig == null) {
				throw new UncategorizedJmsException("Invalid destinations config");
			}

			// remember current state
			boolean wasRunning = isRunning();
	
			// destroy and recreate the connector
			try {
				destroy();
			} catch (Exception e) {
				LOG.error("Error destroying connector: " + e.getMessage(), e);
				// ignore and keep going??
			}
			
			this.inboundDestinations = destinationsConfig;

			try {
				afterPropertiesSet();
			} catch (Exception e) {
				throw new UncategorizedJmsException(e.getMessage(), e);
			}
			
			if (wasRunning) {
				doStart();
			}
		}
	}

	@Override
	public void addDestinations(List<BridgedDestination> destinations) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeDestinations(List<BridgedDestination> destinations) {
		throw new UnsupportedOperationException();
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

	public final BridgeDestinationsConfig getInboundDestinations() {
		return inboundDestinations;
	}

	public final void setInboundDestinations(
			BridgeDestinationsConfig inboundDestinations) {
		this.inboundDestinations = inboundDestinations;
	}

}
