/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.fab.osgi.url.internal;

import org.fusesource.fabric.fab.osgi.url.ServiceConstants;
import org.ops4j.pax.url.commons.handler.HandlerActivator;
import org.osgi.framework.BundleContext;

/**
 * Activator for the fab protocol
 */
public class Activator extends HandlerActivator<Configuration> {
    private static Activator instance;

    private BundleContext bundleContext;

    public static Activator getInstance() {
        return instance;
    }

    public static BundleContext getInstanceBundleContext() {
        Activator activator = getInstance();
        if (activator != null) {
            return activator.getBundleContext();
        }
        return null;
    }

    public Activator() {
        super(ServiceConstants.PROTOCOLS_SUPPORTED, ServiceConstants.PID, new FabConnectionFactory());
        instance = this;
    }

    @Override
    public void start(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        super.start(bundleContext);
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }
}