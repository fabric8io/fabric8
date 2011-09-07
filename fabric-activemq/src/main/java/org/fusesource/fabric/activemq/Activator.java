/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.activemq;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class Activator implements BundleActivator {

    public static final AtomicReference<BundleContext> BUNDLE_CONTEXT = new AtomicReference<BundleContext>();

    private JMSService service = new FabricActiveMQService();
    private ServiceRegistration registration;

	public void start(BundleContext ctx) throws Exception {
        registration = ctx.registerService(JMSService.class.getName(), new FabricActiveMQService(), null);
        BUNDLE_CONTEXT.set(ctx);
	}

    public void stop(BundleContext ctx) throws Exception {
        registration.unregister();
        service.stop();
        BUNDLE_CONTEXT.set(null);
    }

}

