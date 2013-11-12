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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.zookeeper.CreateMode;
import org.fusesource.fabric.api.ModuleStatus;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.api.scr.Validatable;
import org.fusesource.fabric.api.scr.ValidatingReference;
import org.fusesource.fabric.api.scr.ValidationSupport;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setData;

@ThreadSafe
@Component(name = "org.fusesource.fabric.extender.listener.blueprint", description = "Fabric Blueprint Listener", immediate = true)
@Service(BlueprintListener.class)
public final class FabricBlueprintBundleListener extends AbstractExtenderListener implements BlueprintListener, Validatable {

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricBlueprintBundleListener.class);

    private static final String EXTENDER_TYPE = "blueprint";

    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    private final ValidationSupport active = new ValidationSupport();

    @Activate
    void activate(BundleContext bundleContext) {
        bundleContext.addBundleListener(this);
        active.setValid();
    }

    @Deactivate
    void deactivate(BundleContext bundleContext) {
        active.setInvalid();
        bundleContext.removeBundleListener(this);
    }

    @Override
    public boolean isValid() {
        return active.isValid();
    }

    @Override
    public void assertValid() {
        active.assertValid();
    }

    @Override
    public void blueprintEvent(BlueprintEvent event) {
        if (isValid()) {
            long bundleId = event.getBundle().getBundleId();
            try {
                ModuleStatus moduleStatus = toModuleStatus(event.getType());
                putModuleStatus(bundleId, moduleStatus);
                setData(getCurator(), ZkPath.CONTAINER_EXTENDER_BUNDLE.getPath(getKarafName(), EXTENDER_TYPE, String.valueOf(bundleId)), moduleStatus.name(), CreateMode.EPHEMERAL);
                update();
            } catch (Exception e) {
                LOGGER.debug("Failed to write blueprint status of bundle {}.", bundleId, e);
            }
        }
    }

    @Override
    public String getExtenderType() {
        return EXTENDER_TYPE;
    }

    @Override
    protected CuratorFramework getCurator() {
        return curator.get();
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }

    private ModuleStatus toModuleStatus(int type) {
        switch (type) {
            case BlueprintEvent.CREATING:
                return ModuleStatus.STARTING;
            case BlueprintEvent.CREATED:
                return ModuleStatus.STARTED;
            case BlueprintEvent.DESTROYING:
                return ModuleStatus.STOPPING;
            case BlueprintEvent.DESTROYED:
                return ModuleStatus.STOPPED;
            case BlueprintEvent.FAILURE:
                return ModuleStatus.FAILED;
            case BlueprintEvent.GRACE_PERIOD:
                return ModuleStatus.WAITING;
            case BlueprintEvent.WAITING:
                return ModuleStatus.WAITING;
            default:
                return ModuleStatus.UNKNOWN;
        }
    }
}
