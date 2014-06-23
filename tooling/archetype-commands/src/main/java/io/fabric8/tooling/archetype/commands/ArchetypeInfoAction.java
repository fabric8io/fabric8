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

import io.fabric8.tooling.archetype.ArchetypeService;
import io.fabric8.tooling.archetype.catalog.Archetype;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = ArchetypeInfo.FUNCTION_VALUE, scope = ArchetypeInfo.SCOPE_VALUE, description = ArchetypeInfo.DESCRIPTION)
public class ArchetypeInfoAction extends AbstractAction {

    @Argument(index = 0, name = "archetype", description = "Archetype coordinates", required = true, multiValued = false)
    private String archetypeGAV;

    private final ArchetypeService archetypeService;

    public ArchetypeInfoAction(ArchetypeService archetypeService) {
        this.archetypeService = archetypeService;
    }

    @Override
    protected Object doExecute() throws Exception {
        Archetype archetype = archetypeService.getArchetype(archetypeGAV);
        if (archetype != null) {
            System.out.println(String.format("Archetype coordinates: %s:%s:%s", archetype.groupId, archetype.artifactId, archetype.version));
            System.out.println();
            System.out.println(String.format("Description: %s", archetype.description));
            System.out.println(String.format("Repository: %s", archetype.repository));
        } else {
            System.err.println("No archetype found for \"" + archetypeGAV + "\" coordinates");
        }
        return null;
    }

}
