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
package io.fabric8.tooling.archetype.commands;

import io.fabric8.boot.commands.support.AbstractCommandComponent;
import io.fabric8.tooling.archetype.ArchetypeService;
import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.service.command.Function;
import org.apache.karaf.shell.console.completer.FileCompleter;

@Component(immediate = true)
@Service({ Function.class, AbstractCommand.class })
@org.apache.felix.scr.annotations.Properties({
    @Property(name = "osgi.command.scope", value = ArchetypeWorkspace.SCOPE_VALUE),
    @Property(name = "osgi.command.function", value = ArchetypeWorkspace.FUNCTION_VALUE)
})
public class ArchetypeWorkspace extends AbstractCommandComponent {

    public static final String SCOPE_VALUE = "fabric";
    public static final String FUNCTION_VALUE = "archetype-workspace";
    public static final String DESCRIPTION = "To switch the workspace location (the default directory used by the archetype-generate command)";
    public static final String PREFERENCE_WORKSPACE = "workspace";

    @Reference(referenceInterface = ArchetypeService.class, bind = "bindArchetypeService", unbind = "unbindArchetypeService")
    private ArchetypeService archetypeService;

    @Override
    public Action createNewAction() {
        assertValid();
        return new ArchetypeWorkspaceAction(archetypeService);
    }

    @Activate
    void activate() {
        activateComponent();
        bindCompleter(new FileCompleter(null));
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    public void bindArchetypeService(ArchetypeService archetypeService) {
        this.archetypeService = archetypeService;
    }

    public void unbindArchetypeService(ArchetypeService archetypeService) {
        this.archetypeService = null;
    }

}
