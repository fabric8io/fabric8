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

import io.fabric8.api.scr.Configurer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import io.fabric8.api.ModuleStatus;
import io.fabric8.api.jcip.ThreadSafe;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;

import java.util.Map;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;

@ThreadSafe
@Component(name = "io.fabric8.extender.listener.blueprint", label = "Fabric8 Blueprint Listener", immediate = true, metatype = false)
@Service(BlueprintListener.class)
@References({
    @Reference(referenceInterface = CuratorFramework.class, bind = "bindCurator", unbind = "unbindCurator")
})
public final class FabricBlueprintBundleListener extends AbstractExtenderListener implements BlueprintListener {

    @Reference
    private Configurer configurer;
    @Property(name = "name", label = "Container Name", description = "The name of the container", value = "${karaf.name}", propertyPrivate = true)
    private String name;

    private static final String EXTENDER_TYPE = "blueprint";

    @Activate
    void activate(BundleContext bundleContext, Map<String,?> configuration) throws Exception {
        configurer.configure(configuration, this);
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
