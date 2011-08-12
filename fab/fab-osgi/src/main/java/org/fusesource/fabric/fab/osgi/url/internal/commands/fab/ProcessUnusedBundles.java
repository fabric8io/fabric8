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
import org.fusesource.fabric.fab.osgi.url.internal.Bundles;
import org.fusesource.fabric.fab.osgi.url.internal.FabClassPathResolver;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.ExportedPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base class which processes the transitive FAB bundles which are not being used by other bundles
 */
public abstract class ProcessUnusedBundles extends FabCommandSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(UninstallCommand.class);

    @Override
    protected void doExecute(Bundle bundle, FabClassPathResolver resolver) {
        // lets process the bundles from the deepest dependencies first
        List<DependencyTree> sharedDependencies = resolver.getSharedDependencies();
        List<Bundle> bundles = new ArrayList<Bundle>();
        for (DependencyTree dependency : sharedDependencies) {
            String name = dependency.getBundleSymbolicName();
            String version = dependency.getVersion();
            Bundle b = Bundles.findBundle(bundleContext, name, version);
            if (b != null) {
                bundles.add(b);
            } else {
                System.out.println("Warning could not find bundle: " + name + " version: " + version);
            }
        }
        bundles.add(bundle);

        Set<Bundle> bundleSet = new HashSet<Bundle>(bundles);
        for (Bundle b : bundles) {
            if (!bundleUsedByOtherBundles(b, bundleSet)) {
                processBundle(b);
            }
        }
    }

    protected boolean bundleUsedByOtherBundles(Bundle bundle, Set<Bundle> bundleSet) {
        ExportedPackage[] exportedPackages = getPackageAdmin().getExportedPackages(bundle);
        if (exportedPackages != null) {
            for (ExportedPackage exportedPackage : exportedPackages) {
                Bundle[] importingBundles = exportedPackage.getImportingBundles();
                if (importingBundles != null) {
                    for (Bundle importingBundle : importingBundles) {
                        if (!importingBundle.equals(bundle) && !bundleSet.contains(importingBundle)) {
                            System.out.println("Not processing bundle " + bundle + " as its used by " + importingBundle);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    protected abstract void processBundle(Bundle bundle);
}
