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
import io.fabric8.boot.commands.support.VersionCompleter;
import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.service.command.Function;

@Component(immediate = true)
@Service({Function.class, AbstractCommand.class})
@org.apache.felix.scr.annotations.Properties({
        @Property(name = "osgi.command.scope", value = VersionInfo.SCOPE_VALUE),
        @Property(name = "osgi.command.function", value = VersionInfo.FUNCTION_VALUE)
})
public class VersionInfo extends AbstractCommandComponent {

    public static final String SCOPE_VALUE = "fabric";
    public static final String FUNCTION_VALUE = "version-info";
    public static final String DESCRIPTION = "Displays information about a version";

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
    @Reference(referenceInterface = RuntimeProperties.class)
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();

    // Completers
    @Reference(referenceInterface = VersionCompleter.class, bind = "bindVersionCompleter", unbind = "unbindVersionCompleter")
    private VersionCompleter versionCompleter; // dummy field

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
        return new VersionInfoAction(fabricService.get(), runtimeProperties.get());
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }

    void bindRuntimeProperties(RuntimeProperties runtimeProperties) {
        this.runtimeProperties.bind(runtimeProperties);
    }

    void unbindRuntimeProperties(RuntimeProperties runtimeProperties) {
        this.runtimeProperties.unbind(runtimeProperties);
    }

    void bindVersionCompleter(VersionCompleter completer) {
        bindCompleter(completer);
    }

    void unbindVersionCompleter(VersionCompleter completer) {
        unbindCompleter(completer);
    }
}
