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
import io.fabric8.utils.TablePrinter;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = ArchetypeList.FUNCTION_VALUE, scope = ArchetypeList.SCOPE_VALUE, description = ArchetypeList.DESCRIPTION)
public class ArchetypeListAction extends AbstractAction {

    @Option(name = "-v", aliases = "--verbose", description = "Flag for verbose output", multiValued = false, required = false)
    private boolean verbose;

    private final ArchetypeService archetypeService;

    public ArchetypeListAction(ArchetypeService archetypeService) {
        this.archetypeService = archetypeService;
    }

    @Override
    protected Object doExecute() throws Exception {

        TablePrinter table = new TablePrinter();
        if (verbose) {
            table.columns("groupId", "artifactId", "version", "description");
        } else {
            table.columns("artifactId", "description");
        }

        for (Archetype archetype : archetypeService.listArchetypes()) {
            if (verbose) {
                table.row(archetype.groupId, archetype.artifactId, archetype.version, archetype.description);
            } else {
                // only list artifact id in short format
                table.row(archetype.artifactId, archetype.description);
            }
        }
        table.print();

        return null;
    }

}
