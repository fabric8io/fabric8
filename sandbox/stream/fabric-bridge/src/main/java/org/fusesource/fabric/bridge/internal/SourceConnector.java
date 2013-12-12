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
import org.springframework.beans.BeanUtils;
import org.springframework.jms.JmsException;
import org.springframework.jms.UncategorizedJmsException;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.destination.DestinationResolver;

import javax.jms.*;
import java.beans.PropertyDescriptor;
import java.lang.IllegalStateException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Connects destinations on local broker to a staging queue on remote broker. 
 * 
 * @author Dhiraj Bokde
 *
 */
public class SourceConnector extends AbstractConnector {

	private BrokerConfig localBrokerConfig;
	
	private BrokerConfig remoteBrokerConfig;
	
	private BridgeDestinationsConfig outboundDestinations;
	
	private boolean reuseSession;
	
	private boolean reuseMessage;
	
	private Destination stagingQueue;
	
	private ConnectionFactory localConnectionFactory;
	
	private ConnectionFactory remoteConnectionFactory;
	
	private Map<BridgedDestination, AbstractMessageListenerContainer> listenerMap = new HashMap<BridgedDestination, AbstractMessageListenerContainer>();

	@Override
	public void afterPropertiesSet() throws Exception {
		
		if (outboundDestinations == null || localBrokerConfig == null) {
			throw new IllegalArgumentException("Both outboundDestinations and localBrokerConfig properties must be set");
		}
        if (remoteBrokerConfig == null && !outboundDestinations.isUseStagingQueue()) {
            throw new IllegalArgumentException("Property remoteBrokerConfig is missing but property useStagingQueue is false");
        }
        if (remoteBrokerConfig == null && outboundDestinations.isDefaultStagingLocation()) {
            throw new IllegalArgumentException("Property remoteBrokerConfig is missing but property defaultStagingLocation is true");
        }
		if (remoteBrokerConfig != null && !outboundDestinations.isDefaultStagingLocation()) {
			throw new IllegalArgumentException("Property remoteBrokerConfig is set but property defaultStagingLocation is false");
		}
		if ((localBrokerConfig.getBrokerUrl() == null && localBrokerConfig.getConnectionFactory() == null) || 
				(localBrokerConfig.getBrokerUrl() != null && localBrokerConfig.getConnectionFactory() != null)) {
			throw new IllegalArgumentException("Either a local broker url or connection factory must be provided");
		}
		if (remoteBrokerConfig != null) {
			if ((remoteBrokerConfig.getBrokerUrl() == null && remoteBrokerConfig.getConnectionFactory() == null) || 
					(remoteBrokerConfig.getBrokerUrl() != null && remoteBrokerConfig.getConnectionFactory() != null)) {
				throw new IllegalArgumentException("Either a remote broker url or connection factory must be provided");
			}
		}
	}

	protected void doInitialize() {
	
		// get local and remote connection factories
		localConnectionFactory = getConnectionFactory(localBrokerConfig);
		LOG.debug("Using local connection factory " + localConnectionFactory);
		
		if (remoteBrokerConfig != null && outboundDestinations.isDefaultStagingLocation()) {
			remoteConnectionFactory = getConnectionFactory(remoteBrokerConfig);
			LOG.debug("Using remote connection factory " + remoteConnectionFactory);
			reuseSession = false;
		} else {
			// Note that this is sub-optimal, as consumer connections from
			// remote broker to local broker can't be 'occasionally connected'
			// like producer connections from local to remote broker that are
			// only used when messages are being sent.
			// Set the defaultStagingLocation to false if it hasn't been set
			// already since the staging queue will be created locally
			LOG.warn("Using local broker for staging queue");
			outboundDestinations.setDefaultStagingLocation(false);
			remoteConnectionFactory = localConnectionFactory;
			reuseSession = true;
		}
		
		reuseMessage = false;
        Connection localConnection = null;
        Connection remoteConnection = null;
        try {
			
			// check if messages need to be copied for different JMS providers
			localConnection = localConnectionFactory.createConnection();
			Session localSession = localConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Message localMessage = localSession.createMessage();
			
			remoteConnection = remoteConnectionFactory.createConnection();
			Session remoteSession = remoteConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Message remoteMessage = remoteSession.createMessage();
			
			if (localMessage.getClass().isInstance(remoteMessage)) {
				reuseMessage  = true;
			}
			
			// also get the stagingQueue from remoteSession
			stagingQueue = getStagingQueue(remoteSession);
		
		} catch (JMSException e) {
			String msg = "Error checking whether local and remote broker providers are the same: " + e.getMessage();
			LOG.error(msg, e);
			throw new IllegalStateException(msg, e);
		} finally {
            if (localConnection != null) {
                try {
                    localConnection.close();
                } catch (JMSException e) {}
            }
            if (remoteConnection != null) {
                try {
                    remoteConnection.close();
                } catch (JMSException e) {}
            }
        }

		for (BridgedDestination destination : outboundDestinations.getDestinations()) {
			try {
				listenerMap.put(destination, createListenerContainer(destination));
			} catch (Exception e) {
				final String msg = "Error creating listener for destination: " + destination;
				LOG.error(msg, e);
				throw new IllegalStateException(msg, e);
			}
		}
		
		LOG.info("Initialized");
	}

	private Destination getStagingQueue(Session remoteSession) throws JMSException {
        // check if a staging queue is required
        if (!outboundDestinations.isUseStagingQueue()) {
            return null;
        }

		DestinationResolver destinationResolver = null;
		if (remoteBrokerConfig != null) {
			destinationResolver  = remoteBrokerConfig.getDestinationResolver();
		} else {
			destinationResolver = localBrokerConfig.getDestinationResolver();
		}
		return destinationResolver.resolveDestinationName(remoteSession, outboundDestinations.getStagingQueueName(), false);
	}

	protected void doStart() {

		JmsException failedException = null;
		// start all the listener containers
		for (Entry<BridgedDestination, AbstractMessageListenerContainer> entry : listenerMap.entrySet()) {
			final BridgedDestination destination = entry.getKey();
			final AbstractMessageListenerContainer listener = entry.getValue();

			if (!listener.isRunning()) {
				try {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Starting listener for " + destination);
					}
					listener.start();

					if (LOG.isDebugEnabled()) {
						LOG.debug("Listener started for " + destination);
					}
				} catch (JmsException e) {
					failedException = e;
					LOG.error("Error stopping connector for listener " + listener + ": " + e.getMessage(), e);
					break;
				}
			}
		}
		
		if (failedException != null) {
			// stop all listeners
			stop();
			throw failedException;
		}

		LOG.info("Started");
	}

	protected void doStop() {

		// stop all listener containers
		for (Entry<BridgedDestination, AbstractMessageListenerContainer> entry : listenerMap.entrySet()) {
			
			final BridgedDestination destination = entry.getKey();
			final AbstractMessageListenerContainer listener = entry.getValue();
		
			if (listener.isRunning()) {
				try {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Stopping listener for " + destination);
					}
					listener.stop();
					if (LOG.isDebugEnabled()) {
						LOG.debug("Listener stopped for " + destination);
					}
				
				} catch (JmsException e) {
					LOG.error("Error stopping connector for listener " + listener + ": " + e.getMessage(), e);
				}
			}
		}

		LOG.info("Stopped");
	}

	protected void doDestroy() {

		// destroy all listener containers
		for (Entry<BridgedDestination, AbstractMessageListenerContainer> entry : listenerMap.entrySet()) {
			
			final BridgedDestination destination = entry.getKey();
			final AbstractMessageListenerContainer listener = entry.getValue();

			if (listener.isActive()) {
				try {
				
					if (LOG.isDebugEnabled()) {
						LOG.debug("Destroying listener for " + destination);
					}
					listener.destroy();
					if (LOG.isDebugEnabled()) {
						LOG.debug("Listener destroyed for " + destination);
					}
		
				} catch (JmsException e) {
					LOG.error("Error destroying listener for destination "
							+ destination.getName() + " : " + e.getMessage(), e);
				}
			}
		}
		
		listenerMap.clear();
		
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

	protected AbstractMessageListenerContainer createListenerContainer(BridgedDestination destination) {
		// create listener container using localConnectionFactory
		// and DeliveryHandler using remoteConnectionFactory
		DispatchPolicy resolvedPolicy = getResolvedPolicy(destination.getDispatchPolicy(), outboundDestinations.getDispatchPolicy());
		if (LOG.isDebugEnabled()) {
			LOG.debug("Resolved policy for destination " + destination + " is : " + resolvedPolicy);
		}

		// both batch size and timeout are required to enable batch listener
		// set them both to <= zero to use the Spring default listener
		DefaultMessageListenerContainer listenerContainer;
		AbstractDeliveryHandler deliveryHandler = createDeliveryHandler(
				resolvedPolicy,
				(destination.getTargetName() != null) ? destination.getTargetName() : destination.getName(),
				destination.isPubSubDomain());

		if (resolvedPolicy.getBatchSize() <= 0 || resolvedPolicy.getBatchTimeout() <= 0) {

			if (LOG.isDebugEnabled()) {
				LOG.debug("Creating default message listener container for " + destination.getName());
			}

			listenerContainer = new DefaultMessageListenerContainer();
			configureListenerContainer(listenerContainer, resolvedPolicy, true, localBrokerConfig.getDestinationResolver());
			listenerContainer.setMessageListener(deliveryHandler);

		} else {

			if (LOG.isDebugEnabled()) {
				LOG.debug("Creating batch message listener container for " + destination.getName());
			}
			
			listenerContainer = new BatchMessageListenerContainer();
			configureListenerContainer((BatchMessageListenerContainer)listenerContainer, resolvedPolicy, true, localBrokerConfig.getDestinationResolver());
			((BatchMessageListenerContainer) listenerContainer).setBatchMessageListener(deliveryHandler);
		
			
		}
		
		listenerContainer.setConnectionFactory(localConnectionFactory);
		listenerContainer.setDestinationName(destination.getName());
		listenerContainer.setPubSubDomain(destination.isPubSubDomain());
		listenerContainer.setClientId(localBrokerConfig.getClientId());
		listenerContainer.setDurableSubscriptionName(destination.getDurableSubscriptionName());
		listenerContainer.setSubscriptionDurable(destination.isSubscriptionDurable());
		
		// initialize the listener
		listenerContainer.afterPropertiesSet();

		return listenerContainer;
	}

	protected DispatchPolicy getResolvedPolicy(DispatchPolicy dispatchPolicy,
			DispatchPolicy defaultDispatchPolicy) {
		
		if (dispatchPolicy == null) {
			return defaultDispatchPolicy;
		}
		
		DispatchPolicy resolvedPolicy = new DispatchPolicy();
		BeanUtils.copyProperties(defaultDispatchPolicy, resolvedPolicy);
		
		Set<String> propertiesSet = dispatchPolicy.getPropertiesSet();
		PropertyDescriptor[] propertyDescriptors = BeanUtils.getPropertyDescriptors(DispatchPolicy.class);
		Set<String> ignoreProperties = new HashSet<String>();
		
		for (PropertyDescriptor property : propertyDescriptors) {
			if (!propertiesSet.contains(property.getName())) {
				ignoreProperties.add(property.getName());
			}
		}

		BeanUtils.copyProperties(dispatchPolicy, resolvedPolicy,
				ignoreProperties.toArray(new String[ignoreProperties.size()]));

		return resolvedPolicy;
	}

    protected AbstractDeliveryHandler createDeliveryHandler(DispatchPolicy resolvedPolicy, String destinationName, boolean pubSubDomain) {
        // create delivery handler for staging queue using remoteConnectionFactory
		// delivery is always done in the same thread as the receiving destination
		SourceDeliveryHandler deliveryHandler = new SourceDeliveryHandler();

		deliveryHandler.setDispatchPolicy(resolvedPolicy);
		deliveryHandler.setDestinationNameHeader(outboundDestinations.getDestinationNameHeader());
		deliveryHandler.setDestinationTypeHeader(outboundDestinations.getDestinationTypeHeader());
		deliveryHandler.setDestinationName(destinationName);
		deliveryHandler.setPubSubDomain(pubSubDomain);
		deliveryHandler.setReuseSession(reuseSession);
		deliveryHandler.setReuseMessage(reuseMessage);
		deliveryHandler.setTargetConnectionFactory(remoteConnectionFactory);

        // check if a staging queue is used or not
        if (stagingQueue != null) {
		    deliveryHandler.setStagingDestination(stagingQueue);
        } else {
            // creating a connection for every remote destination resolve seems inefficient,
            // but we cannot make that stateful, also pooling makes it a moot point
            Connection remoteConnection = null;
            try {
                remoteConnection = remoteConnectionFactory.createConnection();
                Session remoteSession = remoteConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                deliveryHandler.setStagingDestination(
                    remoteBrokerConfig.getDestinationResolver().resolveDestinationName(
                        remoteSession, destinationName, pubSubDomain));
            } catch (JMSException e) {
                String msg = "Error resolving remote destination " + destinationName + " : " + e.getMessage();
                LOG.error(msg, e);
                throw new IllegalStateException(msg, e);
            } finally {
                try {
                    remoteConnection.close();
                } catch (JMSException e) {}
            }
        }
		return deliveryHandler;
	}

	@Override
	public BridgeDestinationsConfig getDestinationsConfig() {
		synchronized (lifecycleMonitor) {
			return outboundDestinations;
		}
	}

	@Override
	public void setDestinationsConfig(BridgeDestinationsConfig destinationsConfig) {
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
		}
	}

	@Override
	public void addDestinations(List<BridgedDestination> destinations) {
		synchronized (lifecycleMonitor) {
			final Set<String> outboundDestinationNames = getOutboundDestinationNames();
			
			// remove any duplicate destinations
			final List<BridgedDestination> newDestinations = new ArrayList<BridgedDestination>();
			for (BridgedDestination destination : destinations) {
				if (!outboundDestinationNames.contains(destination.getName())) {
					newDestinations.add(destination);
				} else {
					LOG.warn("Ignoring destination " + destination.getName() + " as it already exists as an outbound destination");
				}
			}
	
			// process new destinations depending upon current state
			if (!isInitialized()) {
				// just add destinations to current config
				outboundDestinations.getDestinations().addAll(newDestinations);
			} else {
				// create listeners for new destinations
				Map<BridgedDestination, AbstractMessageListenerContainer> newListenerMap = new HashMap<BridgedDestination, AbstractMessageListenerContainer>();
				for (BridgedDestination destination : newDestinations) {
					try {
						newListenerMap.put(destination, createListenerContainer(destination));
					} catch (Exception e) {
						final String msg = "Error creating listener for new destination: " + destination;
						LOG.error(msg, e);
	
						// destroy any listeners created so far
						for (Entry<BridgedDestination, AbstractMessageListenerContainer> entry : newListenerMap.entrySet()) {
							try {
								entry.getValue().destroy();
							} catch (Exception ex) {
								LOG.warn("Error destorying listener for " + entry.getKey(), ex);
							}
						}
	
						newListenerMap.clear();
						newDestinations.clear();
						throw new IllegalStateException(msg, e);
					}
				}
			
				// add destinations to current config
				outboundDestinations.getDestinations().addAll(newDestinations);

				// add listeners to current config
				listenerMap.putAll(newListenerMap);
	
				// do we need to start the new listeners??
				if (isRunning()) {
					start();
				}
			}
		}
	}

	@Override
	public void removeDestinations(List<BridgedDestination> destinations) {
		synchronized (lifecycleMonitor) {

			Set<String> outboundDestinationNames = getOutboundDestinationNames();
			
			// remove any missing destinations
			final List<BridgedDestination> currentDestinations = new ArrayList<BridgedDestination>();
			for (BridgedDestination destination : destinations) {
				if (outboundDestinationNames.contains(destination.getName())) {
					currentDestinations.add(destination);
				} else {
					LOG.warn("Ignoring destination " + destination + " as it does not exist as an outbound destination");
				}
			}
	
			// process current destinations depending upon current state
			if (!isInitialized()) {
				// just remove destinations from current config
				outboundDestinations.getDestinations().removeAll(currentDestinations);
			} else {
				// destroy listeners for current destinations
				for (BridgedDestination destination : currentDestinations) {
					try {
						// remove listener from current config
						AbstractMessageListenerContainer listener = listenerMap.remove(destination);
	
						// this will also stop the listener before destroying it
						listener.destroy();
						
					} catch (Exception e) {
						final String msg = "Error destroying listener for destination: " + destination;
						LOG.warn(msg, e);
						// ignore and keep processing the rest??
					}
				}
			
				// remove destinations from current config
				outboundDestinations.getDestinations().removeAll(currentDestinations);
			}
		}
	}

	private Set<String> getOutboundDestinationNames() {
		final Set<String> outboundDestinationNames = new HashSet<String>();
		for (BridgedDestination dest : outboundDestinations.getDestinations()) {
			outboundDestinationNames.add(dest.getName());
		}
		return outboundDestinationNames;
	}

	public BrokerConfig getLocalBrokerConfig() {
		return localBrokerConfig;
	}

	public void setLocalBrokerConfig(BrokerConfig localBrokerConfig) {
		this.localBrokerConfig = localBrokerConfig;
	}

	public BrokerConfig getRemoteBrokerConfig() {
		return remoteBrokerConfig;
	}

	public void setRemoteBrokerConfig(BrokerConfig remoteBrokerConfig) {
		this.remoteBrokerConfig = remoteBrokerConfig;
	}

	public BridgeDestinationsConfig getOutboundDestinations() {
		return outboundDestinations;
	}

	public void setOutboundDestinations(BridgeDestinationsConfig outboundDestinations) {
		this.outboundDestinations = outboundDestinations;
	}

}
