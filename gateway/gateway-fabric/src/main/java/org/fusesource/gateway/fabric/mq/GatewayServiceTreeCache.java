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
package org.fusesource.gateway.fabric.mq;

import io.fabric8.api.jcip.GuardedBy;
import io.fabric8.api.scr.InvalidComponentException;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.fusesource.gateway.ServiceMap;
import org.fusesource.gateway.ServiceDTO;
import org.fusesource.gateway.handlers.tcp.TcpGateway;
import org.jledit.utils.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Watches a ZooKeeper path for all services inside the path which may take part in the load balancer
 */
public class GatewayServiceTreeCache {
    private static final transient Logger LOG = LoggerFactory.getLogger(GatewayServiceTreeCache.class);

    private final CuratorFramework curator;
    private final String zkPath;
    private final ServiceMap serviceMap;
    private final List<TcpGateway> gateways;

    private final ExecutorService treeCacheExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final ObjectMapper mapper = new ObjectMapper();

    private final PathChildrenCacheListener treeListener = new PathChildrenCacheListener() {
        @Override
        public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent event) throws Exception {
            treeCacheEvent(event);
        }
    };

    @GuardedBy("active")
    private volatile TreeCache treeCache;


    public GatewayServiceTreeCache(CuratorFramework curator, String zkPath, ServiceMap serviceMap, List<TcpGateway> gateways) {
        this.curator = curator;
        this.zkPath = zkPath;
        this.serviceMap = serviceMap;
        this.gateways = gateways;
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String toString() {
        return "GatewayServiceTreeCache(zkPath: " + zkPath + " gateways: " + gateways + ")";
    }

    protected TreeCache getTreeCache() {
        if (!active.get())
            throw new InvalidComponentException();
        return treeCache;
    }


    public void init() throws Exception {
        if (active.compareAndSet(false, true)) {
            treeCache = new TreeCache(curator, zkPath, true, false, true, treeCacheExecutor);
            treeCache.start(TreeCache.StartMode.NORMAL);
            treeCache.getListenable().addListener(treeListener);
            LOG.info("Started a group listener for " + zkPath);
            for (TcpGateway gateway : gateways) {
                gateway.init();
            }
        }
    }

    public void destroy() {
        if (active.compareAndSet(true, false)) {
            treeCache.getListenable().removeListener(treeListener);
            Closeables.closeQuitely(treeCache);
            treeCache = null;
            treeCacheExecutor.shutdownNow();

            for (TcpGateway gateway : gateways) {
                gateway.destroy();
            }
        }
    }

    protected void treeCacheEvent(PathChildrenCacheEvent event) {
        ChildData childData = event.getData();
        if (childData == null) {
            return;
        }
        String path = childData.getPath();
        PathChildrenCacheEvent.Type type = event.getType();
        byte[] data = childData.getData();
        if (data == null || data.length == 0 || path == null) {
            return;
        }
        if (path.startsWith(zkPath)) {
            path = path.substring(zkPath.length());
        }
        boolean remove = false;
        switch (type) {
            case CHILD_ADDED:
            case CHILD_UPDATED:
                break;
            case CHILD_REMOVED:
                remove = true;
                break;
            default:
                return;
        }
        ServiceDTO dto = null;
        try {
            dto = mapper.readValue(data, ServiceDTO.class);
            expandPropertyResolvers(dto);
            if (remove) {
                serviceMap.serviceRemoved(path, dto);
            } else {
                serviceMap.serviceUpdated(path, dto);
            }
        } catch (IOException e) {
            LOG.warn("Failed to parse the JSON: " + new String(data) + ". Reason: " + e, e);
        } catch (URISyntaxException e) {
            LOG.warn("Failed to update URI for dto: " + dto + ", .Reason: " + e, e);
        }
    }

    protected void expandPropertyResolvers(ServiceDTO dto) throws URISyntaxException {
        List<String> services = dto.getServices();
        List<String> newList = new ArrayList<String>(services.size());
        for (String service : services) {
            String expanded = ZooKeeperUtils.getSubstitutedData(curator, service);
            newList.add(expanded);
        }
        dto.setServices(newList);
    }
}
