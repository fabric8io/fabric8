/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.concurrent.atomic.AtomicReference;

/**
 */
public class Activator implements BundleActivator {

    public static final AtomicReference<BundleContext> BUNDLE_CONTEXT = new AtomicReference<BundleContext>();

	public void start(BundleContext ctx) throws Exception {
        BUNDLE_CONTEXT.set(ctx);
	}

    public void stop(BundleContext ctx) throws Exception {
        BUNDLE_CONTEXT.set(null);
    }

}

