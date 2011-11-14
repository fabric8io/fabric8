/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.commands.fab;

import org.fusesource.fabric.fab.osgi.internal.FabClassPathResolver;
import org.fusesource.fabric.fab.osgi.commands.BundleCommandSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public abstract class FabCommandSupport extends BundleCommandSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabCommandSupport.class);

    protected void doExecute(Bundle bundle) throws Exception {
        FabClassPathResolver resolver = createFabResolver(bundle);
        doExecute(bundle, resolver);
    }

    protected abstract void doExecute(Bundle bundle, FabClassPathResolver resolver) throws Exception;

    protected void stopBundle(Bundle bundle) {
        if (bundle.getState() == Bundle.ACTIVE) {
            LOG.debug("Stopping bundle %s version %s", bundle.getSymbolicName(), bundle.getVersion());
            try {
                bundle.stop();
            } catch (BundleException e) {
                System.out.println("Failed to start " + bundle.getSymbolicName() + " " + bundle.getVersion() + ". " + e);
                e.printStackTrace();
            }
        }
    }
}
