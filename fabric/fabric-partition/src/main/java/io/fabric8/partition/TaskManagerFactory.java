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

package io.fabric8.partition;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferencePolicyOption;
import io.fabric8.api.jcip.GuardedBy;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.partition.internal.DefaultTaskManager;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.collect.Iterables.filter;

/**
 * A {@link ManagedServiceFactory} for creating {@link io.fabric8.partition.internal.DefaultTaskManager} instances.
 */
@ThreadSafe
@Component(name = "io.fabric8.partition", description = "Work Manager Factory", configurationFactory = true, policy = ConfigurationPolicy.REQUIRE)
public final class TaskManagerFactory extends AbstractComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskManagerFactory.class);

    private static final String TASK_ID_PROPERTY_NAME = "id";
    private static final String TASK_DEFINITION_PROPERTY_NAME = "task.definition";
    private static final String PARTITIONS_PATH_PROPERTY_NAME = "partitions.path";
    private static final String WORK_BALANCING_POLICY = "balancing.policy";
    private static final String WORKER_TYPE = "worker.type";

    @Reference(referenceInterface = BalancingPolicy.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private final ConcurrentMap<String, BalancingPolicy> balancingPolicies = new ConcurrentHashMap<String, BalancingPolicy>();
    @Reference(referenceInterface = PartitionListener.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final ConcurrentMap<String, PartitionListener> partitionListeners = new ConcurrentHashMap<String, PartitionListener>();
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();


    private String taskId;
    private String taskDefinition;
    private String partitionsPath;
    private String policyType;
    private String workerType;

    @GuardedBy("this") private TaskManager taskManager;
    @GuardedBy("this") private final ConcurrentMap<String, TaskManager> taskManagers = new ConcurrentHashMap<String, TaskManager>();


    @Activate
    void activate(Map<String,?> configuration) throws ConfigurationException {
        activateInternal(configuration);
        activateComponent();
    }

    @Deactivate
    void deactivate(Map<String,?> configuration) {
        deactivateComponent();
        deactivateInternal();
    }

    private synchronized void activateInternal(Map<String, ?> configuration) throws ConfigurationException {
        validate(configuration);
        String s = readString(configuration, Constants.SERVICE_PID);
        taskId = readString(configuration, TASK_ID_PROPERTY_NAME);
        taskDefinition = readString(configuration, TASK_DEFINITION_PROPERTY_NAME);
        partitionsPath = readString(configuration, PARTITIONS_PATH_PROPERTY_NAME);
        policyType = readString(configuration, WORK_BALANCING_POLICY);
        workerType = readString(configuration, WORKER_TYPE);
        startTaskManager();
    }

    private synchronized void deactivateInternal() {
        stopTaskManager();
    }

    private synchronized void startTaskManager() {
        if (!balancingPolicies.containsKey(policyType)) {
            LOGGER.warn("Policy type {} not found. Will resume: {} when policy is made available.", policyType, taskId);
        } else if (!partitionListeners.containsKey(workerType)) {
            LOGGER.warn("Worker type {} not found. Will resume: {} when worker type is made available.", workerType, taskId);
        } else {
            BalancingPolicy balancingPolicy = balancingPolicies.get(policyType);
            PartitionListener partitionListener = partitionListeners.get(workerType);
            taskManager = new DefaultTaskManager(curator.get(), taskId, taskDefinition, partitionsPath, partitionListener, balancingPolicy);
            taskManager.start();
        }
    }

    private synchronized void stopTaskManager() {
        if (taskManager != null) {
            taskManager.stop();
            taskManager = null;
        }
    }

    /**
     * Validates configuration.
     */
    private void validate(Map<String,?> properties) throws ConfigurationException {
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
     */
    private String readString(Map<String,?> properties, String key) {
        Object obj = properties.get(key);
        if (obj instanceof String) {
            return (String) obj;
        } else {
            return String.valueOf(obj);
        }
    }

    void bindBalancingPolicy(BalancingPolicy balancingPolicy) {
        String type = balancingPolicy.getType();
        balancingPolicies.put(type, balancingPolicy);
        if (type.equals(policyType)) {
            startTaskManager();
        }
    }

    void unbindBalancingPolicy(BalancingPolicy balancingPolicy) {
        String type = balancingPolicy.getType();
        balancingPolicies.remove(type);
        if (type.equals(policyType)) {
            stopTaskManager();
        }

    }

    void bindPartitionListener(PartitionListener partitionListener) {
        String type = partitionListener.getType();
        partitionListeners.put(type, partitionListener);
        if (type.equals(workerType))  {
            startTaskManager();
        }
    }

    void unbindPartitionListener(PartitionListener partitionListener) {
        String type = partitionListener.getType();
        partitionListeners.remove(type);
        if (type.equals(workerType)) {
            stopTaskManager();
        }
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }
}
