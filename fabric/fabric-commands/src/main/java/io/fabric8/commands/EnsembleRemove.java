/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.commands;

import io.fabric8.api.ZooKeeperClusterService;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.boot.commands.support.AbstractCommandComponent;
import io.fabric8.boot.commands.support.ContainerCompleter;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.service.command.Function;
import org.osgi.framework.BundleContext;

@Component(immediate = true)
@Service({ Function.class, AbstractCommand.class })
@org.apache.felix.scr.annotations.Properties({
        @Property(name = "osgi.command.scope", value = EnsembleRemove.SCOPE_VALUE),
        @Property(name = "osgi.command.function", value = EnsembleRemove.FUNCTION_VALUE)
})
public class EnsembleRemove extends AbstractCommandComponent {

    public static final String SCOPE_VALUE = "fabric";
    public static final String FUNCTION_VALUE =  "ensemble-remove";
    public static final String DESCRIPTION = "Re-create the current ensemble, excluding the specified containers from the ensemble";

    @Reference(referenceInterface = ZooKeeperClusterService.class)
    private final ValidatingReference<ZooKeeperClusterService> clusterService = new ValidatingReference<ZooKeeperClusterService>();

    // Completers
    @Reference(referenceInterface = ContainerCompleter.class, bind = "bindContainerCompleter", unbind = "unbindContainerCompleter")
    private ContainerCompleter containerCompleter; // dummy field

    private BundleContext bundleContext;

    @Activate
    void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public Action createNewAction() {
        assertValid();
        return new EnsembleRemoveAction(bundleContext, clusterService.get());
    }

    void bindClusterService(ZooKeeperClusterService clusterService) {
        this.clusterService.bind(clusterService);
    }

    void unbindClusterService(ZooKeeperClusterService clusterService) {
        this.clusterService.unbind(clusterService);
    }

    void bindContainerCompleter(ContainerCompleter completer) {
        bindCompleter(completer);
    }

    void unbindContainerCompleter(ContainerCompleter completer) {
        unbindCompleter(completer);
    }
}
