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
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.api.ModuleStatus;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.api.scr.ValidatingReference;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setData;

@ThreadSafe
@Component(name = "org.fusesource.fabric.extender.listener.blueprint", description = "Fabric Blueprint Listener", immediate = true)
@Service(BlueprintListener.class)
@References({
    @Reference(referenceInterface = CuratorFramework.class, bind = "bindCurator", unbind = "unbindCurator")
})
public final class FabricBlueprintBundleListener extends AbstractExtenderListener implements BlueprintListener {

    private static final String EXTENDER_TYPE = "blueprint";

    @Activate
    void activate(BundleContext bundleContext) {
        super.activate(bundleContext);
    }

    @Deactivate
    void deactivate(BundleContext bundleContext) {
        super.deactivate(bundleContext);
    }

    @Override
    protected String getExtenderType() {
        return EXTENDER_TYPE;
    }

    @Override
    public void blueprintEvent(BlueprintEvent event) {
        long bundleId = event.getBundle().getBundleId();
        ModuleStatus moduleStatus = toModuleStatus(event.getType());
        updateBundle(bundleId, moduleStatus);
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
