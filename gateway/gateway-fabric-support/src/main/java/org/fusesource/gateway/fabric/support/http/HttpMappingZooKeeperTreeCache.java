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
package org.fusesource.gateway.fabric.support.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationConfig;
import io.fabric8.api.jcip.GuardedBy;
import io.fabric8.api.scr.InvalidComponentException;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.fusesource.gateway.ServiceDTO;
import org.fusesource.gateway.handlers.http.HttpMappingRule;
import org.jledit.utils.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.*;

/**
 * Watches a ZooKeeper path for all services inside the path which may take part in the load balancer and keeps
 * an in memory mapping of the incoming URL to the outgoing URLs
 */
public class HttpMappingZooKeeperTreeCache {
    private static final transient Logger LOG = LoggerFactory.getLogger(HttpMappingZooKeeperTreeCache.class);

    private final CuratorFramework curator;
    private final HttpMappingRule mappingRuleConfiguration;
    private final String zooKeeperPath;

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

    public HttpMappingZooKeeperTreeCache(CuratorFramework curator, HttpMappingRule mappingRuleConfiguration, String zooKeeperPath) {
        this.curator = curator;
        this.mappingRuleConfiguration = mappingRuleConfiguration;
        this.zooKeeperPath = zooKeeperPath;
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String toString() {
        return "HttpMappingZooKeeperTreeCache(config: " + mappingRuleConfiguration + ")";
    }


    protected TreeCache getTreeCache() {
        if (!active.get())
            throw new InvalidComponentException();
        return treeCache;
    }


    public void init() throws Exception {
        if (active.compareAndSet(false, true)) {
            treeCache = new TreeCache(curator, zooKeeperPath, true, false, true, treeCacheExecutor);
            treeCache.start(TreeCache.StartMode.NORMAL);
            treeCache.getListenable().addListener(treeListener);
            LOG.info("Started listening to ZK path " + zooKeeperPath);
        }
    }

    public void destroy() {
        if (active.compareAndSet(true, false)) {
            treeCache.getListenable().removeListener(treeListener);
            Closeables.closeQuitely(treeCache);
            treeCache = null;
            treeCacheExecutor.shutdownNow();
        }
    }

    protected void treeCacheEvent(PathChildrenCacheEvent event) {
        String zkPath = zooKeeperPath;
        ChildData childData = event.getData();
        if (childData == null) {
            return;
        }
        String path = childData.getPath();
        Type type = event.getType();
        byte[] data = childData.getData();
        if (data == null || data.length == 0 || path == null) {
            return;
        }
        if (path.startsWith(zkPath)) {
            path = path.substring(zkPath.length());
        }

        // TODO should we remove the version too and pick that one?
        // and include the version in the service chooser?

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
            List<String> services = dto.getServices();

            Map<String,String> params = new HashMap<String, String>();
            params.put("id", paramValue(dto.getId()));
            params.put("container", paramValue(dto.getContainer()));
            params.put("version", paramValue(dto.getVersion()));
            params.put("bundleName", paramValue(dto.getBundleName()));
            params.put("bundleVersion", paramValue(dto.getBundleVersion()));
            mappingRuleConfiguration.updateMappingRules(remove, path, services, params, dto);
        } catch (IOException e) {
            LOG.warn("Failed to parse the JSON: " + new String(data) + ". Reason: " + e, e);
        } catch (URISyntaxException e) {
            LOG.warn("Failed to update URI for dto: " + dto + ", .Reason: " + e, e);
        }
    }

    protected static String paramValue(String paramValue) {
        return paramValue != null ? paramValue : "";
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
