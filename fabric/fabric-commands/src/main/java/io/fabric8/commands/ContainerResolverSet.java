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

import io.fabric8.api.FabricService;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.boot.commands.support.AbstractCommandComponent;
import io.fabric8.boot.commands.support.ContainerCompleter;
import io.fabric8.boot.commands.support.ResolverCompleter;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.service.command.Function;

@Component(immediate = true)
@Service({ Function.class, AbstractCommand.class })
@org.apache.felix.scr.annotations.Properties({
        @Property(name = "osgi.command.scope", value = ContainerResolverSet.SCOPE_VALUE),
        @Property(name = "osgi.command.function", value = ContainerResolverSet.FUNCTION_VALUE)
})
public class ContainerResolverSet extends AbstractCommandComponent {

    public static final String SCOPE_VALUE = "fabric";
    public static final String FUNCTION_VALUE =  "container-resolver-set";
    public static final String DESCRIPTION = "Apply the specified resolver policy to the specified container or containers";

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    // Completers
    @Reference(referenceInterface = ResolverCompleter.class, bind = "bindResolverCompleter", unbind = "unbindResolverCompleter")
    private ResolverCompleter resolverCompleter; // dummy field
    // Optional Completers
    @Reference(referenceInterface = ContainerCompleter.class, bind = "bindContainerCompleter", unbind = "unbindContainerCompleter")
    private ContainerCompleter containerCompleter; // dummy field

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public Action createNewAction() {
        assertValid();
        return new ContainerResolverSetAction(fabricService.get());
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }

    void bindResolverCompleter(ResolverCompleter completer) {
        bindCompleter(completer);
    }

    void unbindResolverCompleter(ResolverCompleter completer) {
        unbindCompleter(completer);
    }

    void bindContainerCompleter(ContainerCompleter completer) {
        bindOptionalCompleter(completer);
    }

    void unbindContainerCompleter(ContainerCompleter completer) {
        unbindOptionalCompleter(completer);
    }
}
