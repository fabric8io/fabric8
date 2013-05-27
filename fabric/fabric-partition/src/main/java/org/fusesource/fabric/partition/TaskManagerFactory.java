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

package org.fusesource.fabric.partition;

import com.google.common.base.Throwables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.apache.curator.framework.CuratorFramework;
import org.fusesource.fabric.partition.internal.WorkManagerWithBalancingPolicy;
import org.fusesource.fabric.partition.internal.DefaultTaskManager;
import org.fusesource.fabric.partition.internal.WorkManagerWithListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.collect.Iterables.filter;

/**
 * A {@link ManagedServiceFactory} for creating {@link org.fusesource.fabric.partition.internal.DefaultTaskManager} instances.
 */
public class TaskManagerFactory implements ManagedServiceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskManagerFactory.class);

    private static final String TASK_ID_PROPERTY_NAME = "id";
    private static final String TASK_DEFINITION_PROPERTY_NAME = "task.definition";
    private static final String PARTITIONS_PATH_PROPERTY_NAME = "partitions.path";
    private static final String WORK_BALANCING_POLICY = "balancing.policy";
    private static final String WORKER_TYPE = "worker.type";

    private final ConcurrentMap<String, TaskManager> taksManagers = new ConcurrentHashMap<String, TaskManager>();
    private final ConcurrentMap<String, BalancingPolicy> balancingPolicies = new ConcurrentHashMap<String, BalancingPolicy>();
    private final ConcurrentMap<String, PartitionListener> partitionListeners = new ConcurrentHashMap<String, PartitionListener>();


    private final Multimap<String, String> waitingOnBalancing = LinkedHashMultimap.create();
    private final Multimap<String, String> waitingOnListener = LinkedHashMultimap.create();
    private final Map<String, Dictionary> pendingPids = new HashMap<String, Dictionary>();

    private BundleContext bundleContext;
    private CuratorFramework curator;


    public synchronized void destroy() {

        for (Map.Entry<String, TaskManager> entry : taksManagers.entrySet()) {
            TaskManager taskManager = entry.getValue();
            taskManager.stop();
        }
        taksManagers.clear();
        balancingPolicies.clear();
        partitionListeners.clear();
    }

    @Override
    public String getName() {
        return "Work Manager Factory";
    }

    @Override
    public synchronized void updated(String s, Dictionary<String, ?> properties) throws ConfigurationException {
        validate(properties);
        String taskId = readString(properties, TASK_ID_PROPERTY_NAME);
        String taskDefinition = readString(properties, TASK_DEFINITION_PROPERTY_NAME);
        String partitionsPath = readString(properties, PARTITIONS_PATH_PROPERTY_NAME);
        String policyType = readString(properties, WORK_BALANCING_POLICY);
        String workerType = readString(properties, WORKER_TYPE);

        if (!balancingPolicies.containsKey(policyType)) {
            waitingOnBalancing.put(policyType, s);
            pendingPids.put(s, properties);
            LOGGER.warn("Policy type {} not found. Will resume: {} when policy is made available.", policyType, s);
        } else if (!partitionListeners.containsKey(workerType)) {
            waitingOnListener.put(workerType, s);
            pendingPids.put(s, properties);
            LOGGER.warn("Worker type {} not found. Will resume: {} when worker type is made available.", workerType, s);
        } else {
            BalancingPolicy balancingPolicy = balancingPolicies.get(policyType);
            PartitionListener partitionListener = partitionListeners.get(workerType);

            TaskManager taskManager = new DefaultTaskManager(curator, taskId, taskDefinition, partitionsPath, partitionListener, balancingPolicy);
            TaskManager oldTaskManager = taksManagers.put(s, taskManager);
            if (oldTaskManager != null) {
                oldTaskManager.stop();
            }
            taskManager.start();
        }
    }

    @Override
    public void deleted(String s) {
        TaskManager taskManager = taksManagers.remove(s);
        taskManager.stop();
    }


    /**
     * Validates configuration.
     * @param properties
     * @throws ConfigurationException
     */
    private void validate(Dictionary<String, ?> properties) throws ConfigurationException {
        if (properties == null) {
            throw new IllegalArgumentException("Configuration is null");
        } else if (properties.get(TASK_ID_PROPERTY_NAME) == null) {
            throw new ConfigurationException(TASK_ID_PROPERTY_NAME, "Property is required.");
        } else if (properties.get(TASK_DEFINITION_PROPERTY_NAME) == null) {
            throw new ConfigurationException(TASK_DEFINITION_PROPERTY_NAME, "Property is required.");
        } else if (properties.get(PARTITIONS_PATH_PROPERTY_NAME) == null) {
            throw new ConfigurationException(PARTITIONS_PATH_PROPERTY_NAME, "Property is required.");
        } else if (properties.get(WORK_BALANCING_POLICY) == null) {
            throw new ConfigurationException(WORK_BALANCING_POLICY, "Property is required.");
        } else if (properties.get(WORKER_TYPE) == null) {
            throw new ConfigurationException(WORKER_TYPE, "Property is required.");
        }
    }

    /**
     * Reads the specified key as a String from configuration.
     * @param dictionary
     * @param key
     * @return
     */
    private String readString(Dictionary dictionary, String key) {
        Object obj = dictionary.get(key);
        if (obj instanceof String) {
            return (String) obj;
        } else {
            return String.valueOf(obj);
        }
    }

    private void stopWorkManagerWithBalancingPolicy(Iterable<TaskManager> workManagers, String balancingPolicyType) {
        for (TaskManager taskManager : filter(workManagers, new WorkManagerWithBalancingPolicy(balancingPolicyType))) {
            taskManager.stop();
        }
    }

    private void stopWorkManagerWithListener(Iterable<TaskManager> workManagers, String listenerType) {
        for (TaskManager taskManager : filter(workManagers, new WorkManagerWithListener(listenerType))) {
            taskManager.stop();
        }
    }

    public synchronized void bindPolicy(ServiceReference<BalancingPolicy> reference) {
        BalancingPolicy balancingPolicy = bundleContext.getService(reference);
        if (balancingPolicy != null) {
            balancingPolicies.put(balancingPolicy.getType(), balancingPolicy);
            for (String pid : waitingOnBalancing.get(balancingPolicy.getType())) {
                try {
                    updated(pid, pendingPids.remove(pid));
                } catch (ConfigurationException e) {
                    Throwables.propagate(e);
                }
            }
        }
    }

    public synchronized void unbindPolicy(ServiceReference<BalancingPolicy> reference) {
        String type = (String) reference.getProperty("type");
        balancingPolicies.remove(type);
        stopWorkManagerWithBalancingPolicy(taksManagers.values(), type);
    }

    public synchronized void bindWorkListener(ServiceReference<PartitionListener> reference) {
        PartitionListener partitionListener = bundleContext.getService(reference);
        if (partitionListener != null) {
            partitionListeners.put(partitionListener.getType(), partitionListener);
            for (String pid : waitingOnListener.get(partitionListener.getType())) {
                try {
                    updated(pid, pendingPids.remove(pid));
                } catch (ConfigurationException e) {
                    Throwables.propagate(e);
                }
            }
        }
    }

    public synchronized void unbindWorkListener(ServiceReference<PartitionListener> reference) {
        String type = (String) reference.getProperty("type");
        partitionListeners.remove(type);
        stopWorkManagerWithListener(taksManagers.values(), type);
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
