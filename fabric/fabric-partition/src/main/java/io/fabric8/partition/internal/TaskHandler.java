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

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import io.fabric8.api.FabricException;
import io.fabric8.partition.TaskContext;
import io.fabric8.partition.WorkItem;
import io.fabric8.partition.WorkItemRepository;
import io.fabric8.partition.Worker;
import io.fabric8.partition.WorkerNode;
import io.fabric8.utils.SystemProperties;
import io.fabric8.zookeeper.ZkPath;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public class TaskHandler implements NodeCacheListener {

    private final Logger LOGGER = LoggerFactory.getLogger(TaskHandler.class);


    private final ObjectMapper mapper = new ObjectMapper();
    private final TypeReference<HashMap<String, String>> partitionTypeRef = new TypeReference<HashMap<String, String>>() {
    };
    private final Set<WorkItem> assignedWorkItems = new LinkedHashSet<WorkItem>();

    private final CuratorFramework curator;
    private final NodeCache cache;
    private final Worker worker;
    private final WorkItemRepository repository;
    private final TaskContext context;
    private final String workerPath;
    private final String name;

    public TaskHandler(String name, TaskContext context, CuratorFramework curator, Worker worker, WorkItemRepository repository) {
        this.name = name;
        this.context = context;
        this.curator = curator;
        this.worker = worker;
        this.repository = repository;
        this.workerPath = ZkPath.TASK_MEMBER_PARTITIONS.getPath(name, context.getId());
        this.cache = new NodeCache(curator, workerPath);
    }

    public void start() {
        try {
            ZooKeeperUtils.createDefault(curator, workerPath, null);
            cache.getListenable().addListener(this);
            cache.start(true);
            cache.rebuild();
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    public void stop() {
        worker.stop(context);
        cache.getListenable().removeListener(this);
        try {
            cache.close();
        } catch (IOException e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public synchronized void nodeChanged() throws Exception {
        LOGGER.info("Task Handler for {} detected change.", context.getId());
        WorkerNode node = readWorkerNode();
        Set<WorkItem> workItems = listWorkItemsOfNode(node);
        Set<WorkItem> added = new LinkedHashSet<WorkItem>(Sets.difference(workItems, assignedWorkItems));
        Set<WorkItem> removed = new LinkedHashSet<WorkItem>(Sets.difference(assignedWorkItems, workItems));
        assignedWorkItems.addAll(added);
        assignedWorkItems.removeAll(removed);
        LOGGER.info("Releasing work items: {}.", removed.toArray());
        worker.release(context , removed);
        LOGGER.info("Assigning work items: {}.", added.toArray());
        worker.assign(context, added);
    }

    /**
     * Each container for any given tasks has a {@link WorkerNode} that contains the list of assigned partitions.
     * This method reads and returns that node.
     *
     * @return The {@link WorkerNode} that represents the Worker of the current container for the current task.
     */
    WorkerNode readWorkerNode() {
        WorkerNode node;
        String fullPath = ZkPath.TASK_MEMBER_PARTITIONS.getPath(name, context.getId());
        try {
            byte[] bytes = curator.getData().forPath(fullPath);
            if (bytes != null) {
                node = mapper.readValue(bytes, WorkerNode.class);
            } else {
                node = createNode();
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        return node;
    }

    WorkerNode createNode() {
        WorkerNode state = new WorkerNode(context.getId());
        return state;
    }

    /**
     * List the {@link io.fabric8.partition.WorkItem} items that has been assigned to the specified {@link WorkerNode}.
     * @param node The {@link WorkerNode} that represents a signel Worker.
     * @return A {@link Set} of {@link io.fabric8.partition.WorkItem} items.
     */
    private Set<WorkItem> listWorkItemsOfNode(WorkerNode node) {
        Set<String> locations = node.getItems() != null ? Sets.newHashSet(node.getItems()) : Sets.<String>newHashSet();
        return Sets.newHashSet(Iterables.transform(locations, new Function<String, WorkItem>() {
            @Override
            public WorkItem apply(String input) {
                    return repository.readWorkItem(input);
            }
        }));
    }
}
