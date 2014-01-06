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
package io.fabric8.partition.internal.repositories;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import io.fabric8.api.FabricException;
import io.fabric8.partition.internal.BaseWorkItemRepository;
import io.fabric8.partition.internal.functions.ChildDataToPath;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ZkWorkItemRepository extends BaseWorkItemRepository implements PathChildrenCacheListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZkWorkItemRepository.class);


    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final String partitionsPath;
    private final CuratorFramework curator;
    private volatile PathChildrenCache partitionCache;

    public ZkWorkItemRepository(CuratorFramework curator, String partitionsPath) {
        this.partitionsPath = partitionsPath;
        this.curator = curator;
    }

    @Override
    public synchronized void start() {
        try {
            ZooKeeperUtils.createDefault(curator, partitionsPath, null);
            if (partitionCache == null) {
                partitionCache = new PathChildrenCache(curator, partitionsPath, true, false, executorService);
                partitionCache.getListenable().addListener(this);
                partitionCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
                partitionCache.rebuild();
            }
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public synchronized void stop() {
        if (partitionCache != null) {
            try {
                partitionCache.close();
                partitionCache = null;
            } catch (IOException e) {
                throw FabricException.launderThrowable(e);
            }
        }
    }

    @Override
    public void close() {
        stop();
        executorService.shutdownNow();
    }

    @Override
    public List<String> listWorkItemLocations() {
        List<ChildData> children = partitionCache.getCurrentData();
        return Lists.transform(children, ChildDataToPath.INSTANCE);
    }

    @Override
    public String readContent(String location) {
        try {
            return Resources.toString(new URL(ZkWorkItemRepositoryFactory.SCHEME + ":" + location), Charsets.UTF_8);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
        switch (event.getType()) {
            case INITIALIZED:
            case CHILD_ADDED:
            case CHILD_REMOVED:
                notifyListeners();
                break;
        }
    }
}
