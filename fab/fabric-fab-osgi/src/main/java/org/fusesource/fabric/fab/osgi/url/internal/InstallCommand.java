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
import org.fusesource.fabric.fab.VersionedDependencyId;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Command(name = "install", scope = "fab", description = "Install a module")
public class InstallCommand extends FabCommand {

    @Argument(index = 0, required = true, description = "Name of the module to install")
    private String name;

    @Option(name = "--version", description = "Version to install")
    private String version;

    @Option(name = "--force", description = "Force install")
    private boolean force;

    @Override
    protected Object doExecute() throws Exception {
        OsgiModuleRegistry registry = Activator.registry;
        List<ModuleRegistry.Module> modules = registry.getApplicationModules();
        Map<VersionedDependencyId, Bundle> installed = registry.getInstalled();
        for (ModuleRegistry.Module module : modules) {
            if( name.equals(module.getName()) ) {
                HashSet<VersionedDependencyId> s = new HashSet<VersionedDependencyId>(module.getVersionIds());
                s.retainAll(installed.keySet());
                boolean isInstalled = !s.isEmpty();
                if( isInstalled && !force) {
                    throw new Exception("The module is already installed");
                }

                VersionedDependencyId id = module.latest().getId();
                String v = version!=null ? version : id.getVersion();

                String url = "fab:mvn:"+id.getGroupId()+"/"+id.getArtifactId()+"/"+v+"/"+id.getExtension()+
                        (id.getClassifier()==null ? "" : "/"+id.getClassifier());

                println("Installing: "+url);

                Bundle bundle = install(url);
                if( bundle!=null ) {
                    println("Installed bundle: %d", bundle.getBundleId());
                }
            }
        }

        return null;
    }

    protected Bundle install(String location) {
        try {
            return getBundleContext().installBundle(location, null);
        } catch (BundleException ex) {
            if (ex.getNestedException() != null) {
                println(ex.getNestedException().toString());
            } else {
                println(ex.toString());
            }
        } catch (Exception ex) {
            println(ex.toString());
        }
        return null;
    }
}