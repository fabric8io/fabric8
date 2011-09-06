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
import org.fusesource.fabric.fab.util.Objects;
import org.fusesource.fabric.fab.util.Strings;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import java.util.*;

/**
 * Some bundle helper methods
 */
public class Bundles {

    public static boolean isInstalled(BundleContext bundleContext, String name, String version) {
        return findBundle(bundleContext, name, version) != null;
    }

    /**
     * Filter out any packages which are already installed as a version which matches the imported version range
     *
     * @return the filtered set of packages which are not installed
     */
    public static Set<String> filterInstalled(BundleContext bundleContext, Collection<String> packages, VersionResolver resolver) {
        HashSet<String> rc = new HashSet<String>(packages);
        for (Bundle bundle : bundleContext.getBundles()) {
            if (rc.isEmpty()) {
                break;
            }
            String value = (String) bundle.getHeaders().get("Export-Package");
            if (Strings.notEmpty(value)) {
                Map<String, Map<String, String>> values = new Analyzer().parseHeader(value);
                for (String packageName : packages) {
                    Map<String, String> map = values.get(packageName);
                    if (map != null) {
                        String version = map.get("version");
                        if (version != null) {
                            String importedVersion = resolver.resolvePackageVersion(packageName);
                            if (importedVersion != null) {
                                if (Versions.inRange(version, importedVersion)) {
                                    rc.remove(packageName);
                                }
                            }
                        }
                    }
                }
            }
        }
        return rc;
    }

    public static Bundle findBundle(BundleContext bundleContext, String name, String version) {
        Version v = new Version(VersionCleaner.clean(version));
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            // TODO should be using ranges here!!
            if (Objects.equal(bundle.getSymbolicName(), name) && Objects.equal(bundle.getVersion(), v)) {
                return bundle;
            }
        }
        return null;
    }
}
