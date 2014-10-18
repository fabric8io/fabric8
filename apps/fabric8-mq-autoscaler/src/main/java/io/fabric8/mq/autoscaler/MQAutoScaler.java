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

import io.fabric8.common.util.JMXUtils;
import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ControllerDesiredState;
import io.fabric8.kubernetes.api.model.ManifestContainer;
import io.fabric8.kubernetes.api.model.PodSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;
import io.fabric8.kubernetes.jolokia.JolokiaClients;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pResponse;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MQAutoScaler implements MQAutoScalerMBean {
    private static final Logger LOG = LoggerFactory.getLogger(MQAutoScaler.class);
    private final String DEFAULT_DOMAIN = "io.fabric8";
    private String brokerName = "fabricMQ";
    private String groupName = "default";
    private int producerLimit = 10;
    private int consumerLimit = 10;
    private int pollTime = 5;
    private int minimumGroupSize = 1;
    private int maximumGroupSize = 2;
    private ObjectName MQAutoScalerObjectName;
    private AtomicBoolean started = new AtomicBoolean();
    private JolokiaClients clients;
    private Kubernetes kubernetes;
    private BrokerLimits brokerLimits;
    private DestinationLimits destinationLimits;
    private Timer timer;

    @Override
    public int getConsumerLimit() {
        return consumerLimit;
    }

    @Override
    public void setConsumerLimit(int consumerLimit) {
        this.consumerLimit = consumerLimit;
    }

    @Override
    public int getProducerLimit() {
        return producerLimit;
    }

    @Override
    public void setProducerLimit(int producerLimit) {
        this.producerLimit = producerLimit;
    }

    @Override
    public String getGroupName() {
        return groupName;
    }

    @Override
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public String getBrokerName() {
        return brokerName;
    }

    @Override
    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    @Override
    public int getPollTime() {
        return pollTime;
    }

    @Override
    public void setPollTime(int pollTime) {
        this.pollTime = pollTime;
    }

    public int getMaximumGroupSize() {
        return maximumGroupSize;
    }

    public void setMaximumGroupSize(int maximumGroupSize) {
        this.maximumGroupSize = maximumGroupSize;
    }

    public int getMinimumGroupSize() {
        return minimumGroupSize;
    }

    public void setMinimumGroupSize(int minimumGroupSize) {
        this.minimumGroupSize = minimumGroupSize;
    }

    public void start() throws Exception {
        if (started.compareAndSet(false, true)) {
            MQAutoScalerObjectName = new ObjectName(DEFAULT_DOMAIN, "type", "mq-autoscaler");
            JMXUtils.registerMBean(this, MQAutoScalerObjectName);
            brokerLimits = new BrokerLimits();
            destinationLimits = new DestinationLimits();

            clients = new JolokiaClients();
            kubernetes = clients.getKubernetes();
            timer = new Timer("MQAutoScaler timer");
            long pollTime = getPollTime() * 1000;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    LOG.info("Checking Load across Fabric8MQ group: " + getGroupName());
                    validateMQLoad();
                }
            }, 0, pollTime);

        }
    }

    public void stop() throws Exception {

        if (started.compareAndSet(true, false)) {
            if (MQAutoScalerObjectName != null) {
                JMXUtils.unregisterMBean(MQAutoScalerObjectName);
            }
            if (timer != null) {
                timer.cancel();
            }
        }
    }

    void validateMQLoad() {
        try {
            String selector = "container=java,name=" + getBrokerName() + ",region=" + getGroupName();
            List<BrokerVitalSigns> result = pollBrokers(selector);
            distributeLoad(result);

        } catch (Throwable e) {
            LOG.error("Failed to validate MQ load: ", e);
        }
    }

    void distributeLoad(List<BrokerVitalSigns> brokers) {
        int totalConnections = 0;
        int totalDestinations = 0;
        if (!brokers.isEmpty()) {
            boolean brokerLimitsExceeded = false;
            boolean destinationLimitsExceeded = false;
            for (BrokerVitalSigns brokerVitalSigns : brokers) {
                brokerLimitsExceeded = brokerVitalSigns.areLimitsExceeded(brokerLimits);
                destinationLimitsExceeded = brokerVitalSigns.areLimitsExceeded(destinationLimits);
                totalConnections += brokerVitalSigns.getTotalConnections();
                totalDestinations += brokerVitalSigns.getTotalDestinations();
            }

            if (brokerLimitsExceeded || destinationLimitsExceeded) {

                try {
                    requestDesiredBrokerNumber(getBrokerName(), brokers.size() + 1);
                } catch (Exception e) {
                    LOG.error("Failed to request more brokers ", e);
                }

                if (brokers.size() < getMaximumGroupSize()) {
                    if (destinationLimitsExceeded) {
                        //we can't do much - other than distribute the load for all clients
                        //at this point
                        //bounce the brokers
                        for (BrokerVitalSigns broker : brokers) {
                            try {
                                bounceBroker(broker);
                            } catch (Exception e) {
                                LOG.error("Failed to bounce broker connectors for " + broker.getBrokerName(), e);
                            }
                        }
                    } else {
                        //connection limits exceeded
                        int newSize = brokers.size() + 1;
                        int averageSize = (totalConnections / newSize) + 1;
                        for (BrokerVitalSigns brokerVitalSigns : brokers) {
                            try {
                                bounceConnections(brokerVitalSigns, (brokerVitalSigns.getTotalConnections() - averageSize));
                            } catch (Exception e) {
                                LOG.error("Failed to stop client connections", e);
                            }
                        }
                    }
                }
            } else if (brokers.size() > getMinimumGroupSize()) {
                //see if we have spare capacity - so we can remove a broker(s)

                boolean spareConnectionCapacity = ((totalConnections / brokers.size()) + 1) < brokerLimits.getConnectionsLimit();
                boolean spareDestinationCapacity = ((totalDestinations / brokers.size()) + 1) < brokerLimits.getDestinationsLimit();
                if (spareConnectionCapacity && spareDestinationCapacity) {
                    LOG.info("Scaling down brokers ");

                    try {
                        requestDesiredBrokerNumber(getBrokerName(), brokers.size() - 1);
                    } catch (Exception e) {
                        LOG.error("Failed to request more brokers ", e);
                    }
                }
            }
        }
    }

    List<BrokerVitalSigns> pollBrokers(String selector) {
        List<BrokerVitalSigns> result = new ArrayList<>();
        Map<String, PodSchema> podMap = KubernetesHelper.getPodMap(kubernetes, selector);
        Collection<PodSchema> pods = podMap.values();
        for (PodSchema pod : pods) {
            String host = KubernetesHelper.getHost(pod);
            List<ManifestContainer> containers = KubernetesHelper.getContainers(pod);
            for (ManifestContainer container : containers) {
                LOG.info("Checking pod " + pod.getId() + " container: " + container.getName() + " image: " + container.getImage());
                J4pClient client = clients.jolokiaClient(host, container);
                BrokerVitalSigns brokerVitalSigns = getBrokerVitalSigns(client);
                LOG.debug("Broker vitals for container " + container.getName() + " is: " + brokerVitalSigns);
                result.add(brokerVitalSigns);
            }
        }
        return result;
    }

    private BrokerVitalSigns getBrokerVitalSigns(J4pClient client) {
        BrokerVitalSigns brokerVitalSigns = null;
        ObjectName root = null;
        String attribute = "";
        if (client != null) {

            try {
                root = getBrokerJMXRoot(client);
                attribute = "BrokerName";
                Object brokerName = getAttribute(client, root, attribute);
                brokerVitalSigns = new BrokerVitalSigns(brokerName.toString(), client, root);

                attribute = "TotalConnectionsCount";
                Number result = (Number) getAttribute(client, root, attribute);
                brokerVitalSigns.setTotalConnections(result.intValue());
                populateDestinations(brokerVitalSigns);

            } catch (Throwable e) {
                LOG.error("Unable able to get BrokerVitalSigns from type=" + root + ",attribute: " + attribute, e);
            }
        }
        return brokerVitalSigns;
    }

    /*
* Because, for some reason, we can't really know upfront what random way the ActiveMQ brokerName is set,
* and its critical to use it to find values, we'll do some munging to get the proper root.
*/
    private ObjectName getBrokerJMXRoot(J4pClient client) throws Exception {
        String type = "org.apache.activemq:brokerName=*,type=Broker";
        String attribute = "BrokerName";
        ObjectName objectName = new ObjectName(type);
        J4pResponse<J4pReadRequest> result = client.execute(new J4pReadRequest(objectName, attribute));
        JSONObject jsonObject = result.getValue();
        return new ObjectName(jsonObject.keySet().iterator().next().toString());

    }

    private Object getAttribute(J4pClient client, ObjectName objectName, String attribute) throws Exception {
        J4pResponse<J4pReadRequest> result = client.execute(new J4pReadRequest(objectName, attribute));
        return result.getValue();
    }

    private BrokerVitalSigns populateDestinations(BrokerVitalSigns brokerVitalSigns) throws Exception {
        populateDestinations(DestinationVitalSigns.Type.QUEUE, brokerVitalSigns);
        populateDestinations(DestinationVitalSigns.Type.TOPIC, brokerVitalSigns);
        return brokerVitalSigns;
    }

    private BrokerVitalSigns populateDestinations(DestinationVitalSigns.Type type, BrokerVitalSigns brokerVitalSigns) throws Exception {

        ObjectName root = brokerVitalSigns.getRoot();
        Hashtable<String, String> props = root.getKeyPropertyList();
        props.put("destinationType", type == DestinationVitalSigns.Type.QUEUE ? "Queue" : "Topic");
        props.put("destinationName", "*");
        String objectName = root.getDomain() + ":" + getOrderedProperties(props);

        J4pResponse<J4pReadRequest> response = brokerVitalSigns.getClient().execute(new J4pReadRequest(objectName, "Name", "QueueSize", "ConsumerCount", "ProducerCount"));
        JSONObject value = response.getValue();
        for (Object key : value.keySet()) {
            //get the destinations
            JSONObject jsonObject = (JSONObject) value.get(key);
            String name = jsonObject.get("Name").toString();
            String producerCount = jsonObject.get("ProducerCount").toString().trim();
            String consumerCount = jsonObject.get("ConsumerCount").toString().trim();
            String queueSize = jsonObject.get("QueueSize").toString().trim();

            if (!name.contains("Advisory") && !name.contains(ActiveMQDestination.TEMP_DESTINATION_NAME_PREFIX)) {
                ActiveMQDestination destination = type == DestinationVitalSigns.Type.QUEUE ? new ActiveMQQueue(name) : new ActiveMQTopic(name);
                DestinationVitalSigns destinationVitalSigns = new DestinationVitalSigns(destination);
                destinationVitalSigns.setNumberOfConsumers(Integer.parseInt(consumerCount));
                destinationVitalSigns.setNumberOfProducers(Integer.parseInt(producerCount));
                destinationVitalSigns.setQueueDepth(Integer.parseInt(queueSize));
                brokerVitalSigns.addDestinationVitalSigns(destinationVitalSigns);
            }
        }
        return brokerVitalSigns;
    }

    private String getOrderedProperties(Hashtable<String, String> properties) {
        TreeMap<String, String> map = new TreeMap<>(properties);
        String result = "";
        String separator = "";
        for (Map.Entry<String, String> entry : map.entrySet()) {
            result += separator;
            result += entry.getKey() + "=" + entry.getValue();
            separator = ",";
        }
        return result;
    }

    private void requestDesiredBrokerNumber(String selector, int number) throws Exception {
        Map<String, ReplicationControllerSchema> replicationControllerMap = KubernetesHelper.getReplicationControllerMap(kubernetes, selector);
        Collection<ReplicationControllerSchema> replicationControllers = replicationControllerMap.values();
        for (ReplicationControllerSchema replicationController : replicationControllers) {
            ControllerDesiredState desiredState = replicationController.getDesiredState();
            desiredState.setReplicas(number);
            replicationController.setDesiredState(desiredState);
            kubernetes.updateReplicationController(replicationController.getId(), replicationController);
        }
    }

    private void bounceBroker(BrokerVitalSigns broker) throws Exception {
        ObjectName root = broker.getRoot();
        Hashtable<String, String> props = root.getKeyPropertyList();
        props.put("connector", "clientConnectors");
        props.put("connectorName", "*");
        String objectName = root.getDomain() + ":" + getOrderedProperties(props);

        /**
         * not interested in StatisticsEnabled, just need a real attribute so we can get the root which we
         * can execute against
         */

        List<String> roots = new ArrayList<>();
        J4pResponse<J4pReadRequest> response = broker.getClient().execute(new J4pReadRequest(objectName, "StatisticsEnabled"));
        JSONObject value = response.getValue();
        for (Object key : value.keySet()) {
            roots.add(key.toString());
        }

        for (String key : roots) {
            broker.getClient().execute(new J4pExecRequest(key, "stop"));
        }
        Thread.sleep(1000);
        for (String key : roots) {
            broker.getClient().execute(new J4pExecRequest(key, "start"));
        }

    }

    private void bounceConnections(BrokerVitalSigns broker, int number) throws Exception {
        ObjectName root = broker.getRoot();
        Hashtable<String, String> props = root.getKeyPropertyList();
        props.put("connector", "clientConnectors");
        props.put("connectorName", "*");
        String objectName = root.getDomain() + ":" + getOrderedProperties(props);

        /**
         * not interested in StatisticsEnabled, just need a real attribute so we can get the root which we
         * can execute against
         */

        List<String> connectors = new ArrayList<>();
        J4pResponse<J4pReadRequest> response = broker.getClient().execute(new J4pReadRequest(objectName, "StatisticsEnabled"));
        JSONObject value = response.getValue();
        for (Object key : value.keySet()) {
            connectors.add(key.toString());
        }

        List<String> targets = new ArrayList<>();
        for (String key : connectors) {
            ObjectName on = new ObjectName(key);
            Hashtable<String, String> p = on.getKeyPropertyList();
            p.put("connectionName", "*");
            p.put("connectionViewType", "clientId");
            String clientObjectName = root.getDomain() + ":" + getOrderedProperties(p);
            ObjectName on1 = new ObjectName(clientObjectName);
            J4pResponse<J4pReadRequest> response1 = broker.getClient().execute(new J4pReadRequest(on1, "Slow"));
            JSONObject value1 = response1.getValue();
            for (Object k : value1.keySet()) {
                targets.add(k.toString());
            }
        }

        int count = 0;
        for (String key : targets) {
            broker.getClient().execute(new J4pExecRequest(key, "stop"));
            if (++count >= number) {
                break;
            }
        }

    }

}

