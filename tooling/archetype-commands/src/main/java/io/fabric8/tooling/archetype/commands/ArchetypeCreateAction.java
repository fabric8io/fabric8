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

import java.io.File;

import io.fabric8.tooling.archetype.ArchetypeService;
import io.fabric8.tooling.archetype.catalog.Archetype;
import io.fabric8.tooling.archetype.generator.ArchetypeHelper;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = ArchetypeInfo.FUNCTION_VALUE, scope = ArchetypeCreate.SCOPE_VALUE, description = ArchetypeCreate.DESCRIPTION)
public class ArchetypeCreateAction extends AbstractAction {

    @Argument(index = 0, name = "archetype", description = "Archetype coordinates", required = true, multiValued = false)
    private String archetypeGAV;

    @Argument(index = 1, name = "target", description = "Target directory where the project will be generated", required = true, multiValued = false)
    private File target;

    private final ArchetypeService archetypeService;

    public ArchetypeCreateAction(ArchetypeService archetypeService) {
        this.archetypeService = archetypeService;
    }

    @Override
    protected Object doExecute() throws Exception {
        Archetype archetype = archetypeService.getArchetype(archetypeGAV);
        if (archetype != null) {
            System.out.println(String.format("Generating %s:%s in %s", archetype.groupId, archetype.artifactId, target.getCanonicalPath()));
        } else {
            System.err.println("No archetype found for \"" + archetypeGAV + "\" coordinates");
        }
        return null;
    }

}
