/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.url.internal.commands.fab;

import org.apache.felix.gogo.commands.Command;
import org.fusesource.fabric.fab.DependencyTree;
import org.fusesource.fabric.fab.osgi.url.internal.FabClassPathResolver;
import org.osgi.framework.Bundle;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Command(name = "tree", scope = "fab", description = "Display the dependency tree of a Fabric Bundle")
public class TreeCommand extends FabCommandSupport {

    private Set<DependencyTree> allDependencies;
    private Set<DependencyTree> sharedDependencies;


    @Override
    protected void doExecute(Bundle bundle, FabClassPathResolver resolver) {
        sharedDependencies = new HashSet<DependencyTree>(resolver.getSharedDependencies());
        allDependencies = new HashSet<DependencyTree>(resolver.getNonSharedDependencies());
        allDependencies.addAll(sharedDependencies);

        DependencyTree rootTree = resolver.getRootTree();
        write(System.out, rootTree);
    }


    /*
    * Write this node to the PrintWriter.  It should be indented one step
    * further for every element in the indents array.  If an element in the
    * array is <code>true</code>, there should be a | to connect to the next
    * sibling.
    */
    protected void write(PrintStream writer, DependencyTree node, boolean... indents) {
        for (boolean indent : indents) {
            writer.printf("%-3s", indent ? "|" : "");
        }
        String postfix = "";
        if (sharedDependencies.contains(node)) {
            postfix = " (shared)";
        }
        writer.println("+- " + node.getBundleSymbolicName() + " [" + node.getVersion() + "]" + postfix);
        List<DependencyTree> children = new ArrayList<DependencyTree>();
        for (DependencyTree child : node.getChildren()) {
            if (allDependencies.contains(child)) {
                children.add(child);
            }
        }
        for (int i = 0, size = children.size(); i < size; i++) {
            DependencyTree child = children.get(i);
            write(writer, child, concat(indents, (i + 1 < size)));
        }
    }

    /*
     * Add an element to the end of the array
     */
    private boolean[] concat(boolean[] array, boolean element) {
        boolean[] result = new boolean[array.length + 1];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        result[array.length] = element;
        return result;
    }
}