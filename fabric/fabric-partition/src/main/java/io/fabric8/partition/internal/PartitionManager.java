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

package io.fabric8.partition.internal;

import io.fabric8.partition.BalancingPolicy;
import io.fabric8.partition.WorkItemRepository;
import io.fabric8.partition.WorkItemRepositoryFactory;
import io.fabric8.partition.Worker;
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
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@ThreadSafe
@Component(name = "io.fabric8.partition", description = "Work Manager Factory", configurationFactory = true, policy = ConfigurationPolicy.REQUIRE)
public final class PartitionManager extends AbstractComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(PartitionManager.class);

    public static final String TASK_ID_PROPERTY_NAME = "id";
    public static final String TASK_DEFINITION_PROPERTY_NAME = "task.definition";
    public static final String WORKITEM_PATH_PROPERTY_NAME = "workitem.path";
    public static final String WORKITEM_REPO_TYPE_PROPERTY_NAME = "workitem.repository.type";
    public static final String WORK_BALANCING_POLICY = "balancing.policy";
    public static final String WORKER_TYPE = "worker.type";

    @Reference(referenceInterface = BalancingPolicy.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private final ConcurrentMap<String, BalancingPolicy> balancingPolicies = new ConcurrentHashMap<String, BalancingPolicy>();

    @Reference(referenceInterface = Worker.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final ConcurrentMap<String, Worker> workers = new ConcurrentHashMap<String, Worker>();

    @Reference(referenceInterface = WorkItemRepositoryFactory.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final ConcurrentMap<String, WorkItemRepositoryFactory> repositoryFactories = new ConcurrentHashMap<String, WorkItemRepositoryFactory>();

    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    private String taskId;
    private String taskDefinition;
    private String partitionsPath;
    private String policyType;
    private String workerType;
    private String repositoryType;

    @GuardedBy("this") private WorkItemRepository repository;
    @GuardedBy("this") private TaskCoordinator coordinator;
    @GuardedBy("this") private TaskHandler taskHandler;

    @Activate
    void activate(Map<String,?> configuration) throws ConfigurationException {
        activateInternal(configuration);
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        deactivateInternal();
    }

    private synchronized void activateInternal(Map<String, ?> configuration) throws ConfigurationException {
        validate(configuration);
        taskId = readString(configuration, TASK_ID_PROPERTY_NAME);
        taskDefinition = readString(configuration, TASK_DEFINITION_PROPERTY_NAME);
        partitionsPath = readString(configuration, WORKITEM_PATH_PROPERTY_NAME);
        policyType = readString(configuration, WORK_BALANCING_POLICY);
        workerType = readString(configuration, WORKER_TYPE);
        repositoryType = readString(configuration, WORKITEM_REPO_TYPE_PROPERTY_NAME);
        startCoordinator();
        startWorkHandler();
    }

    private synchronized void deactivateInternal() {
        stopCoordinator();
        stopWorkHandler();
        stopRepository();
    }

    private synchronized void startCoordinator() {
        if (coordinator == null) {
            BalancingPolicy balancingPolicy = balancingPolicies.get(policyType);
            WorkItemRepositoryFactory repositoryFactory = repositoryFactories.get(repositoryType);

            if (balancingPolicies == null) {
                LOGGER.warn("Policy type {} not found. Task Coordinator will resume: {} when policy is made available.", policyType, taskId);
            } else if (repositoryFactory == null) {
                LOGGER.warn("Repository type {} not found. Task Coordinator will resume: {} when worker type is made available.", repositoryType, taskId);
            } else {
                LOGGER.info("Starting Task Coordinator with repository {} for task {}.", repositoryType, taskId);
                if (repository == null) {
                    repository = repositoryFactory.build(partitionsPath);
                }
                coordinator = new TaskCoordinator(new TaskContextImpl(taskId, taskDefinition), repository, balancingPolicy, curator.get());
                coordinator.start();
            }
        }
    }

    private synchronized void stopCoordinator() {
        if (coordinator != null) {
            coordinator.stop();
            coordinator = null;
        }
    }

    private synchronized void startWorkHandler() {
        if (taskHandler == null) {
            Worker worker = workers.get(workerType);
            WorkItemRepositoryFactory repositoryFactory = repositoryFactories.get(repositoryType);

            if (worker == null) {
                LOGGER.warn("Worker type {} not found. Task Handler will resume: {} when worker type is made available.", workerType, taskId);
            } else if (repositoryFactory == null) {
                LOGGER.warn("Repository type {} not found. Task Handler will resume: {} when worker type is made available.", repositoryType, taskId);
            } else {
                if (repository == null) {
                    repository = repositoryFactory.build(partitionsPath);
                }
                LOGGER.info("Starting Task Handler type {} for task {}.", workerType, taskId);
                taskHandler = new TaskHandler(new TaskContextImpl(taskId, taskDefinition), curator.get(), worker, repository);
                taskHandler.start();
            }
        }
    }

    private synchronized void stopWorkHandler() {
        if (taskHandler != null) {
            taskHandler.stop();
            taskHandler = null;
        }
    }

    private synchronized void stopRepository() {
        if (repository != null) {
            repository.close();
            repository = null;
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
        } else if (properties.get(WORKITEM_PATH_PROPERTY_NAME) == null) {
            throw new ConfigurationException(WORKITEM_PATH_PROPERTY_NAME, "Property is required.");
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
            startWorkHandler();
        }
    }

    void unbindBalancingPolicy(BalancingPolicy balancingPolicy) {
        String type = balancingPolicy.getType();
        balancingPolicies.remove(type);
        if (type.equals(policyType)) {
            stopWorkHandler();
        }

    }

    void bindWorker(Worker worker) {
        String type = worker.getType();
        workers.put(type, worker);
        if (type.equals(workerType))  {
            startWorkHandler();
        }
    }

    void unbindWorker(Worker worker) {
        String type = worker.getType();
        workers.remove(type);
        if (type.equals(workerType)) {
            stopWorkHandler();
        }
    }

    void bindWorkItemRepositoryFactory(WorkItemRepositoryFactory factory) {
        String type = factory.getType();
        repositoryFactories.put(type, factory);
        if (type.equals(repositoryType))  {
            startCoordinator();
            startWorkHandler();
        }
    }

    void unbindWorkItemRepositoryFactory(WorkItemRepositoryFactory factory) {
        String type = factory.getType();
        repositoryFactories.remove(type);
        if (type.equals(repositoryType)) {
            stopCoordinator();
            stopWorkHandler();
            stopRepository();
        }
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }
}
