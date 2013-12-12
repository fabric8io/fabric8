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
import io.fabric8.fab.VersionedDependencyId;
import io.fabric8.fab.osgi.internal.Activator;
import io.fabric8.fab.osgi.internal.OsgiModuleRegistry;
import io.fabric8.fab.osgi.commands.CommandSupport;
import org.fusesource.common.util.Strings;
import org.osgi.framework.Bundle;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Command(name = "list", scope = "module", description = "List the installed modules")
public class ListCommand extends CommandSupport {

    @Argument(index = 0, required = false, description = "Name of the module to list")
    private String name;

    @Override
    protected Object doExecute() throws Exception {
        OsgiModuleRegistry registry = Activator.registry;
        List<ModuleRegistry.Module> modules = registry.getApplicationModules();
        Map<VersionedDependencyId, Bundle> installed = registry.getInstalled();

        Table table = new Table("{1} | {2} | {3} | {4}", 3, -20, -10, -40);
        table.add("Id", "Name", "Version", "Description");
        for (ModuleRegistry.Module module : modules) {
            ModuleRegistry.VersionedModule latest = module.latest();
            HashSet<VersionedDependencyId> s = new HashSet<VersionedDependencyId>(module.getVersionIds());
            s.retainAll(installed.keySet());
            if( !s.isEmpty() && (name==null || module.getName().indexOf(name) >=0) ) {
                HashSet<Long> bundles = new HashSet<Long>();
                for (VersionedDependencyId dependencyId : s) {
                    bundles.add(installed.get(dependencyId).getBundleId());
                }
                table.add(Strings.join(bundles, ", "), module.getName(), latest.getId().getVersion(), latest.getDescription());
            }
        }
        table.print(session.getConsole());

        return null;
    }


}