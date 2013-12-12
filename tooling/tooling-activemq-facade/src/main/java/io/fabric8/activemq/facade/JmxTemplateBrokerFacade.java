/*
 * Copyright 2010 Red Hat, Inc.
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
import io.fabric8.service.JmxTemplateSupport;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import java.util.Collection;

/**
 */
public class JmxTemplateBrokerFacade implements BrokerFacade {
    private final JmxTemplateSupport template;

    public JmxTemplateBrokerFacade(JmxTemplateSupport template) {
        this.template = template;
    }


    /**
     * Executes a JMX operation on a BrokerFacade
     */
    public <T> T execute(final BrokerFacadeCallback<T> callback) {
        return template.execute(new JmxTemplateSupport.JmxConnectorCallback<T>() {
            public T doWithJmxConnector(JMXConnector connector) throws Exception {
                MBeanServerConnection connection = connector.getMBeanServerConnection();
                BrokerFacade brokerFacade = new RemoteBrokerFacade(connection);
                return callback.doWithBrokerFacade(brokerFacade);
            }
        });
    }

    @Override
    public String getId() throws Exception {
        return execute(new BrokerFacadeCallback<String>() {
            public String doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getId();
            }
        });
    }

    @Override
    public BrokerFacade[] getBrokers() throws Exception {
        return execute(new BrokerFacadeCallback<BrokerFacade[]>() {
            public BrokerFacade[] doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getBrokers();
            }
        });
    }

    public BrokerViewFacade getBrokerAdmin() throws Exception {
        return execute(new BrokerFacadeCallback<BrokerViewFacade>() {
            public BrokerViewFacade doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getBrokerAdmin();
            }
        });
    }

    public String getBrokerName() throws Exception {
        return execute(new BrokerFacadeCallback<String>() {
            public String doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getBrokerName();
            }
        });
    }

    public Collection<QueueViewFacade> getQueues() throws Exception {
        return execute(new BrokerFacadeCallback<Collection<QueueViewFacade>>() {
            public Collection<QueueViewFacade> doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getQueues();
            }
        });
    }

    public Collection<TopicViewFacade> getTopics() throws Exception {
        return execute(new BrokerFacadeCallback<Collection<TopicViewFacade>>() {
            public Collection<TopicViewFacade> doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getTopics();
            }
        });
    }

    public Collection<SubscriptionViewFacade> getQueueConsumers(final String queueName) throws Exception {
        return execute(new BrokerFacadeCallback<Collection<SubscriptionViewFacade>>() {
            public Collection<SubscriptionViewFacade> doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getQueueConsumers(queueName);
            }
        });
    }

    @Override
    public Collection<SubscriptionViewFacade> getTopicConsumers(final String topicName) throws Exception {
        return execute(new BrokerFacadeCallback<Collection<SubscriptionViewFacade>>() {
            public Collection<SubscriptionViewFacade> doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getTopicConsumers(topicName);
            }
        });
    }

    @Override
    public Collection<DurableSubscriptionViewFacade> getTopicDurableConsumers(final String topicName) throws Exception {
        return execute(new BrokerFacadeCallback<Collection<DurableSubscriptionViewFacade>>() {
            public Collection<DurableSubscriptionViewFacade> doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getTopicDurableConsumers(topicName);
            }
        });
    }

    public Collection<DurableSubscriptionViewFacade> getDurableTopicSubscribers() throws Exception {
        return execute(new BrokerFacadeCallback<Collection<DurableSubscriptionViewFacade>>() {
            public Collection<DurableSubscriptionViewFacade> doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getDurableTopicSubscribers();
            }
        });
    }

    public Collection<DurableSubscriptionViewFacade> getInactiveDurableTopicSubscribers() throws Exception {
        return execute(new BrokerFacadeCallback<Collection<DurableSubscriptionViewFacade>>() {
            public Collection<DurableSubscriptionViewFacade> doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getInactiveDurableTopicSubscribers();
            }
        });
    }

    @Override
    public Collection<ProducerViewFacade> getQueueProducers(final String queueName) throws Exception {
        return execute(new BrokerFacadeCallback<Collection<ProducerViewFacade>>() {
            public Collection<ProducerViewFacade> doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getQueueProducers(queueName);
            }
        });
    }

    @Override
    public Collection<ProducerViewFacade> getTopicProducers(final String topicName) throws Exception {
        return execute(new BrokerFacadeCallback<Collection<ProducerViewFacade>>() {
            public Collection<ProducerViewFacade> doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getTopicProducers(topicName);
            }
        });
    }

    public Collection<String> getConnectors() throws Exception {
        return execute(new BrokerFacadeCallback<Collection<String>>() {
            public Collection<String> doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getConnectors();
            }
        });
    }

    public ConnectorViewFacade getConnector(final String name) throws Exception {
        return execute(new BrokerFacadeCallback<ConnectorViewFacade>() {
            public ConnectorViewFacade doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getConnector(name);
            }
        });
    }

    public Collection<ConnectionViewFacade> getConnections() throws Exception {
        return execute(new BrokerFacadeCallback<Collection<ConnectionViewFacade>>() {
            public Collection<ConnectionViewFacade> doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getConnections();
            }
        });
    }

    public Collection<String> getConnections(final String connectorName) throws Exception {
        return execute(new BrokerFacadeCallback<Collection<String>>() {
            public Collection<String> doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getConnections(connectorName);
            }
        });
    }

    public ConnectionViewFacade getConnection(final String connectionName) throws Exception {
        return execute(new BrokerFacadeCallback<ConnectionViewFacade>() {
            public ConnectionViewFacade doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getConnection(connectionName);
            }
        });
    }

    public Collection<SubscriptionViewFacade> getConsumersOnConnection(final String connectionName) throws Exception {
        return execute(new BrokerFacadeCallback<Collection<SubscriptionViewFacade>>() {
            public Collection<SubscriptionViewFacade> doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getConsumersOnConnection(connectionName);
            }
        });
    }

    public Collection<NetworkConnectorViewFacade> getNetworkConnectors() throws Exception {
        return execute(new BrokerFacadeCallback<Collection<NetworkConnectorViewFacade>>() {
            public Collection<NetworkConnectorViewFacade> doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getNetworkConnectors();
            }
        });
    }

    public Collection<NetworkBridgeViewFacade> getNetworkBridges() throws Exception {
        return execute(new BrokerFacadeCallback<Collection<NetworkBridgeViewFacade>>() {
            public Collection<NetworkBridgeViewFacade> doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getNetworkBridges();
            }
        });
    }

    public void purgeQueue(final ActiveMQDestination destination) throws Exception {
        execute(new BrokerFacadeCallback() {
            public Object doWithBrokerFacade(BrokerFacade broker) throws Exception {
                broker.purgeQueue(destination);
                return null;
            }
        });
    }

    public QueueViewFacade getQueue(final String name) throws Exception {
        return execute(new BrokerFacadeCallback<QueueViewFacade>() {
            public QueueViewFacade doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getQueue(name);
            }
        });
    }

    public TopicViewFacade getTopic(final String name) throws Exception {
        return execute(new BrokerFacadeCallback<TopicViewFacade>() {
            public TopicViewFacade doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getTopic(name);
            }
        });
    }

    public JobSchedulerViewFacade getJobScheduler() throws Exception {
        return execute(new BrokerFacadeCallback<JobSchedulerViewFacade>() {
            public JobSchedulerViewFacade doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getJobScheduler();
            }
        });
    }

    public Collection<JobFacade> getScheduledJobs() throws Exception {
        return execute(new BrokerFacadeCallback<Collection<JobFacade>>() {
            public Collection<JobFacade> doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.getScheduledJobs();
            }
        });
    }

    public boolean isJobSchedulerStarted() {
        return execute(new BrokerFacadeCallback<Boolean>() {
            public Boolean doWithBrokerFacade(BrokerFacade broker) throws Exception {
                return broker.isJobSchedulerStarted();
            }
        });
    }
}
