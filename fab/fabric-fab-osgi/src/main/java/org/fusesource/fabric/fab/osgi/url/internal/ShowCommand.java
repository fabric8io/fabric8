/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.url.internal;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.fab.ModuleRegistry;
import static org.fusesource.fabric.fab.util.Strings.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

@Command(name = "list", scope = "fab", description = "Display details about a module.")
public class ShowCommand extends FabCommand {

    @Argument(index = 0, required = true, description = "Name of the module to display.")
    private String name;

    @Option(name = "--version", multiValued = false, required = false)
    private String version;

    @Override
    protected Object doExecute() throws Exception {
        OsgiModuleRegistry registry = Activator.registry;
        List<ModuleRegistry.Module> modules = registry.getApplicationModules();

        for (ModuleRegistry.Module module : modules) {
            if( module.getName().equals(name) ) {

                ModuleRegistry.VersionedModule selected = null;

                TreeSet<String> versions = new TreeSet<String>();
                for (ModuleRegistry.VersionedModule mv : module.getVersions()) {
                    String v = mv.getId().getVersion();
                    versions.add(v);
                    if( version!=null && version.equals(v) ) {
                        selected = mv;
                    }
                }

                if( version==null ) {
                    selected = module.latest();
                } else {
                    if( selected==null ) {
                        println("Unknown version: %s", version);
                        return null;
                    }
                }

                println("Name    : %s", selected.getName());
                println("Version : %s", selected.getId().getVersion());
                versions.remove(selected.getId().getVersion());
                if( !versions.isEmpty() ) {
                println("  Available: %s", join(versions, ", "));
                }

                println("Description    : %s", notEmpty(selected.getLongDescription()) ? selected.getLongDescription() : selected.getDescription());
                Map<String,ModuleRegistry.VersionedModule> extensions = selected.getAvailableExtensions();
                if( extensions.size() > 0 ) {
                    println("Extensions");
                    println("  Default: %s", join(selected.getDefaultExtensions(), ", "));
                    println("  Enabled: %s", join(selected.getEnabledExtensions(), ", "));
                    println("  Available: %s\t%s\t%s", "Name",  "Version", "Description");
                    println("             %s\t%s\t%s", "Name",  "Version", "Description");
                    for (Map.Entry<String, ModuleRegistry.VersionedModule> entry : extensions.entrySet()) {
                        ModuleRegistry.VersionedModule extension = entry.getValue();
                        if( name!=null && name.equals(extension.getName()) ) {
                            println("            %s\t%s\t%s", extension.getName(), extension.getId().getVersion(), extension.getDescription());
                        }
                    }
                }

            }
        }

        return null;
    }


}