/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.xjc.profile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.base.Throwables;
import com.google.common.io.Closeables;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class SchemaDirectoryWatcher implements PathChildrenCacheListener {
    private static final transient Logger LOG = LoggerFactory.getLogger(SchemaDirectoryWatcher.class);

    private final CuratorFramework curator;
    private final PathChildrenCache pathCache;
    private final String schemaPath;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    public SchemaDirectoryWatcher(ProfileDynamicXJc xjc, String schemaPath) {
        this.schemaPath = schemaPath;
        curator = xjc.getCurator();
        this.pathCache = new PathChildrenCache(curator, schemaPath, true, false, executorService);
    }

    public void start() {
         try {
             ZooKeeperUtils.createDefault(curator, schemaPath, null);
             pathCache.getListenable().addListener(this);
             pathCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
             pathCache.rebuild();
         } catch (Exception e) {
             throw Throwables.propagate(e);
         }
     }

     public void stop() throws IOException {
         Closeables.close(pathCache, true);
         executorService.shutdown();
     }


    public void childEvent(CuratorFramework curator, PathChildrenCacheEvent event)
            throws Exception {
        switch (event.getType()) {
            case INITIALIZED:
            case CHILD_ADDED:
            case CHILD_REMOVED:
                executorService.submit(new RecompileTask());
                break;
        }

    }


    private class RecompileTask implements Runnable {
        @Override
        public void run() {
            recompile();
        }
    }

    protected  void recompile() {
        System.out.println("Recompiling the XSDs!");

        List<ChildData> childData = pathCache.getCurrentData();
        int totalItems = childData.size();
        for (ChildData data : childData) {
            String path = data.getPath();
            System.out.println("Has path: " + path);
        }
    }
}
