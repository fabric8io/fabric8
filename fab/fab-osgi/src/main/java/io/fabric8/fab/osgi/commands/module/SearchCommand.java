/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.fab.osgi.commands.module;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import io.fabric8.fab.ModuleRegistry;
import io.fabric8.fab.osgi.internal.Activator;
import io.fabric8.fab.osgi.internal.OsgiModuleRegistry;
import io.fabric8.fab.osgi.commands.CommandSupport;

import java.util.List;

@Command(name = "search", scope = "module", description = "Search for all the available modules")
public class SearchCommand extends CommandSupport {

    @Argument(index = 0, required = false, description = "Name of the module to list")
    private String name;

    @Override
    protected Object doExecute() throws Exception {
        OsgiModuleRegistry registry = Activator.registry;
        List<ModuleRegistry.Module> modules = registry.getApplicationModules();


        Table table = new Table("{1} | {2} | {3}", -20, -10, -40);
        table.add("Name", "Version", "Description");
        for (ModuleRegistry.Module module : modules) {
            ModuleRegistry.VersionedModule latest = module.latest();
            if( name==null || module.getName().indexOf(name) >=0 ) {
                table.add(module.getName(), latest.getId().getVersion(), latest.getDescription());
            }
        }
        table.print(session.getConsole());

        return null;
    }

}