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

import java.util.*;

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

                println("%15s: %s", "Name", selected.getName());
                println("%15s: %s", "Version", selected.getId());
                println("%15s: %s", "Artifact Id", selected.getId());

                versions.remove(selected.getId().getVersion());
                if( !versions.isEmpty() ) {
                    println("%20s: %s", "Available", join(versions, ", "));
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
                    println("Extensions:");
                    println("  * %-20s\t%-10s\t%-40s", "Name",  "Version", "Description");

                    for (Map.Entry<String, ModuleRegistry.VersionedModule> entry : extensions.entrySet()) {
                        ModuleRegistry.VersionedModule extension = entry.getValue();
                        println("  %s %-20s\t%-10s\t%-40s", enabled.contains(extension.getName()) ? "+" : "-", extension.getName(), extension.getId().getVersion(), extension.getDescription());
                    }
                }

            }
        }

        return null;
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