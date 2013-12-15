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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import io.fabric8.groups.GroupListener;
import io.fabric8.groups.Group;
import io.fabric8.groups.internal.ZooKeeperGroup;
import io.fabric8.partition.BalancingPolicy;
import io.fabric8.partition.Partition;
import io.fabric8.partition.PartitionListener;
import io.fabric8.partition.TaskManager;
import io.fabric8.partition.WorkerNode;
import io.fabric8.utils.Closeables;
import io.fabric8.utils.SystemProperties;
import io.fabric8.zookeeper.ZkPath;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;


public class DefaultTaskManager implements TaskManager, GroupListener<WorkerNode>, PathChildrenCacheListener, NodeCacheListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTaskManager.class);

    private final String name = System.getProperty(SystemProperties.KARAF_NAME);
    private final Group<WorkerNode> group;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    private final String taskId;
    private final String taskDefinition;
    private final String partitionPath;
    private final CuratorFramework curator;
    private final PartitionListener partitionListener;
    private final BalancingPolicy balancingPolicy;
    private final ObjectMapper mapper = new ObjectMapper();
    private final TypeReference<HashMap<String, String>> partitionTypeRef = new TypeReference<HashMap<String, String>>() {
    };

    private volatile PathChildrenCache partitionCache;
    private volatile NodeCache workerCache;
    private WorkerNode node;
    private final Set<Partition> assignedPartitions = new LinkedHashSet<Partition>();


    public DefaultTaskManager(CuratorFramework curator, String taskId, String taskDefinition, String partitionPath, PartitionListener partitionListener, BalancingPolicy balancingPolicy) {
        this.curator = checkNotNull(curator, "curator");
        this.taskId = checkNotNull(taskId);
        this.taskDefinition = checkNotNull(taskDefinition);
        this.partitionPath = checkNotNull(partitionPath);
        this.partitionListener = partitionListener;
        this.balancingPolicy = balancingPolicy;
        this.group = new ZooKeeperGroup<WorkerNode>(curator, ZkPath.TASK.getPath(taskId), WorkerNode.class);
        this.group.add(this);
    }

    public void start() {
        node = createNode();
        try {
            workerCache = createWorkerCache();
            workerCache.getListenable().addListener(this);
            workerCache.start(true);
            ZooKeeperUtils.createDefault(curator, partitionPath, null);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        group.update(createNode());
        group.start();
    }

    public void stop() {
        Closeables.closeQuitely(partitionCache);
        Closeables.closeQuitely(workerCache);
        Closeables.closeQuitely(group);
        executorService.shutdown();
        partitionListener.stop(taskId, taskDefinition, assignedPartitions);
        assignedPartitions.clear();
        node = null;
    }

    private synchronized void startWatchingForTasks() {
        try {
            if (partitionCache == null) {
                partitionCache = new PathChildrenCache(curator, partitionPath, true, false, executorService);
                partitionCache.getListenable().addListener(this);
                partitionCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
                partitionCache.rebuild();
                workerCache.rebuild();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to start partition cache.", e);
        }
    }

    private synchronized void stopWatchingForTasks() {
        if (partitionCache != null) {
            try {
                partitionCache.close();
                partitionCache = null;
            } catch (IOException e) {
                LOGGER.error("Failed to stop partition cache.", e);
            }
        }
    }


    @Override
    public void groupEvent(Group<WorkerNode> group, GroupEvent event) {
        switch (event) {
            case CONNECTED:
            case CHANGED:
                WorkerNode state = createNode();
                if (group.isMaster()) {
                    startWatchingForTasks();
                    state.setDefinition(taskDefinition);
                    group.update(state);
                    executorService.submit(new RebalanceTask());
                } else {
                    group.update(state);
                    stopWatchingForTasks();
                }
                break;
            case DISCONNECTED:
                stopWatchingForTasks();
        }
    }

    WorkerNode createNode() {
        WorkerNode state = new WorkerNode(taskId);
        return state;
    }

    /**
     * Creates a {@link NodeCache} for caching the current {@link WorkerNode}
     */
    NodeCache createWorkerCache() throws Exception {
        String fullPath = ZkPath.TASK_MEMBER_PARTITIONS.getPath(name, taskId);
        ZooKeeperUtils.createDefault(curator, fullPath, null);
        return new NodeCache(curator, fullPath);
    }

    WorkerNode readWorkerNode() {
        WorkerNode node;
        String fullPath = ZkPath.TASK_MEMBER_PARTITIONS.getPath(name, taskId);
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

    @Override
    public PartitionListener getPartitionListener() {
        return this.partitionListener;
    }

    @Override
    public BalancingPolicy getBalancingPolicy() {
        return this.balancingPolicy;
    }

    public void rebalance() {
        List<ChildData> childData = partitionCache.getCurrentData();
        int totalItems = childData.size();
        String[] partitions = Lists.transform(childData, ChildDataToPath.INSTANCE).toArray(new String[totalItems]);
        Map<String, WorkerNode> groupMembers = group.members();
        int totalMembers = groupMembers.size();
        String[] members = groupMembers.keySet().toArray(new String[totalMembers]);
        balancingPolicy.rebalance(taskId, partitions, members);
    }

    @Override
    public synchronized void updated(WorkerNode node) {
        Set<Partition> partitions = listNodePartitions(node);
        Set<Partition> added = new LinkedHashSet<Partition>(Sets.difference(partitions, assignedPartitions));
        Set<Partition> removed = new LinkedHashSet<Partition>(Sets.difference(assignedPartitions, partitions));
        assignedPartitions.addAll(added);
        assignedPartitions.removeAll(removed);
        partitionListener.stop(taskId, taskDefinition, removed);
        partitionListener.start(taskId, taskDefinition, added);
    }


    private Set<Partition> listNodePartitions(WorkerNode node) {
        Set<String> paths = node.getPartitions() != null ? Sets.newHashSet(node.getPartitions()) : Sets.<String>newHashSet();
        return Sets.newHashSet(Iterables.transform(paths, new Function<String, Partition>() {
            @Override
            public Partition apply(String input) {
                try {
                    return new PartitionImpl(input, (Map<String, String>) mapper.readValue(curator.getData().forPath(input), partitionTypeRef));
                } catch (Exception e) {
                    LOGGER.warn("Failed to read partition data, using empty configuration instead.");
                    return new PartitionImpl(input, Maps.<String, String>newHashMap());
                }
            }
        }));
    }


    @Override
    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
        switch (event.getType()) {
            case INITIALIZED:
            case CHILD_ADDED:
            case CHILD_REMOVED:
                executorService.submit(new RebalanceTask());
                break;
        }
    }

    @Override
    public synchronized void nodeChanged() throws Exception {
        this.node = readWorkerNode();
        updated(node);
    }

    private class RebalanceTask implements Runnable {
        @Override
        public void run() {
            rebalance();
        }
    }
}
