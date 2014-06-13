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
import io.fabric8.boot.commands.support.ProfileCompleter;
import io.fabric8.boot.commands.support.VersionCompleter;
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
    @Property(name = "osgi.command.scope", value = ProfileDisplay.SCOPE_VALUE),
    @Property(name = "osgi.command.function", value = ProfileDisplay.FUNCTION_VALUE)
})
public class ProfileDisplay extends AbstractCommandComponent {

    public static final String SCOPE_VALUE = "fabric";
    public static final String FUNCTION_VALUE = "profile-display";
    public static final String DESCRIPTION = "Displays information about the specified version of the specified profile (where the version defaults to the current default version)";

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
    @Reference(referenceInterface = ProfileCompleter.class, bind = "bindProfileCompleter", unbind = "unbindProfileCompleter")
    private ProfileCompleter profileCompleter; // dummy field
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
        return new ProfileDisplayAction(fabricService.get());
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }

    void bindProfileCompleter(ProfileCompleter completer) {
        bindCompleter(completer);
    }

    void unbindProfileCompleter(ProfileCompleter completer) {
        unbindCompleter(completer);
    }

    void bindVersionCompleter(VersionCompleter completer) {
        bindOptionalCompleter("--version", completer);
    }

    void unbindVersionCompleter(VersionCompleter completer) {
        unbindOptionalCompleter(completer);
    }
}
