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
package io.fabric8.mq.autoscaler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.fabric8.mq.autoscaler.EnvUtils.getEnv;

public class MQAutoScalerMain {
    private static final Logger LOG = LoggerFactory.getLogger(MQAutoScalerMain.class);

    public static void main(String args[]) {

        try {
            MQAutoScaler mqAutoScaler = new MQAutoScaler();
            String brokerName = getEnv("AMQ_SERVICE_ID", mqAutoScaler.getBrokerName());
            mqAutoScaler.setBrokerName(brokerName);
            String groupName = getEnv("AMQ_GROUP_NAME", mqAutoScaler.getGroupName());
            mqAutoScaler.setGroupName(groupName);
            String pollTime = getEnv("POLL_TIME", mqAutoScaler.getPollTime());
            mqAutoScaler.setPollTime(Integer.valueOf(pollTime));
            String maximumGroupSize = getEnv("MAX_GROUP_SIZE", mqAutoScaler.getMaximumGroupSize());
            mqAutoScaler.setMaximumGroupSize(Integer.valueOf(maximumGroupSize));
            String minimumGroupSize = getEnv("MIN_GROUP_SIZE", mqAutoScaler.getMinimumGroupSize());
            mqAutoScaler.setMinimumGroupSize(Integer.parseInt(minimumGroupSize));
            String kubernetesMaster = getEnv("KUBERNETES_MASTER", mqAutoScaler.getKubernetesMaster());
            mqAutoScaler.setKubernetesMaster(kubernetesMaster);
            String maxBrokerConnections = getEnv("MAX_BROKER_CONNECTIONS",mqAutoScaler.getMaxConnectionsPerBroker());
            mqAutoScaler.setMaxConnectionsPerBroker(Integer.parseInt(maxBrokerConnections));
            String maxBrokerDestinations = getEnv("MAX_BROKER_DESTINATIONS", mqAutoScaler.getMaxDestinationsPerBroker());
            mqAutoScaler.setMaxDestinationsPerBroker(Integer.parseInt(maxBrokerDestinations));
            String maxDestinationDepth = getEnv("MAX_DESTINATION_DEPTH", mqAutoScaler.getMaxDestinationDepth());
            mqAutoScaler.setMaxDestinationDepth(Integer.parseInt(maxDestinationDepth));
            String maxProducersPerDestination = getEnv("MAX_PRODUCERS_PER_DESTINATION", mqAutoScaler.getMaxProducersPerDestination());
            mqAutoScaler.setMaxProducersPerDestination(Integer.parseInt(maxProducersPerDestination));
            String maxConsumersPerDestination = getEnv("MAX_CONSUMERS_PER_DESTINATION", mqAutoScaler.getMaxConsumersPerDestination());
            mqAutoScaler.setMaxConsumersPerDestination(Integer.parseInt(maxConsumersPerDestination));
            mqAutoScaler.start();

            waiting();
        } catch (Throwable e) {
            LOG.error("Failed to start MQAutoScaler", e);
        }

    }

    static void waiting() {
        while (true) {
            final Object object = new Object();
            synchronized (object) {
                try {
                    object.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

}
