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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import io.fabric8.bridge.DestinationsConfigManager;
import io.fabric8.bridge.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.jms.JmsException;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.destination.DestinationResolver;

import javax.jms.ConnectionFactory;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * @author Dhiraj Bokde
 *
 */
@XmlType(name="abstract-connector")
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AbstractConnector extends IdentifiedType
		implements SmartLifecycle, InitializingBean, DisposableBean,
		ApplicationContextAware, DestinationsConfigManager {

    protected final Logger LOG = LoggerFactory.getLogger(this.getClass());

	protected Object lifecycleMonitor = new Object();
	
	private volatile boolean initialized;

	private volatile boolean running;
	
	protected ApplicationContext applicationContext;

	@XmlAttribute
	private int phase = Integer.MAX_VALUE;
	
	@XmlAttribute
	private boolean autoStartup = true;

	@Override
	public void start() {
		synchronized (lifecycleMonitor) {
			if (running) {
				return;
			}

			if (!initialized) {
				doInitialize();
				initialized = true;
			}
			
			doStart();
			running = true;
		}
	}

	protected abstract void doInitialize();

	protected abstract void doStart();

	@Override
	public void stop() {
		synchronized (lifecycleMonitor) {
			if (!running) {
				return;
			}
			doStop();
			running = false;
		}
	}

	@Override
	public void stop(Runnable callback) {
		this.stop();
		callback.run();
	}

	protected abstract void doStop();
	
	@Override
	public void destroy() throws Exception {
		synchronized (lifecycleMonitor) {
			if (!initialized) {
				return;
			}
			if (running) {
				doStop();
				running = false;
			}
			doDestroy();
			initialized = false;
		}
	}

	protected abstract void doDestroy() throws Exception;

	@Override
	public abstract BridgeDestinationsConfig getDestinationsConfig() throws JmsException;

	@Override
	public abstract void setDestinationsConfig(BridgeDestinationsConfig destinationsConfig) throws JmsException;
	
	@Override
	public abstract void addDestinations(List<BridgedDestination> destinations) throws JmsException;
	
	@Override
	public abstract void removeDestinations(List<BridgedDestination> destinations) throws JmsException;

	@Override
	public abstract void afterPropertiesSet() throws Exception;

	@Override
	public boolean isRunning() {
		return running;
	}

	public final void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	@Override
	public int getPhase() {
		return phase ;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public boolean isAutoStartup() {
		return autoStartup ;
	}

	protected ConnectionFactory getConnectionFactory(BrokerConfig brokerConfig) {
	
		if (brokerConfig.getBrokerUrl() != null) {
			// create a default pooled connection factory
			// note that this does not support XA, so use idempotent JMS consumers
			PooledConnectionFactory pooledConnectionFactory = null;
		
			if (brokerConfig.getUserName() != null) {
				ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
				connectionFactory.setUserName(brokerConfig.getUserName());
				connectionFactory.setPassword(brokerConfig.getPassword());
				pooledConnectionFactory = new PooledConnectionFactory();
				pooledConnectionFactory.setConnectionFactory(connectionFactory);
	
			} else {
				pooledConnectionFactory = new PooledConnectionFactory(brokerConfig.getBrokerUrl());
			}
			
			pooledConnectionFactory.setMaxConnections(brokerConfig.getMaxConnections());
			if (brokerConfig.getMaxConnections() > 0) {
				pooledConnectionFactory.setMaxConnections(brokerConfig.getMaxConnections());
			}
			return pooledConnectionFactory;
		}
	
		return brokerConfig.getConnectionFactory();
	}

	protected void configureListenerContainer(
			DefaultMessageListenerContainer listenerContainer,
			DispatchPolicy resolvedPolicy, boolean localListener,
			DestinationResolver destinationResolver) {
	
		listenerContainer.setCacheLevel(resolvedPolicy.getCacheLevel());
		listenerContainer.setConcurrentConsumers(resolvedPolicy.getConcurrentConsumers());
		listenerContainer.setMaxConcurrentConsumers(resolvedPolicy.getMaxConcurrentConsumers());
		
		listenerContainer.setSessionAcknowledgeMode(localListener ? resolvedPolicy.getLocalAcknowledgeMode() : resolvedPolicy.getRemoteAcknowledgeMode());
		listenerContainer.setSessionTransacted(localListener ? resolvedPolicy.isLocalSessionTransacted() : resolvedPolicy.isRemoteSessionTransacted());
		listenerContainer.setMessageSelector(resolvedPolicy.getMessageSelector());
		listenerContainer.setDestinationResolver(destinationResolver);
		listenerContainer.setAutoStartup(false);
	
	}

	protected void configureListenerContainer(
			BatchMessageListenerContainer listenerContainer,
			DispatchPolicy resolvedPolicy, boolean localListener,
			DestinationResolver destinationResolver) {
		
		configureListenerContainer((DefaultMessageListenerContainer)listenerContainer, resolvedPolicy, localListener, destinationResolver);
		
		listenerContainer.setBatchSize(resolvedPolicy.getBatchSize());
		listenerContainer.setBatchTimeout(resolvedPolicy.getBatchTimeout());
	
	}

	protected final boolean isInitialized() {
		return initialized;
	}

}