/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.activemq.facade;

import org.apache.activemq.command.ActiveMQDestination;

import java.util.Collection;

/**
 * A facade for either a local in JVM broker or a remote broker over JMX
 *
 * 
 * 
 */
public interface BrokerFacade {

    /**
     * @return a unique id for this resource, typically a JMX ObjectName
     * @throws Exception
     */
    String getId() throws Exception;

    /**
     * Returns all the available brokers.
     *
     * @return not <code>null</code>
     * @throws Exception
     */
    public BrokerFacade[] getBrokers() throws Exception;

	/**
	 * The name of the active broker (f.e. 'localhost' or 'my broker').
	 * 
	 * @return not <code>null</code>
	 * @throws Exception
	 */
	String getBrokerName() throws Exception;

	/**
	 * Admin view of the broker.
	 * 
	 * @return not <code>null</code>
	 * @throws Exception
	 */
	BrokerViewFacade getBrokerAdmin() throws Exception;

	/**
	 * All queues known to the broker.
	 * 
	 * @return not <code>null</code>
	 * @throws Exception
	 */
	Collection<QueueViewFacade> getQueues() throws Exception;

	/**
	 * All topics known to the broker.
	 * 
	 * @return not <code>null</code>
	 * @throws Exception
	 */
	Collection<TopicViewFacade> getTopics() throws Exception;

	/**
	 * All active consumers of a queue.
	 * 
	 * @param queueName
	 *            the name of the queue, not <code>null</code>
	 * @return not <code>null</code>
	 * @throws Exception
	 */
	Collection<SubscriptionViewFacade> getQueueConsumers(String queueName)
			throws Exception;

    /**
     * Returns the consumers available on the given topic
     */
	Collection<SubscriptionViewFacade> getTopicConsumers(String topicName) throws Exception;

    /**
     * Returns the durable consumers available on the given topic
     */
    Collection<DurableSubscriptionViewFacade> getTopicDurableConsumers(String topicName) throws Exception;


    /**
     * Returns the producers available on the given queue
     */
    Collection<ProducerViewFacade> getQueueProducers(String queueName) throws Exception;

    /**
     * Returns the producers available on the given topic
     */
    Collection<ProducerViewFacade> getTopicProducers(String topicName) throws Exception;

	/**
	 * Active durable subscribers to topics of the broker.
	 * 
	 * @return not <code>null</code>
	 * @throws Exception
	 */
	Collection<DurableSubscriptionViewFacade> getDurableTopicSubscribers()
			throws Exception;


	/**
	 * Inactive durable subscribers to topics of the broker.
	 *
	 * @return not <code>null</code>
	 * @throws Exception
	 */
	Collection<DurableSubscriptionViewFacade> getInactiveDurableTopicSubscribers()
			throws Exception;

	/**
	 * The names of all transport connectors of the broker (f.e. openwire, ssl)
	 * 
	 * @return not <code>null</code>
	 * @throws Exception
	 */
	Collection<String> getConnectors() throws Exception;

	/**
	 * A transport connectors.
	 * 
	 * @param name
	 *            name of the connector (f.e. openwire)
	 * @return <code>null</code> if not found
	 * @throws Exception
	 */
	ConnectorViewFacade getConnector(String name) throws Exception;

	/**
	 * All connections to all transport connectors of the broker.
	 * 
	 * @return not <code>null</code>
	 * @throws Exception
	 */
	Collection<ConnectionViewFacade> getConnections() throws Exception;

	/**
	 * The names of all connections to a specific transport connectors of the
	 * broker.
	 * 
	 * @see #getConnection(String)
	 * @param connectorName
	 *            not <code>null</code>
	 * @return not <code>null</code>
	 * @throws Exception
	 */
	Collection<String> getConnections(String connectorName) throws Exception;

	/**
	 * A specific connection to the broker.
	 * 
	 * @param connectionName
	 *            the name of the connection, not <code>null</code>
	 * @return not <code>null</code>
	 * @throws Exception
	 */
	ConnectionViewFacade getConnection(String connectionName) throws Exception;
	/**
	 * Returns all consumers of a connection.
	 * 
	 * @param connectionName
	 *            the name of the connection, not <code>null</code>
	 * @return not <code>null</code>
	 * @throws Exception
	 */
	Collection<SubscriptionViewFacade> getConsumersOnConnection(
			String connectionName) throws Exception;
	/**
	 * The brokers network connectors.
	 * 
	 * @return not <code>null</code>
	 * @throws Exception
	 */
	Collection<NetworkConnectorViewFacade> getNetworkConnectors()
			throws Exception;


	/**
	 * The brokers network bridges.
	 *
	 * @return not <code>null</code>
	 * @throws Exception
	 */
    Collection<NetworkBridgeViewFacade> getNetworkBridges()
            throws Exception;

    /**
	 * Purges the given destination
	 * 
	 * @param destination
	 * @throws Exception
	 */
	void purgeQueue(ActiveMQDestination destination) throws Exception;
	/**
	 * Get the view of the queue with the specified name.
	 * 
	 * @param name
	 *            not <code>null</code>
	 * @return <code>null</code> if no queue with this name exists
	 * @throws Exception
	 */
	QueueViewFacade getQueue(String name) throws Exception;
	/**
	 * Get the view of the topic with the specified name.
	 * 
	 * @param name
	 *            not <code>null</code>
	 * @return <code>null</code> if no topic with this name exists
	 * @throws Exception
	 */
	TopicViewFacade getTopic(String name) throws Exception;
	
	/**
	 * Get the JobScheduler MBean
	 * @return the jobScheduler or null if not configured
	 * @throws Exception
	 */
	JobSchedulerViewFacade getJobScheduler() throws Exception;
	
	/**
     * Get the JobScheduler MBean
     * @return the jobScheduler or null if not configured
     * @throws Exception
     */
    Collection<JobFacade> getScheduledJobs() throws Exception;

    boolean isJobSchedulerStarted();
}
