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
import org.apache.felix.gogo.commands.Option;
import io.fabric8.fab.ModuleRegistry;
import io.fabric8.fab.VersionedDependencyId;
import io.fabric8.fab.osgi.internal.Activator;
import io.fabric8.fab.osgi.internal.OsgiModuleRegistry;
import io.fabric8.fab.osgi.commands.CommandSupport;
import org.osgi.framework.Bundle;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Command(name = "uninstall", scope = "module", description = "Uninstall a module")
public class UninstallCommand extends CommandSupport {

    @Argument(index = 0, required = true, description = "Name of the module to uninstall")
    private String name;

    @Option(name = "--version", description = "Version to uninstall")
    private String version;

    @Override
    protected Object doExecute() throws Exception {
        OsgiModuleRegistry registry = Activator.registry;
        List<ModuleRegistry.Module> modules = registry.getApplicationModules();
        Map<VersionedDependencyId, Bundle> installed = registry.getInstalled();

        for (ModuleRegistry.Module module : modules) {
            HashSet<VersionedDependencyId> s = new HashSet<VersionedDependencyId>(module.getVersionIds());
            s.retainAll(installed.keySet());
            if( !s.isEmpty() &&  module.getName().equals(name) ) {
                for (VersionedDependencyId dependencyId : s) {
                    if( version==null || version.equals(dependencyId.getVersion()) ) {
                        installed.get(dependencyId).uninstall();
                    }
                }
            }
        }

        return null;
    }

}