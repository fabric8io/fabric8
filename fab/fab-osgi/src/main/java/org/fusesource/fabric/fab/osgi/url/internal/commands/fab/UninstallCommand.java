/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.url.internal.commands.fab;

import org.apache.felix.gogo.commands.Command;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = "uninstall", scope = "fab", description = "Uninstalls the Fabric Bundle along with all of its transitive dependencies which are not being used by other bundles")
public class UninstallCommand extends ProcessUnusedBundles {
    private static final transient Logger LOG = LoggerFactory.getLogger(UninstallCommand.class);

    @Override
    protected void processBundle(Bundle bundle) {
        stopBundle(bundle);
        if (bundle.getState() != Bundle.UNINSTALLED) {
            LOG.debug("Uninstalling bundle %s version %s", bundle.getSymbolicName(), bundle.getVersion());
            try {
                bundle.uninstall();
            } catch (BundleException e) {
                System.out.println("Failed to start " + bundle.getSymbolicName() + " " + bundle.getVersion() + ". " + e);
                e.printStackTrace();
            }
        }
    }

}