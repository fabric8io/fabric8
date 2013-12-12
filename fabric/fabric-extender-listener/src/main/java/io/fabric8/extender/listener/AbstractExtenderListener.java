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
package io.fabric8.extender.listener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import io.fabric8.api.ModuleStatus;
import io.fabric8.api.jcip.GuardedBy;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.zookeeper.ZkPath;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.delete;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;

@ThreadSafe
public abstract class AbstractExtenderListener extends AbstractComponent implements BundleListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricBlueprintBundleListener.class);

    private static final String KARAF_NAME = System.getProperty("karaf.name");

    @GuardedBy("ConcurrentMap")
    private final ConcurrentMap<Long, ModuleStatus> statusMap = new ConcurrentHashMap<Long, ModuleStatus>();

    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    void activate(BundleContext bundleContext) {
        bundleContext.addBundleListener(this);
        activateComponent();
    }

    void deactivate(BundleContext bundleContext) {
        deactivateComponent();
        bundleContext.removeBundleListener(this);
        executor.shutdownNow();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    protected abstract String getExtenderType();

    void update(final long bundleId, final ModuleStatus bundleStatus, final ModuleStatus extenderStatus) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                if (isValid()) {
                    String extender = getExtenderType();
                    try {
                        if (bundleStatus != null) {
                            setData(getCurator(), ZkPath.CONTAINER_EXTENDER_BUNDLE.getPath(KARAF_NAME, extender, String.valueOf(bundleId)),
                                    bundleStatus.name(), CreateMode.EPHEMERAL);
                        } else {
                            delete(getCurator(), ZkPath.CONTAINER_EXTENDER_BUNDLE.getPath(KARAF_NAME, extender, String.valueOf(bundleId)));
                        }
                        setData(getCurator(), ZkPath.CONTAINER_EXTENDER_STATUS.getPath(KARAF_NAME, extender),
                                extenderStatus.name(), CreateMode.EPHEMERAL);
                    } catch (Exception e) {
                        LOGGER.debug("Failed to update status of bundle {} for extender {}.", bundleId, extender);
                    }
                }
            }
        });
    }

    public void bundleChanged(BundleEvent event) {
        long bundleId = event.getBundle().getBundleId();
        if (event.getType() == BundleEvent.UNINSTALLED) {
            statusMap.remove(bundleId);
            update(bundleId, null, getExtenderStatus());
        }
    }

    public void updateBundle(long bundleId, ModuleStatus moduleStatus) {
        statusMap.put(bundleId, moduleStatus);
        update(bundleId, moduleStatus, getExtenderStatus());
    }

    /**
     * Updates the extender status
     */
    protected ModuleStatus getExtenderStatus() {
        int starting = 0;
        int failed = 0;
        int waiting = 0;
        int stopping = 0;
        for (Map.Entry<Long, ModuleStatus> entry : statusMap.entrySet()) {
            ModuleStatus moduleStatus = entry.getValue();
            if (moduleStatus == ModuleStatus.FAILED) {
                failed++;
            } else if (moduleStatus == ModuleStatus.WAITING) {
                waiting++;
            } else if (moduleStatus == ModuleStatus.STOPPING) {
                stopping++;
            } else if (moduleStatus == ModuleStatus.STARTING) {
                starting++;
            }
        }
        if (failed > 0) {
            return ModuleStatus.FAILED;
        } else if (waiting > 0) {
            return ModuleStatus.WAITING;
        } else if (stopping > 0) {
            return ModuleStatus.STOPPING;
        } else if (starting > 0) {
            return ModuleStatus.STARTING;
        } else {
            return ModuleStatus.STARTED;
        }
    }

    protected CuratorFramework getCurator() {
        return curator.get();
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }

}
