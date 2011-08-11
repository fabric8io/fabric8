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
import org.apache.karaf.shell.dev.AbstractBundleCommand;
import org.fusesource.fabric.fab.DependencyTree;
import org.fusesource.fabric.fab.ModuleRegistry;
import org.fusesource.fabric.fab.VersionedDependencyId;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.fusesource.fabric.fab.util.Strings.join;

@Command(name = "tree", scope = "fab", description = "Display the dependency tree of a Fabric Bundle")
public class TreeCommand extends AbstractBundleCommand {

    protected void doExecute(Bundle bundle) throws Exception {
        Properties instructions = new Properties();
        Dictionary headers = bundle.getHeaders();
        Enumeration e = headers.keys();
        while (e.hasMoreElements()) {
            Object key = e.nextElement();
            Object value = headers.get(key);
            if (key instanceof String && value instanceof String) {
                instructions.setProperty((String) key, (String) value);
            }
            System.out.println("Found " + key + " = " + value);
        }

        FabFacade facade = new BundleFabFacade(bundle);
        Map<String, Object> embeddedResources = new HashMap<String, Object>();
        FabClassPathResolver resolver = new FabClassPathResolver(facade, instructions, embeddedResources);
        resolver.resolve();

        List<DependencyTree> nonSharedDependencies = resolver.getNonSharedDependencies();
        log.info("local classpath");
        for (DependencyTree dependency : nonSharedDependencies) {
            log.info("    " + dependency.getDependencyId());
        }
        List<DependencyTree> sharedDependencies = resolver.getNonSharedDependencies();
        log.info("shared classpath");
        for (DependencyTree dependency : sharedDependencies) {
            log.info("    " + dependency.getDependencyId());
        }
    }
}