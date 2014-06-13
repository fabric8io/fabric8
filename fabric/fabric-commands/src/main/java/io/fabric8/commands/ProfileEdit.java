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
import io.fabric8.commands.support.BundleLocationCompleter;
import io.fabric8.commands.support.FeaturesCompleter;
import io.fabric8.commands.support.FeaturesUrlCompleter;
import io.fabric8.commands.support.PidCompleter;
import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.service.command.Function;
import org.apache.karaf.features.FeaturesService;
import org.jledit.EditorFactory;
import org.osgi.service.cm.ConfigurationAdmin;

@Component(immediate = true)
@Service({ Function.class, AbstractCommand.class })
@org.apache.felix.scr.annotations.Properties({
    @Property(name = "osgi.command.scope", value = ProfileEdit.SCOPE_VALUE),
    @Property(name = "osgi.command.function", value = ProfileEdit.FUNCTION_VALUE)
})
public class ProfileEdit extends AbstractCommandComponent {

    public static final String SCOPE_VALUE = "fabric";
    public static final String FUNCTION_VALUE = "profile-edit";
    public static final String DESCRIPTION = "Edits the specified version of the specified profile (where the version defaults to the current default version)";

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
    @Reference(referenceInterface = ConfigurationAdmin.class)
    private final ValidatingReference<ConfigurationAdmin> configurationAdmin = new ValidatingReference<ConfigurationAdmin>();
    @Reference(referenceInterface = EditorFactory.class)
    private final ValidatingReference<EditorFactory> editorFactory = new ValidatingReference<EditorFactory>();
    @Reference(referenceInterface = FeaturesService.class)
    private final ValidatingReference<FeaturesService> featuresService = new ValidatingReference<FeaturesService>();
    @Reference(referenceInterface = ProfileCompleter.class, bind = "bindProfileCompleter", unbind = "unbindProfileCompleter")
    private ProfileCompleter profileCompleter; // dummy field
    @Reference(referenceInterface = VersionCompleter.class, bind = "bindVersionCompleter", unbind = "unbindVersionCompleter")
    private VersionCompleter versionCompleter; // dummy field
    @Reference(referenceInterface = BundleLocationCompleter.class, bind = "bindBundleLocationCompleter", unbind = "unbindBundleLocationCompleter")
    private BundleLocationCompleter bundleLocationCompleter; // dummy field
    @Reference(referenceInterface = FeaturesCompleter.class, bind = "bindFeaturesCompleter", unbind = "unbindFeaturesCompleter")
    private FeaturesCompleter featuresCompleter; // dummy field
    @Reference(referenceInterface = FeaturesUrlCompleter.class, bind = "bindFeaturesUrlCompleter", unbind = "unbindFeaturesUrlCompleter")
    private FeaturesUrlCompleter featuresUrlCompleter; // dummy field
    @Reference(referenceInterface = PidCompleter.class, bind = "bindPidCompleter", unbind = "unbindPidCompleter")
    private PidCompleter pidCompleter; // dummy field

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
        return new ProfileEditAction(fabricService.get(), configurationAdmin.get(), editorFactory.get());
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }

    void bindConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin.bind(configurationAdmin);
    }

    void unbindConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin.unbind(configurationAdmin);
    }

    void bindEditorFactory(EditorFactory editorFactory) {
        this.editorFactory.bind(editorFactory);
    }

    void unbindEditorFactory(EditorFactory editorFactory) {
        this.editorFactory.unbind(editorFactory);
    }

    void bindFeaturesService(FeaturesService featuresService) {
        this.featuresService.bind(featuresService);
    }

    void unbindFeaturesService(FeaturesService featuresService) {
        this.featuresService.unbind(featuresService);
    }

    void bindProfileCompleter(ProfileCompleter completer) {
        bindCompleter(completer);
    }

    void unbindProfileCompleter(ProfileCompleter completer) {
        unbindCompleter(completer);
    }

    void bindVersionCompleter(VersionCompleter completer) {
        bindCompleter(completer);
    }

    void unbindVersionCompleter(VersionCompleter completer) {
        unbindCompleter(completer);
    }

    void bindBundleLocationCompleter(BundleLocationCompleter completer) {
        bindOptionalCompleter("--bundles", completer);
    }

    void unbindBundleLocationCompleter(BundleLocationCompleter completer) {
        unbindOptionalCompleter("--bundles");
    }

    void bindFeaturesCompleter(FeaturesCompleter completer) {
        bindOptionalCompleter("--features", completer);
    }

    void unbindFeaturesCompleter(FeaturesCompleter completer) {
        unbindOptionalCompleter("--features");
    }

    void bindFeaturesUrlCompleter(FeaturesUrlCompleter completer) {
        bindOptionalCompleter("--repositories", completer);
    }

    void unbindFeaturesUrlCompleter(FeaturesUrlCompleter completer) {
        unbindOptionalCompleter("--repositories");
    }

    void bindPidCompleter(PidCompleter completer) {
        bindOptionalCompleter("--pid", completer);
    }

    void unbindPidCompleter(PidCompleter completer) {
        unbindOptionalCompleter("--pid");
    }

}
