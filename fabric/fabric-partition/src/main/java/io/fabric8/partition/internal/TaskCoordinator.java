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

import io.fabric8.api.FabricException;
import io.fabric8.groups.Group;
import io.fabric8.groups.GroupListener;
import io.fabric8.groups.internal.ZooKeeperGroup;
import io.fabric8.partition.BalancingPolicy;
import io.fabric8.partition.TaskContext;
import io.fabric8.partition.WorkItemListener;
import io.fabric8.partition.WorkItemRepository;
import io.fabric8.partition.WorkerNode;
import io.fabric8.zookeeper.ZkPath;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskCoordinator implements GroupListener<WorkerNode>, WorkItemListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskCoordinator.class);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final TaskContext context;
    private final WorkItemRepository repository;
    private final BalancingPolicy balancingPolicy;
    private final Group<WorkerNode> group;
    private final CuratorFramework curator;


    public TaskCoordinator(TaskContext context, WorkItemRepository repository, BalancingPolicy balancingPolicy, CuratorFramework curator) {
        this.context = context;
        this.repository = repository;
        this.balancingPolicy = balancingPolicy;
        this.curator = curator;
        this.group = new ZooKeeperGroup<WorkerNode>(curator, ZkPath.TASK.getPath(context.getId()), WorkerNode.class);
    }

    public void start() {
        group.add(this);
        repository.addListener(this);
        group.start();
    }

    public void stop() {
        repository.removeListener(this);
        try {
            group.close();
        } catch (IOException e) {
            throw FabricException.launderThrowable(e);
        }
    }

    WorkerNode createNode() {
        WorkerNode state = new WorkerNode(context.getId());
        return state;
    }

    @Override
    public void groupEvent(Group<WorkerNode> group, GroupEvent event) {
        switch (event) {
            case CONNECTED:
            case CHANGED:
                WorkerNode state = createNode();
                if (group.isMaster()) {
                    repository.start();
                    state.setServices(new String[] {context.getId()});
                    group.update(state);
                    partitionUpdated();
                } else {
                    group.update(state);
                    repository.stop();
                }
                break;
            case DISCONNECTED:
                repository.stop();
        }
    }

    @Override
    public void partitionUpdated() {
        LOGGER.info("Rebalancing work for {}.", context.getId());
         executorService.submit(new RebalanceTask());
    }


    /**
     * Re-balances all available {@link io.fabric8.partition.WorkItem} items to all available Workers.
     */
    public void rebalance() {
        List<String> workItems = repository.listWorkItemLocations();
        Set<String> members = group.members().keySet();
        balancingPolicy.rebalance(context, workItems, members);
    }

    private class RebalanceTask implements Runnable {
        @Override
        public void run() {
            rebalance();
        }
    }
}
