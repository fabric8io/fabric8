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

import io.fabric8.api.scr.support.ConfigInjection;
import io.fabric8.partition.BalancingPolicy;
import io.fabric8.partition.WorkItemRepository;
import io.fabric8.partition.WorkItemRepositoryFactory;
import io.fabric8.partition.Worker;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.*;
import io.fabric8.api.jcip.GuardedBy;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@ThreadSafe
@Component(name = "io.fabric8.partition", label = "Fabric8 Work Manager Factory", configurationFactory = true, policy = ConfigurationPolicy.REQUIRE, metatype = true)
public final class PartitionManager extends AbstractComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(PartitionManager.class);

    public static final String TASK_ID = "id";
    public static final String WORKITEM_PATH = "workItemPath";
    public static final String WORKITEM_REPO_TYPE = "workItemRepositoryType";
    public static final String WORK_BALANCING_POLICY = "balancingPolicyType";
    public static final String WORKER_TYPE = "workerType";

    @Reference(referenceInterface = BalancingPolicy.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private final ConcurrentMap<String, BalancingPolicy> balancingPolicies = new ConcurrentHashMap<String, BalancingPolicy>();

    @Reference(referenceInterface = Worker.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final ConcurrentMap<String, Worker> workers = new ConcurrentHashMap<String, Worker>();

    @Reference(referenceInterface = WorkItemRepositoryFactory.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final ConcurrentMap<String, WorkItemRepositoryFactory> repositoryFactories = new ConcurrentHashMap<String, WorkItemRepositoryFactory>();

    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    @Property(name = TASK_ID, label = "Task ID", description = "A unique identifier for the task")
    private String id;
    @Property(name = WORKITEM_REPO_TYPE, label = "Repository Type", description = "Defines where and how the work items will be read")
    private String workItemRepositoryType;
    @Property(name = WORKITEM_PATH, label = "Work Item Path", description = "The path where the work items are located")
    private String workItemPath;
    @Property(name = WORK_BALANCING_POLICY, label = "Policy Type", description = "Defines how the work items will be distributed")
    private String balancingPolicyType;
    @Property(name = WORKER_TYPE, label = "Repository Worker", description = "Defines how each assigned work item will be processed")
    private String workerType;


    private Map<String, ?> taskConfiguration;

    @GuardedBy("this") private WorkItemRepository repository;
    @GuardedBy("this") private TaskCoordinator coordinator;
    @GuardedBy("this") private TaskHandler taskHandler;

    @Activate
    void activate(Map<String,?> configuration) throws Exception {
        activateInternal(configuration);
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        deactivateInternal();
    }

    private synchronized void activateInternal(Map<String, ?> configuration) throws Exception {
        validate(configuration);
        ConfigInjection.applyConfiguration(configuration, this);
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
            BalancingPolicy balancingPolicy = balancingPolicies.get(balancingPolicyType);
            WorkItemRepositoryFactory repositoryFactory = repositoryFactories.get(workItemRepositoryType);

            if (balancingPolicies == null) {
                LOGGER.warn("Policy type {} not found. Task Coordinator will resume: {} when policy is made available.", balancingPolicyType, id);
            } else if (repositoryFactory == null) {
                LOGGER.warn("Repository type {} not found. Task Coordinator will resume: {} when worker type is made available.", workItemRepositoryType, id);
            } else {
                LOGGER.info("Starting Task Coordinator with repository {} for task {}.", workItemRepositoryType, id);
                if (repository == null) {
                    repository = repositoryFactory.build(workItemPath);
                }
                coordinator = new TaskCoordinator(new TaskContextImpl(id, taskConfiguration), repository, balancingPolicy, curator.get());
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
            WorkItemRepositoryFactory repositoryFactory = repositoryFactories.get(workItemRepositoryType);

            if (worker == null) {
                LOGGER.warn("Worker type {} not found. Task Handler will resume: {} when worker type is made available.", workerType, id);
            } else if (repositoryFactory == null) {
                LOGGER.warn("Repository type {} not found. Task Handler will resume: {} when worker type is made available.", workItemRepositoryType, id);
            } else {
                if (repository == null) {
                    repository = repositoryFactory.build(workItemPath);
                }
                LOGGER.info("Starting Task Handler type {} for task {}.", workerType, id);
                taskHandler = new TaskHandler(new TaskContextImpl(id, taskConfiguration), curator.get(), worker, repository);
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
        } else if (properties.get(TASK_ID) == null) {
            throw new ConfigurationException(TASK_ID, "Property is required.");
        } else if (properties.get(WORKITEM_PATH) == null) {
            throw new ConfigurationException(WORKITEM_PATH, "Property is required.");
        } else if (properties.get(WORK_BALANCING_POLICY) == null) {
            throw new ConfigurationException(WORK_BALANCING_POLICY, "Property is required.");
        } else if (properties.get(WORKER_TYPE) == null) {
            throw new ConfigurationException(WORKER_TYPE, "Property is required.");
        }
    }

    void bindBalancingPolicy(BalancingPolicy balancingPolicy) {
        String type = balancingPolicy.getType();
        balancingPolicies.put(type, balancingPolicy);
        if (type.equals(balancingPolicyType)) {
            startWorkHandler();
        }
    }

    void unbindBalancingPolicy(BalancingPolicy balancingPolicy) {
        String type = balancingPolicy.getType();
        balancingPolicies.remove(type);
        if (type.equals(balancingPolicyType)) {
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
        if (type.equals(workItemRepositoryType))  {
            startCoordinator();
            startWorkHandler();
        }
    }

    void unbindWorkItemRepositoryFactory(WorkItemRepositoryFactory factory) {
        String type = factory.getType();
        repositoryFactories.remove(type);
        if (type.equals(workItemRepositoryType)) {
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
