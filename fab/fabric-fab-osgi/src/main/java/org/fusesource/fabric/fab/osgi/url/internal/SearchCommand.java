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
import org.fusesource.fabric.fab.ModuleRegistry;

import java.util.List;

@Command(name = "search", scope = "fab", description = "Search for all the available modules")
public class SearchCommand extends FabCommand {

    @Argument(index = 0, required = false, description = "Name of the module to list")
    private String name;

    @Override
    protected Object doExecute() throws Exception {
        OsgiModuleRegistry registry = Activator.registry;
        List<ModuleRegistry.Module> modules = registry.getApplicationModules();

        println("%-20s\t%-10s\t%-40s", "Name",  "Version", "Description");
        for (ModuleRegistry.Module module : modules) {
            ModuleRegistry.VersionedModule latest = module.latest();
            if( name==null || module.getName().indexOf(name) >=0 ) {
                println("%-20s\t%-10s\t%-40s", module.getName(), latest.getId().getVersion(), latest.getDescription());
            }
        }

        return null;
    }


}