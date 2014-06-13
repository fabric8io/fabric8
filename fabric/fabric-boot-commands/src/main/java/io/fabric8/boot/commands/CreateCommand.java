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
package io.fabric8.boot.commands;

import io.fabric8.api.BootstrapComplete;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.ZooKeeperClusterBootstrap;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.boot.commands.service.CreateAvailable;
import io.fabric8.boot.commands.support.AbstractCommandComponent;
import io.fabric8.boot.commands.support.ResolverCompleter;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.service.command.Function;
import org.osgi.framework.BundleContext;

@Command(name = CreateCommand.FUNCTION_VALUE, scope = CreateCommand.SCOPE_VALUE, description = CreateCommand.DESCRIPTION, detailedDescription = "classpath:create.txt")
@Component(immediate = true)
@Service({ Function.class, AbstractCommand.class, CreateAvailable.class })
@org.apache.felix.scr.annotations.Properties({
        @Property(name = "osgi.command.scope", value = CreateCommand.SCOPE_VALUE),
        @Property(name = "osgi.command.function", value = CreateCommand.FUNCTION_VALUE)
})
public class CreateCommand extends AbstractCommandComponent implements CreateAvailable {

    public static final String SCOPE_VALUE = "fabric";
    public static final String FUNCTION_VALUE =  "create";
    public static final String DESCRIPTION = "Creates a new fabric ensemble (ZooKeeper ensemble) and imports fabric profiles";

    // Logical Dependencies
    @Reference
    private BootstrapComplete bootComplete;

    @Reference(referenceInterface = ZooKeeperClusterBootstrap.class, bind = "bindBootstrap", unbind = "unbindBootstrap")
    private final ValidatingReference<ZooKeeperClusterBootstrap> bootstrap = new ValidatingReference<ZooKeeperClusterBootstrap>();
    @Reference(referenceInterface = RuntimeProperties.class, bind = "bindRuntimeProperties", unbind = "unbindRuntimeProperties")
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();

    // Optional Completers
    @Reference(referenceInterface = ResolverCompleter.class, bind = "bindResolverCompleter", unbind = "unbindResolverCompleter")
    private ResolverCompleter resolverCompleter; // dummy field

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
        return new CreateAction(bundleContext, bootstrap.get(), runtimeProperties.get());
    }

    void bindBootstrap(ZooKeeperClusterBootstrap bootstrap) {
        this.bootstrap.bind(bootstrap);
    }

    void unbindBootstrap(ZooKeeperClusterBootstrap bootstrap) {
        this.bootstrap.unbind(bootstrap);
    }

    void bindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.bind(service);
    }

    void unbindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.unbind(service);
    }

    void bindResolverCompleter(ResolverCompleter completer) {
        bindOptionalCompleter(completer);
    }

    void unbindResolverCompleter(ResolverCompleter completer) {
        unbindOptionalCompleter(completer);
    }
}
