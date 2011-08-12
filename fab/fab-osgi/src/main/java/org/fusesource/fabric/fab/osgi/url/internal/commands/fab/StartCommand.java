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
import org.fusesource.fabric.fab.osgi.url.internal.Bundles;
import org.fusesource.fabric.fab.osgi.url.internal.FabClassPathResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Command(name = "start", scope = "fab", description = "Starts the Fabric Bundle along with its transitive dependencies")
public class StartCommand extends FabCommandSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(StartCommand.class);

    @Override
    protected void doExecute(Bundle bundle, FabClassPathResolver resolver) {
        // lets process the bundles from the deepest dependencies first
        List<DependencyTree> sharedDependencies = resolver.getSharedDependencies();
        for (int i = sharedDependencies.size() - 1; i >= 0; i--) {
            DependencyTree dependency = sharedDependencies.get(i);
            String name = dependency.getBundleSymbolicName();
            String version = dependency.getVersion();
            Bundle b = Bundles.findBundle(bundleContext, name, version);
            if (b != null) {
                startBundle(b);
            }
        }
        startBundle(bundle);
    }

    protected void startBundle(Bundle bundle) {
        int state = bundle.getState();
        if (state == Bundle.INSTALLED || state == Bundle.RESOLVED) {
            LOG.debug("Starting bundle %s version %s", bundle.getSymbolicName(), bundle.getVersion());
            try {
                bundle.start();
            } catch (BundleException e) {
                System.out.println("Failed to start " + bundle.getSymbolicName() + " " + bundle.getVersion() + ". " + e);
                e.printStackTrace();
            }
        }
    }
}