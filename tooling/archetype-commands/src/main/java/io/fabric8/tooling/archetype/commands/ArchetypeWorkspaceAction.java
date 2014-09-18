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

import java.util.prefs.Preferences;

import io.fabric8.common.util.Strings;
import io.fabric8.tooling.archetype.ArchetypeService;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = ArchetypeWorkspace.FUNCTION_VALUE, scope = ArchetypeWorkspace.SCOPE_VALUE, description = ArchetypeWorkspace.DESCRIPTION)
public class ArchetypeWorkspaceAction extends AbstractAction {

    @Argument(index = 0, name = "workspace", description = "To switch the workspace location (directory)", required = false, multiValued = false)
    private String workspace;

    private final ArchetypeService archetypeService;

    public ArchetypeWorkspaceAction(ArchetypeService archetypeService) {
        this.archetypeService = archetypeService;
    }

    @Override
    protected Object doExecute() throws Exception {
        if (Strings.isNullOrBlank(workspace)) {
            Preferences preferences = Preferences.userNodeForPackage(getClass());
            String current = preferences.get(ArchetypeWorkspace.PREFERENCE_WORKSPACE, null);

            if (current == null) {
                System.out.println("No workspace has been set.");
            } else {
                System.out.println("Current workspace: " + current);
            }
        } else {
            Preferences preferences = Preferences.userNodeForPackage(getClass());
            preferences.put(ArchetypeWorkspace.PREFERENCE_WORKSPACE, workspace);
            System.out.println("Workspace switched to: " + workspace);
        }

        return null;
    }

}
