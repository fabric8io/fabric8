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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Throwables;
import com.google.common.io.Closeables;

import org.apache.aries.util.AriesFrameworkUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.xjc.CompileResults;
import org.fusesource.fabric.xjc.DynamicJaxbDataFormat;
import org.fusesource.fabric.xjc.DynamicXJC;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class SchemaDirectoryWatcher implements PathChildrenCacheListener {
    private static final transient Logger LOG = LoggerFactory.getLogger(SchemaDirectoryWatcher.class);

    private CuratorFramework curator;
    private String schemaPath;
    private BundleContext bundleContext;
    private FabricService fabricService;
    private Timer timer = new Timer();
    private AtomicBoolean startedFlag = new AtomicBoolean(false);

    private ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final ConcurrentMap<String, PathChildrenCache>
            pathCacheMap = new ConcurrentHashMap<String, PathChildrenCache>();
    private long timerDelay = 1000;
    private CompileResults compileResults;
    private DynamicJaxbDataFormat dataFormat;

    public SchemaDirectoryWatcher() {
    }

    public void start() {
        try {
            watchSchemaFolders();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public void stop() throws IOException {
        for (Map.Entry<String, PathChildrenCache> entry : pathCacheMap.entrySet()) {
            PathChildrenCache pathCache = entry.getValue();
            Closeables.close(pathCache, true);
        }
        pathCacheMap.clear();
        executorService.shutdown();
        timer.cancel();
    }


    public void childEvent(CuratorFramework curator, PathChildrenCacheEvent event)
            throws Exception {
        switch (event.getType()) {
        case INITIALIZED:
        case CHILD_ADDED:
        case CHILD_REMOVED:
            asyncRecompile();
            break;
        }

    }


    // Properties
    //-------------------------------------------------------------------------

    /**
     * Returns the latest XJC compiler results
     */
    public CompileResults getCompileResults() {
        return compileResults;
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public String getSchemaPath() {
        return schemaPath;
    }

    public void setSchemaPath(String schemaPath) {
        this.schemaPath = schemaPath;
    }

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public long getTimerDelay() {
        return timerDelay;
    }

    public void setTimerDelay(long timerDelay) {
        this.timerDelay = timerDelay;
    }

    public DynamicJaxbDataFormat getDataFormat() {
        return dataFormat;
    }

    public void setDataFormat(DynamicJaxbDataFormat dataFormat) {
        this.dataFormat = dataFormat;
    }

    // Implementation
    //-------------------------------------------------------------------------

    protected void asyncRecompile() {
        if (startedFlag.compareAndSet(false, true)) {
            timer.schedule(new RecompileTask(), timerDelay);
        }
    }


    /**
     * Lets try resolve the current {@link #getSchemaPath()} based on the profile paths
     */
    protected void watchSchemaFolders() throws Exception {
        Container container = fabricService.getCurrentContainer();
        String version = container.getVersion().getId();
        Profile[] profiles = container.getProfiles();
        for (Profile profile : profiles) {
            String profileId = profile.getId();
            String profilePath = ZkPath.getProfilePath(version, profileId);
            String path = profilePath + "/" + schemaPath;
            if (curator.checkExists().forPath(path) != null) {
                LOG.info("Starting SchemaDirectoryWatcher on path " + path);
                PathChildrenCache pathCache = new PathChildrenCache(curator, path, true, false,
                        executorService);
                pathCacheMap.put(path, pathCache);
                pathCache.getListenable().addListener(this);
                pathCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
                pathCache.rebuild();
            }
        }
    }

    private class RecompileTask extends TimerTask {
        @Override
        public void run() {
            recompile();
        }
    }

    protected void recompile() {
        LOG.debug("Looking for XSDs to recompile");

        Set<String> urls = new TreeSet<String>();
        List<PathChildrenCache> pathCaches = new ArrayList<PathChildrenCache>(pathCacheMap.values());
        for (PathChildrenCache pathCache : pathCaches) {
            List<ChildData> childData = pathCache.getCurrentData();
            int totalItems = childData.size();
            for (ChildData data : childData) {
                String path = data.getPath();
                urls.add("zk:" + path);
            }
        }
        LOG.info("Recompiling XSDs at URLs: " + urls);
        startedFlag.set(false);

        ClassLoader classLoader = AriesFrameworkUtil.getClassLoader(bundleContext.getBundle());
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        DynamicXJC xjc = new DynamicXJC(classLoader);
        xjc.setSchemaUrls(new ArrayList<String>(urls));
        compileResults = xjc.compileSchemas();
        LOG.info("Got XJC JAXBContext: " + compileResults.getJAXBContext());
        if (dataFormat != null) {
            dataFormat.updateCompileResults(compileResults);
        }
    }
}
