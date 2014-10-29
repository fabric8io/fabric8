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
package io.fabric8.gateway.handlers.detecting.protocol.openwire.support.advisory;

import io.fabric8.gateway.handlers.detecting.protocol.openwire.command.ActiveMQDestination;
import io.fabric8.gateway.handlers.detecting.protocol.openwire.command.ActiveMQTopic;
import org.fusesource.hawtbuf.UTF8Buffer;
import static org.fusesource.hawtbuf.Buffer.*;

public final class AdvisorySupport {

    public static final String ADVISORY_TOPIC_PREFIX = "ActiveMQ.Advisory.";
    public static final ActiveMQTopic CONNECTION_ADVISORY_TOPIC = new ActiveMQTopic(ADVISORY_TOPIC_PREFIX + "Connection");
    public static final ActiveMQTopic QUEUE_ADVISORY_TOPIC = new ActiveMQTopic(ADVISORY_TOPIC_PREFIX + "Queue");
    public static final ActiveMQTopic TOPIC_ADVISORY_TOPIC = new ActiveMQTopic(ADVISORY_TOPIC_PREFIX + "Topic");
    public static final ActiveMQTopic TEMP_QUEUE_ADVISORY_TOPIC = new ActiveMQTopic(ADVISORY_TOPIC_PREFIX + "TempQueue");
    public static final ActiveMQTopic TEMP_TOPIC_ADVISORY_TOPIC = new ActiveMQTopic(ADVISORY_TOPIC_PREFIX + "TempTopic");
    public static final String PRODUCER_ADVISORY_TOPIC_PREFIX = ADVISORY_TOPIC_PREFIX + "Producer.";
    public static final String QUEUE_PRODUCER_ADVISORY_TOPIC_PREFIX = PRODUCER_ADVISORY_TOPIC_PREFIX + "Queue.";
    public static final String TOPIC_PRODUCER_ADVISORY_TOPIC_PREFIX = PRODUCER_ADVISORY_TOPIC_PREFIX + "Topic.";
    public static final String CONSUMER_ADVISORY_TOPIC_PREFIX = ADVISORY_TOPIC_PREFIX + "Consumer.";
    public static final String QUEUE_CONSUMER_ADVISORY_TOPIC_PREFIX = CONSUMER_ADVISORY_TOPIC_PREFIX + "Queue.";
    public static final String TOPIC_CONSUMER_ADVISORY_TOPIC_PREFIX = CONSUMER_ADVISORY_TOPIC_PREFIX + "Topic.";
    public static final String EXPIRED_TOPIC_MESSAGES_TOPIC_PREFIX = ADVISORY_TOPIC_PREFIX + "Expired.Topic.";
    public static final String EXPIRED_QUEUE_MESSAGES_TOPIC_PREFIX = ADVISORY_TOPIC_PREFIX + "Expired.Queue.";
    public static final String NO_TOPIC_CONSUMERS_TOPIC_PREFIX = ADVISORY_TOPIC_PREFIX + "NoConsumer.Topic.";
    public static final String NO_QUEUE_CONSUMERS_TOPIC_PREFIX = ADVISORY_TOPIC_PREFIX + "NoConsumer.Queue.";
    public static final String SLOW_CONSUMER_TOPIC_PREFIX = ADVISORY_TOPIC_PREFIX + "SlowConsumer.";
    public static final String FAST_PRODUCER_TOPIC_PREFIX = ADVISORY_TOPIC_PREFIX + "FastPorducer.";
    public static final String MESSAGE_DISCAREDED_TOPIC_PREFIX = ADVISORY_TOPIC_PREFIX + "MessageDiscarded.";
    public static final String FULL_TOPIC_PREFIX = ADVISORY_TOPIC_PREFIX + "FULL.";
    public static final String MESSAGE_DELIVERED_TOPIC_PREFIX = ADVISORY_TOPIC_PREFIX + "MessageDelivered.";
    public static final String MESSAGE_CONSUMED_TOPIC_PREFIX = ADVISORY_TOPIC_PREFIX + "MessageConsumed.";
    public static final String MASTER_BROKER_TOPIC_PREFIX = ADVISORY_TOPIC_PREFIX + "MasterBroker";
    public static final String AGENT_TOPIC = "ActiveMQ.Agent";
    public static final String ADIVSORY_MESSAGE_TYPE = "Advisory";
    public static final UTF8Buffer ADIVSORY_MESSAGE_TYPE_BUFFER = utf8("Advisory");
    public static final String MSG_PROPERTY_ORIGIN_BROKER_ID="originBrokerId";
    public static final String MSG_PROPERTY_ORIGIN_BROKER_NAME="originBrokerName";
    public static final String MSG_PROPERTY_ORIGIN_BROKER_URL="originBrokerURL";
    public static final String MSG_PROPERTY_USAGE_NAME="usageName";
    public static final String MSG_PROPERTY_CONSUMER_ID="consumerId";
    public static final String MSG_PROPERTY_PRODUCER_ID="producerId";
    public static final String MSG_PROPERTY_MESSAGE_ID="orignalMessageId";
    public static final ActiveMQTopic TEMP_DESTINATION_COMPOSITE_ADVISORY_TOPIC = new ActiveMQTopic(TEMP_QUEUE_ADVISORY_TOPIC + "," + TEMP_TOPIC_ADVISORY_TOPIC);
    private static final ActiveMQTopic AGENT_TOPIC_DESTINATION = new ActiveMQTopic(AGENT_TOPIC);

    private AdvisorySupport() {        
    }
    
    public static ActiveMQTopic getConnectionAdvisoryTopic() {
        return CONNECTION_ADVISORY_TOPIC;
    }

    public static ActiveMQTopic getConsumerAdvisoryTopic(ActiveMQDestination destination) {
        if (destination.isQueue()) {
            return new ActiveMQTopic(QUEUE_CONSUMER_ADVISORY_TOPIC_PREFIX + destination.getPhysicalName());
        } else {
            return new ActiveMQTopic(TOPIC_CONSUMER_ADVISORY_TOPIC_PREFIX + destination.getPhysicalName());
        }
    }

    public static ActiveMQTopic getProducerAdvisoryTopic(ActiveMQDestination destination) {
        if (destination.isQueue()) {
            return new ActiveMQTopic(QUEUE_PRODUCER_ADVISORY_TOPIC_PREFIX + destination.getPhysicalName());
        } else {
            return new ActiveMQTopic(TOPIC_PRODUCER_ADVISORY_TOPIC_PREFIX + destination.getPhysicalName());
        }
    }

    public static ActiveMQTopic getExpiredMessageTopic(ActiveMQDestination destination) {
        if (destination.isQueue()) {
            return getExpiredQueueMessageAdvisoryTopic(destination);
        }
        return getExpiredTopicMessageAdvisoryTopic(destination);
    }

    public static ActiveMQTopic getExpiredTopicMessageAdvisoryTopic(ActiveMQDestination destination) {
        String name = EXPIRED_TOPIC_MESSAGES_TOPIC_PREFIX + destination.getPhysicalName();
        return new ActiveMQTopic(name);
    }

    public static ActiveMQTopic getExpiredQueueMessageAdvisoryTopic(ActiveMQDestination destination) {
        String name = EXPIRED_QUEUE_MESSAGES_TOPIC_PREFIX + destination.getPhysicalName();
        return new ActiveMQTopic(name);
    }

    public static ActiveMQTopic getNoTopicConsumersAdvisoryTopic(ActiveMQDestination destination) {
        String name = NO_TOPIC_CONSUMERS_TOPIC_PREFIX + destination.getPhysicalName();
        return new ActiveMQTopic(name);
    }

    public static ActiveMQTopic getNoQueueConsumersAdvisoryTopic(ActiveMQDestination destination) {
        String name = NO_QUEUE_CONSUMERS_TOPIC_PREFIX + destination.getPhysicalName();
        return new ActiveMQTopic(name);
    }
    
    public static ActiveMQTopic getSlowConsumerAdvisoryTopic(ActiveMQDestination destination) {
        String name = SLOW_CONSUMER_TOPIC_PREFIX
                + destination.getDestinationTypeAsString() + "."
                + destination.getPhysicalName();
        return new ActiveMQTopic(name);
    }
    
    public static ActiveMQTopic getFastProducerAdvisoryTopic(ActiveMQDestination destination) {
        String name = FAST_PRODUCER_TOPIC_PREFIX
                + destination.getDestinationTypeAsString() + "."
                + destination.getPhysicalName();
        return new ActiveMQTopic(name);
    }
    
    public static ActiveMQTopic getMessageDiscardedAdvisoryTopic(ActiveMQDestination destination) {
        String name = MESSAGE_DISCAREDED_TOPIC_PREFIX
                + destination.getDestinationTypeAsString() + "."
                + destination.getPhysicalName();
        return new ActiveMQTopic(name);
    }
    
    public static ActiveMQTopic getMessageDeliveredAdvisoryTopic(ActiveMQDestination destination) {
        String name = MESSAGE_DELIVERED_TOPIC_PREFIX
                + destination.getDestinationTypeAsString() + "."
                + destination.getPhysicalName();
        return new ActiveMQTopic(name);
    }
    
    public static ActiveMQTopic getMessageConsumedAdvisoryTopic(ActiveMQDestination destination) {
        String name = MESSAGE_CONSUMED_TOPIC_PREFIX
                + destination.getDestinationTypeAsString() + "."
                + destination.getPhysicalName();
        return new ActiveMQTopic(name);
    }
    
    public static ActiveMQTopic getMasterBrokerAdvisoryTopic() {
        return new ActiveMQTopic(MASTER_BROKER_TOPIC_PREFIX);
    }
    
    public static ActiveMQTopic getFullAdvisoryTopic(ActiveMQDestination destination) {
        String name = FULL_TOPIC_PREFIX
                + destination.getDestinationTypeAsString() + "."
                + destination.getPhysicalName();
        return new ActiveMQTopic(name);
    }

    public static ActiveMQTopic getDestinationAdvisoryTopic(ActiveMQDestination destination) {
        switch (destination.getDestinationType()) {
        case ActiveMQDestination.QUEUE_TYPE:
            return QUEUE_ADVISORY_TOPIC;
        case ActiveMQDestination.TOPIC_TYPE:
            return TOPIC_ADVISORY_TOPIC;
        case ActiveMQDestination.TEMP_QUEUE_TYPE:
            return TEMP_QUEUE_ADVISORY_TOPIC;
        case ActiveMQDestination.TEMP_TOPIC_TYPE:
            return TEMP_TOPIC_ADVISORY_TOPIC;
        default:
            throw new RuntimeException("Unknown destination type: " + destination.getDestinationType());
        }
    }

    public static boolean isDestinationAdvisoryTopic(ActiveMQDestination destination) {
        return destination.equals(TEMP_QUEUE_ADVISORY_TOPIC) || destination.equals(TEMP_TOPIC_ADVISORY_TOPIC) || destination.equals(QUEUE_ADVISORY_TOPIC)
               || destination.equals(TOPIC_ADVISORY_TOPIC);
    }

    public static boolean isAdvisoryTopic(ActiveMQDestination destination) {
        return destination.isTopic() && destination.getPhysicalName().startsWith(utf8(ADVISORY_TOPIC_PREFIX));
    }

    public static boolean isConnectionAdvisoryTopic(ActiveMQDestination destination) {
        return destination.equals(CONNECTION_ADVISORY_TOPIC);
    }

    public static boolean isProducerAdvisoryTopic(ActiveMQDestination destination) {
        return destination.isTopic() && destination.getPhysicalName().startsWith(utf8(PRODUCER_ADVISORY_TOPIC_PREFIX));
    }

    public static boolean isConsumerAdvisoryTopic(ActiveMQDestination destination) {
        return destination.isTopic() && destination.getPhysicalName().startsWith(utf8(CONSUMER_ADVISORY_TOPIC_PREFIX));
    }
    
    public static boolean isSlowConsumerAdvisoryTopic(ActiveMQDestination destination) {
        return destination.isTopic() && destination.getPhysicalName().startsWith(utf8(SLOW_CONSUMER_TOPIC_PREFIX));
    }
    
    public static boolean isFastProducerAdvisoryTopic(ActiveMQDestination destination) {
        return destination.isTopic() && destination.getPhysicalName().startsWith(utf8(FAST_PRODUCER_TOPIC_PREFIX));
    }
    
    public static boolean isMessageConsumedAdvisoryTopic(ActiveMQDestination destination) {
        return destination.isTopic() && destination.getPhysicalName().startsWith(utf8(MESSAGE_CONSUMED_TOPIC_PREFIX));
    }
    
    public static boolean isMasterBrokerAdvisoryTopic(ActiveMQDestination destination) {
        return destination.isTopic() && destination.getPhysicalName().startsWith(utf8(MASTER_BROKER_TOPIC_PREFIX));
    }
    
    public static boolean isMessageDeliveredAdvisoryTopic(ActiveMQDestination destination) {
        return destination.isTopic() && destination.getPhysicalName().startsWith(utf8(MESSAGE_DELIVERED_TOPIC_PREFIX));
    }
    
    public static boolean isMessageDiscardedAdvisoryTopic(ActiveMQDestination destination) {
        return destination.isTopic() && destination.getPhysicalName().startsWith(utf8(MESSAGE_DISCAREDED_TOPIC_PREFIX));
    }
    
    public static boolean isFullAdvisoryTopic(ActiveMQDestination destination) {
        return destination.isTopic() && destination.getPhysicalName().startsWith(utf8(FULL_TOPIC_PREFIX));
    }

    /**
     * Returns the agent topic which is used to send commands to the broker
     */
    public static ActiveMQDestination getAgentDestination() {
        return AGENT_TOPIC_DESTINATION;
    }
}
