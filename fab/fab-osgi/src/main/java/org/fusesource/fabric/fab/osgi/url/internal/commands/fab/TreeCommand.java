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

import java.util.List;

@Command(name = "tree", scope = "fab", description = "Display the dependency tree of a Fabric Bundle")
public class TreeCommand extends FabCommandSupport {

    @Override
    protected void doExecute(Bundle bundle, FabClassPathResolver resolver) {
        List<DependencyTree> nonSharedDependencies = resolver.getNonSharedDependencies();
        System.out.println("local classpath");
        for (DependencyTree dependency : nonSharedDependencies) {
            System.out.println("    " + dependency.getDependencyId());
        }
        List<DependencyTree> sharedDependencies = resolver.getSharedDependencies();
        System.out.println("shared classpath");
        for (DependencyTree dependency : sharedDependencies) {
            System.out.println("    " + dependency.getDependencyId());
        }
    }
}