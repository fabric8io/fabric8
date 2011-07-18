/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.url.internal;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 */
public class Bundles {
    public static boolean isInstalled(BundleContext bundleContext, String name) {
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            if (bundle.getSymbolicName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
