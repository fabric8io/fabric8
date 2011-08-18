/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.url.internal.commands.fab;

import org.fusesource.fabric.fab.DependencyTree;
import org.fusesource.fabric.fab.osgi.url.internal.FabClassPathResolver;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A helper class for printing dependency trees
 */
public class TreeHelper {
    private Set<DependencyTree> allDependencies;
    private Set<DependencyTree> sharedDependencies;

    public static void write(PrintStream writer, FabClassPathResolver resolver) {
        TreeHelper treeHelper = newInstance(resolver);
        DependencyTree rootTree = resolver.getRootTree();
        treeHelper.write(writer, rootTree);
    }

    public static TreeHelper newInstance(FabClassPathResolver resolver) {
        Set<DependencyTree> sharedDependencies = new HashSet<DependencyTree>(resolver.getSharedDependencies());
        Set<DependencyTree> allDependencies = new HashSet<DependencyTree>(resolver.getNonSharedDependencies());
        allDependencies.addAll(sharedDependencies);
        return new TreeHelper(allDependencies, sharedDependencies);
    }

    public TreeHelper(Set<DependencyTree> allDependencies, Set<DependencyTree> sharedDependencies) {
        this.allDependencies = allDependencies;
        this.sharedDependencies = sharedDependencies;
    }

    /*
    * Write this node to the PrintWriter.  It should be indented one step
    * further for every element in the indents array.  If an element in the
    * array is <code>true</code>, there should be a | to connect to the next
    * sibling.
    */
    public void write(PrintStream writer, DependencyTree node, boolean... indents) {
        for (boolean indent : indents) {
            writer.printf("%-3s", indent ? "|" : "");
        }
        String postfix = "";
        if (node.isOptional()) {
            postfix += " (optional)";
        }
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
