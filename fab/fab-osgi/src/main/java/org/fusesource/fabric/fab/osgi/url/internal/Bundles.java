/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.url.internal;

import aQute.lib.osgi.Analyzer;
import org.apache.felix.utils.version.VersionCleaner;
import org.fusesource.fabric.fab.util.Strings;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.PackageAdmin;

import java.util.*;

/**
 * Some bundle helper methods
 */
public class Bundles {
    public static boolean isInstalled(BundleContext bundleContext, String name, String version) {
        return findBundle(bundleContext, name, version) != null;
    }

    public static Set<String> filterInstalled(BundleContext bundleContext, Collection<String> packages) {
        HashSet<String> rc = new HashSet<String>(packages);
        for (Bundle bundle : bundleContext.getBundles()) {
            if( rc.isEmpty() ) {
                break;
            }
            String value = (String) bundle.getHeaders().get("Export-Package");
            if(Strings.notEmpty(value)) {
                Map<String, Map<String, String>> values = new Analyzer().parseHeader( value );
                rc.removeAll( values.keySet());
            }
        }
        return rc;
    }

    public static Bundle findBundle(BundleContext bundleContext, String name, String version) {
        Version v = new Version(VersionCleaner.clean(version));
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            if (bundle.getSymbolicName().equals(name) && bundle.getVersion().equals(v)) {
                return bundle;
            }
        }
        return null;
    }
}
