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
package org.fusesource.fabric.extender.listener;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.fusesource.fabric.api.ModuleStatus;
import org.fusesource.fabric.api.jcip.GuardedBy;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.delete;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setData;

@ThreadSafe
public abstract class AbstractExtenderListener extends AbstractComponent implements BundleListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricBlueprintBundleListener.class);

    private static final String KARAF_NAME = System.getProperty("karaf.name");

    @GuardedBy("ConcurrentMap") private final ConcurrentMap<Long, ModuleStatus> statusMap = new ConcurrentHashMap<Long, ModuleStatus>();

    protected String getKarafName() {
        return KARAF_NAME;
    }

    protected ModuleStatus putModuleStatus(long bundleId, ModuleStatus status) {
        return statusMap.put(bundleId, status);
    }

    protected abstract String getExtenderType();

    protected abstract CuratorFramework getCurator();

    @Override
    public synchronized void bundleChanged(BundleEvent event) {
        if (isValid()) {
            long bundleId = event.getBundle().getBundleId();
            if (event.getType() == BundleEvent.UNINSTALLED) {
                try {
                    statusMap.remove(bundleId);
                    delete(getCurator(), ZkPath.CONTAINER_EXTENDER_BUNDLE.getPath(KARAF_NAME, getExtenderType(), String.valueOf(bundleId)));
                    update();
                } catch (Exception e) {
                    LOGGER.debug("Failed to delete blueprint status of bundle {}.", event.getBundle().getBundleId());
                }
            }
        }
    }

    /**
     * Updates the extender status
     */
    synchronized void update() throws Exception {
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
            setData(getCurator(), ZkPath.CONTAINER_EXTENDER_STATUS.getPath(KARAF_NAME, getExtenderType()), ModuleStatus.FAILED.name(), CreateMode.EPHEMERAL);
        } else if (waiting > 0) {
            setData(getCurator(), ZkPath.CONTAINER_EXTENDER_STATUS.getPath(KARAF_NAME, getExtenderType()), ModuleStatus.WAITING.name(), CreateMode.EPHEMERAL);
        } else if (stopping > 0) {
            setData(getCurator(), ZkPath.CONTAINER_EXTENDER_STATUS.getPath(KARAF_NAME, getExtenderType()), ModuleStatus.STOPPING.name(), CreateMode.EPHEMERAL);
        } else if (starting > 0) {
            setData(getCurator(), ZkPath.CONTAINER_EXTENDER_STATUS.getPath(KARAF_NAME, getExtenderType()), ModuleStatus.STARTING.name(), CreateMode.EPHEMERAL);
        } else {
            setData(getCurator(), ZkPath.CONTAINER_EXTENDER_STATUS.getPath(KARAF_NAME, getExtenderType()), ModuleStatus.STARTED.name(), CreateMode.EPHEMERAL);
        }
    }
}
