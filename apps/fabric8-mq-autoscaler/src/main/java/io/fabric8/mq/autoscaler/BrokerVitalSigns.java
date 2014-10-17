/*
 *
 *  * Copyright 2005-2014 Red Hat, Inc.
 *  * Red Hat licenses this file to you under the Apache License, version
 *  * 2.0 (the "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  * implied.  See the License for the specific language governing
 *  * permissions and limitations under the License.
 *
 */

package io.fabric8.mq.autoscaler;

import org.apache.activemq.command.ActiveMQDestination;
import org.jolokia.client.J4pClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BrokerVitalSigns {
    private static final Logger LOG = LoggerFactory.getLogger(BrokerVitalSigns.class);

    private final String brokerName;
    private int totalConnections;
    private int totalDestinations;
    private boolean blockedProducers;
    private Map<ActiveMQDestination, DestinationVitalSigns> queueVitalSigns = new ConcurrentHashMap<>();
    private Map<ActiveMQDestination, DestinationVitalSigns> topicVitalSigns = new ConcurrentHashMap<>();
    private final J4pClient client;
    private final ObjectName root;

    public BrokerVitalSigns(String brokerName, J4pClient client, ObjectName root) {
        this.brokerName = brokerName;
        this.client = client;
        this.root = root;
    }

    public int getTotalConnections() {
        return totalConnections;
    }

    public void setTotalConnections(int totalConnections) {
        this.totalConnections = totalConnections;
    }

    public int getTotalDestinations() {
        return queueVitalSigns.size() + topicVitalSigns.size();
    }

    public boolean isBlockedProducers() {
        return blockedProducers;
    }

    public void setBlockedProducers(boolean blockedProducers) {
        this.blockedProducers = blockedProducers;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public J4pClient getClient() {
        return client;
    }

    public ObjectName getRoot() {
        return root;
    }

    public void addDestinationVitalSigns(DestinationVitalSigns destinationVitalSigns) {
        if (destinationVitalSigns.getDestination().isQueue()) {
            queueVitalSigns.put(destinationVitalSigns.getDestination(), destinationVitalSigns);
        } else {
            topicVitalSigns.put(destinationVitalSigns.getDestination(), destinationVitalSigns);
        }
    }

    public boolean areLimitsExceeded(BrokerLimits brokerLimits) {
        int totalConnections = getTotalConnections();
        boolean connectionsExceeded = totalConnections > brokerLimits.getConnectionsLimit();
        if (connectionsExceeded) {
            LOG.info("Broker " + getBrokerName() + " exceeded connection limits(" + brokerLimits.getConnectionsLimit() + ") with " + totalConnections + " connections");
        }

        int totalDestinations = getTotalDestinations();
        boolean destinationsExceeded = totalDestinations > brokerLimits.getDestinationsLimit();

        if (destinationsExceeded) {
            LOG.info("Broker " + getBrokerName() + " exceeded destination limits(" + brokerLimits.getDestinationsLimit() + ") with " + totalDestinations + " destinations");
        }
        return connectionsExceeded || destinationsExceeded;
    }

    public boolean areLimitsExceeded(DestinationLimits destinationLimits) {
        boolean limitsExceeded = areLimitsExceeded(topicVitalSigns, destinationLimits);
        limitsExceeded |= areLimitsExceeded(queueVitalSigns, destinationLimits);
        return limitsExceeded;
    }

    private boolean areLimitsExceeded(Map<ActiveMQDestination, DestinationVitalSigns> map, DestinationLimits destinationLimits) {
        boolean limitsExceeded = false;
        for (Map.Entry<ActiveMQDestination, DestinationVitalSigns> entry : map.entrySet()) {
            DestinationVitalSigns destinationVitalSigns = entry.getValue();
            // iterate through the list, as we log info about destinations that have been exceeded
            limitsExceeded |= destinationVitalSigns.areLimitsExceeded(this, destinationLimits);
        }
        return limitsExceeded;
    }

    public String toString() {
        String result = "BrokerVitalSigns(" + getBrokerName() + ") connections=" + getTotalConnections() + ",destinations=" +
                            getTotalDestinations() + ",blockedProducers=" + isBlockedProducers();
        if (!topicVitalSigns.isEmpty()) {
            result += System.lineSeparator();
            result += "\tTopics=";
            String separator = "";
            for (DestinationVitalSigns destinationVitalSigns : topicVitalSigns.values()) {
                result += separator;
                result += destinationVitalSigns.toString();
                separator += ",";
            }
        }
        if (!queueVitalSigns.isEmpty()) {
            result += System.lineSeparator();
            result += "\tQueues=";
            String separator = "";
            for (DestinationVitalSigns destinationVitalSigns : queueVitalSigns.values()) {
                result += separator;
                result = destinationVitalSigns.toString();
                separator += ",";
            }
        }
        return result;
    }

}
