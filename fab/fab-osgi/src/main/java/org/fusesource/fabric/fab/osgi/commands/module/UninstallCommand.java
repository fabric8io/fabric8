/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.commands.module;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.fab.ModuleRegistry;
import org.fusesource.fabric.fab.VersionedDependencyId;
import org.fusesource.fabric.fab.osgi.internal.Activator;
import org.fusesource.fabric.fab.osgi.internal.OsgiModuleRegistry;
import org.fusesource.fabric.fab.osgi.commands.CommandSupport;
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