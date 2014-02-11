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

import io.fabric8.api.scr.Configurer;
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

import java.util.Map;


@ThreadSafe
@Component(name = "io.fabric8.partition", label = "Fabric8 Work Manager Factory", configurationFactory = true, policy = ConfigurationPolicy.REQUIRE, metatype = true)
public final class PartitionManager extends AbstractComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(PartitionManager.class);

    public static final String TASK_ID = "id";
    public static final String WORKITEM_PATH = "workItemPath";
    public static final String WORKITEM_REPO_TYPE = "workItemRepositoryFactory.target";
    public static final String WORK_BALANCING_POLICY = "balancingPolicy.target";
    public static final String WORKER_TYPE = "worker.target";

    @Reference
    private Configurer configurer;
    @Reference(name = "balancingPolicy", referenceInterface = BalancingPolicy.class)
    private final ValidatingReference<BalancingPolicy> balancingPolicy = new ValidatingReference<BalancingPolicy>();

    @Reference(name = "worker", referenceInterface = Worker.class)
    private final ValidatingReference<Worker> worker = new ValidatingReference<Worker>();

    @Reference(name = "workItemRepositoryFactory", referenceInterface = WorkItemRepositoryFactory.class)
    private final ValidatingReference<WorkItemRepositoryFactory> workItemRepositoryFactory = new ValidatingReference<WorkItemRepositoryFactory>();

    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    @Property(name = TASK_ID, label = "Task ID", description = "A unique identifier for the task")
    private String id;
    @Property(name = WORKITEM_REPO_TYPE, label = "Repository Factory Filter", description = "Ldap filter for repository factory. Factory defines where and how the work items will be read")
    private String workItemRepositoryFactoryTarget;
    @Property(name = WORKITEM_PATH, label = "Work Item Path", description = "The path where the work items are located")
    private String workItemPath;
    @Property(name = WORK_BALANCING_POLICY, label = "Policy Filter", description = "Ldap filter for the balancing Policy. Balancing Policy defines how the work items will be distributed")
    private String balancingPolicyTarget;
    @Property(name = WORKER_TYPE, label = "Worker Filter", description = "Ldap filter for the Worker to use. Worker defines how each item will be processed")
    private String workerTarget;
    @Property(name = "name", label = "Container Name", description = "The name of the container", value = "${karaf.name}", propertyPrivate = true)
    private String name;


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
        taskConfiguration = configuration;
        configurer.configure(taskConfiguration, this);
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
            coordinator = new TaskCoordinator(new TaskContextImpl(id, taskConfiguration), workItemRepositoryFactory.get().build(workItemPath), balancingPolicy.get(), curator.get());
            coordinator.start();
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
                LOGGER.info("Starting Task Handler type {} for task {}.", workerTarget, id);
                taskHandler = new TaskHandler(name, new TaskContextImpl(id, taskConfiguration), curator.get(), worker.get(), workItemRepositoryFactory.get().build(workItemPath));
                taskHandler.start();
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

    void bindBalancingPolicy(BalancingPolicy service) {
        balancingPolicy.bind(service);
    }

    void unbindBalancingPolicy(BalancingPolicy service) {
        balancingPolicy.unbind(service);
    }

    void bindWorker(Worker service) {
       worker.bind(service);
    }

    void unbindWorker(Worker service) {
        worker.unbind(service);
    }

    void bindWorkItemRepositoryFactory(WorkItemRepositoryFactory service) {
        workItemRepositoryFactory.bind(service);
    }

    void unbindWorkItemRepositoryFactory(WorkItemRepositoryFactory service) {
        workItemRepositoryFactory.unbind(service);
    }

    void bindCurator(CuratorFramework service) {
        this.curator.bind(service);
    }

    void unbindCurator(CuratorFramework service) {
        this.curator.unbind(service);
    }
}
