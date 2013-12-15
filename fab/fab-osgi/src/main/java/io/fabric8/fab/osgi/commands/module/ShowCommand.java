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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.fab.ModuleDescriptor;
import io.fabric8.fab.ModuleRegistry;
import io.fabric8.fab.VersionedDependencyId;
import io.fabric8.fab.osgi.ServiceConstants;
import io.fabric8.fab.osgi.commands.CommandSupport;
import io.fabric8.fab.osgi.internal.Activator;
import io.fabric8.fab.osgi.internal.OsgiModuleRegistry;
import org.osgi.framework.Bundle;

import static org.fusesource.common.util.Strings.join;
import static org.fusesource.common.util.Strings.notEmpty;

@Command(name = "show", scope = "module", description = "Display details about a module.")
public class ShowCommand extends CommandSupport {

    @Argument(index = 0, required = true, description = "Name of the module to display.")
    private String name;

    @Option(name = "--version", multiValued = false, required = false)
    private String version;

    @Override
    protected Object doExecute() throws Exception {
        OsgiModuleRegistry registry = Activator.registry;
        List<ModuleRegistry.Module> modules = registry.getApplicationModules();
        Map<VersionedDependencyId, Bundle> installed = registry.getInstalled();

        for (ModuleRegistry.Module module : modules) {
            if( module.getName().equals(name) ) {

                ModuleRegistry.VersionedModule selected = null;

                TreeSet<String> versions = mapVersion(module.getVersionIds());
                HashSet<VersionedDependencyId> installedVersionIds = new HashSet<VersionedDependencyId>(module.getVersionIds());
                installedVersionIds.retainAll(installed.keySet());
                TreeSet<String> installedVersions = mapVersion(installedVersionIds);
                versions.removeAll(installedVersions);

                if( version!=null ) {
                    selected = module.getVersions().get(version);
                    if( selected==null ) {
                        println("Unknown version: %s", version);
                        return null;
                    }
                } else {
                    selected = module.latest();
                }

                println("%15s: %s", "Name", selected.getName());
                if( !installedVersionIds.isEmpty() ) {
                    for (VersionedDependencyId id : installedVersionIds) {
                        Bundle bundle = installed.get(id);
                        String version = id.getVersion();
                        String extensionIds = (String) bundle.getHeaders().get(ServiceConstants.INSTR_FAB_MODULE_ENABLED_EXTENSIONS);
                        if( notEmpty(extensionIds) ) {
                            List<VersionedDependencyId> ids = ModuleDescriptor.decodeVersionList(extensionIds);
                            ArrayList<String> ext = new ArrayList<String>();
                            for (VersionedDependencyId x : ids) {
                                ModuleRegistry.VersionedModule vm = registry.getVersionedModule(x);
                                if( vm!=null ) {
                                    ext.add(vm.getName());
                                }
                            }
                            if( !ext.isEmpty() ) {
                                version += ", Extensions: "+join(ext, " ")+"";
                            }
                        }
                        println("%15s: %s, Bundle: %d", "Installed", version, bundle.getBundleId());
                    }
                }
                if( !versions.isEmpty() ) {
                    println("%15s: %s", "Available", join(versions, ", "));
                }

                String desc = notEmpty(selected.getLongDescription()) ? selected.getLongDescription() : selected.getDescription();
                String lines[] = wordWrap(desc, 60);
                println("%15s: %s", "Description", lines[0]);
                for( int i=1; i < lines.length; i++ ) {
                    println("%15s  %s", "", lines[i]);
                }

                Map<String,ModuleRegistry.VersionedModule> extensions = selected.getAvailableExtensions();
                if( extensions.size() > 0 ) {
                    HashSet enabled = new HashSet<String>(selected.getEnabledExtensions());
                    println("%15s: %s", "Extensions", selected.getDefaultExtensions().isEmpty() ? "" : "(Default: "+join(selected.getDefaultExtensions(), ", ")+")");

                    Table table = new Table("       {1} | {2} | {3}", -20, -10, -40);
                    table.add("Name", "Version", "Description");

                    for (Map.Entry<String, ModuleRegistry.VersionedModule> entry : extensions.entrySet()) {
                        ModuleRegistry.VersionedModule extension = entry.getValue();
                        table.add( extension.getName(), extension.getId().getVersion(), extension.getDescription());
                    }
                    table.print(session.getConsole());
                }

            }
        }

        return null;
    }

    private TreeSet<String> mapVersion(Collection<VersionedDependencyId> versionIds) {
        TreeSet<String> versions = new TreeSet<String>();
        for (VersionedDependencyId id : versionIds) {
            String v = id.getVersion();
            versions.add(v);
        }
        return versions;
    }

    private String[] wordWrap(String desc, int max) {
        ArrayList<String> lines = new ArrayList<String>();
        LinkedList<String> words = new LinkedList<String>(Arrays.asList(desc.split("\\s+")));
        String current="";
        while(!words.isEmpty()) {
            String word = words.getFirst();
            if( current.length()==0 || (current.length() + word.length()) < max ) {
                if( current.length()!=0 ){
                    current += " ";
                }
                current += word;
                words.removeFirst();
            } else {
                lines.add(current);
                current = "";
            }
        }
        if( current.length()!=0 ) {
            lines.add(current);
        }
        return lines.toArray(new String[lines.size()]);
    }


}