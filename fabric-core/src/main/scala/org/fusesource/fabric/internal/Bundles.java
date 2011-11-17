/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.internal;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class Bundles {
    private static final transient Logger logger = LoggerFactory.getLogger(Bundles.class);
    
    public static void startBundle(BundleContext context, String containsName) {
        Bundle[] bundles = context.getBundles();
        for (Bundle bundle : bundles) {
            String name = bundle.getSymbolicName();
            if (name.contains(containsName)) {
                logger.debug("About to start bundle: " + name);
                try {
                    bundle.start();
                } catch (Exception e) {
                    logger.error("Failed to start: " + e.getMessage(), e);
                }
            }
        }
    }

    public static void stopBundle(BundleContext context, String containsName) {
        Bundle[] bundles = context.getBundles();
        for (Bundle bundle : bundles) {
            String name = bundle.getSymbolicName();
            if (name.contains(containsName)) {
                logger.debug("About to start bundle: " + name);
                try {
                    bundle.stop();
                } catch (Exception e) {
                    logger.error("Failed to start: " + e.getMessage(), e);
                }
            }
        }
    }
}
