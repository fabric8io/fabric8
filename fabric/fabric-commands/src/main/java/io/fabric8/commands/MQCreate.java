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
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.boot.commands.support.AbstractCommandComponent;
import io.fabric8.boot.commands.support.ProfileCompleter;
import io.fabric8.boot.commands.support.VersionCompleter;
import io.fabric8.commands.support.BrokerKindCompleter;
import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.service.command.Function;

@Component(immediate = true)
@Service({ Function.class, AbstractCommand.class })
@org.apache.felix.scr.annotations.Properties({
    @Property(name = "osgi.command.scope", value = MQCreate.SCOPE_VALUE),
    @Property(name = "osgi.command.function", value = MQCreate.FUNCTION_VALUE)
})
public class MQCreate extends AbstractCommandComponent {

    public static final String SCOPE_VALUE = "fabric";
    public static final String FUNCTION_VALUE = "mq-create";
    public static final String DESCRIPTION = "Create a new broker";

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
    @Reference(referenceInterface = RuntimeProperties.class, bind = "bindRuntimeProperties", unbind = "unbindRuntimeProperties")
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();

    @Reference(referenceInterface = ProfileCompleter.class, bind = "bindProfileCompleter", unbind = "unbindProfileCompleter")
    private ProfileCompleter profileCompleter; // dummy field
    @Reference(referenceInterface = ProfileCompleter.class, bind = "bindParentProfileCompleter", unbind = "unbindParentProfileCompleter")
    private ProfileCompleter parentProfileCompleter; // dummy field
    @Reference(referenceInterface = VersionCompleter.class, bind = "bindVersionCompleter", unbind = "unbindVersionCompleter")
    private VersionCompleter versionCompleter; // dummy field

    @Activate
    void activate() {
        bindOptionalCompleter("--kind", new BrokerKindCompleter());
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        unbindOptionalCompleter("--kind");
        deactivateComponent();
    }

    @Override
    public Action createNewAction() {
        assertValid();
        return new MQCreateAction(fabricService.get(), runtimeProperties.get());
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }

    void bindProfileCompleter(ProfileCompleter completer) {
        bindOptionalCompleter("--profile", completer);
    }

    void unbindProfileCompleter(ProfileCompleter completer) {
        unbindOptionalCompleter(completer);
    }

    void bindParentProfileCompleter(ProfileCompleter completer) {
        bindOptionalCompleter("--parent-profile", completer);
    }

    void unbindParentProfileCompleter(ProfileCompleter completer) {
        unbindOptionalCompleter(completer);
    }

    void bindVersionCompleter(VersionCompleter completer) {
        bindOptionalCompleter("--version", completer);
    }

    void unbindVersionCompleter(VersionCompleter completer) {
        unbindOptionalCompleter(completer);
    }


    void bindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.bind(service);
    }

    void unbindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.unbind(service);
    }

}
