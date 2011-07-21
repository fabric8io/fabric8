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
import org.codehaus.plexus.interpolation.ValueSource;
import org.fusesource.fabric.fab.ModuleRegistry;
import org.fusesource.fabric.fab.VersionedDependencyId;
import org.fusesource.fabric.fab.util.Strings;
import org.osgi.framework.Bundle;

import java.io.PrintStream;
import java.util.*;
import java.util.regex.Pattern;

@Command(name = "list", scope = "fab", description = "List the installed modules")
public class ListCommand extends FabCommand {

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