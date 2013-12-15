/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.fab.osgi.internal;

import aQute.lib.osgi.Analyzer;
import io.fabric8.fab.osgi.ServiceConstants;
import org.fusesource.common.util.Objects;
import org.fusesource.common.util.Strings;
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
        Version v = Versions.fromMavenVersion(version);
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            // TODO should be using ranges here!!
            if (Objects.equal(bundle.getSymbolicName(), name) && Objects.equal(bundle.getVersion(), v)) {
                return bundle;
            }
        }
        return null;
    }

    /**
     * Find installed bundles by symbolic name
     *
     * @param context the bundle context to search
     * @param name the bundle symbolic name
     * @return the set of bundles found
     */
    public static Set<Bundle> findBundles(BundleContext context, String name) {
        Set<Bundle> result = new HashSet<Bundle>();
        Bundle[] bundles = context.getBundles();
        for (Bundle bundle : bundles) {
            if (Objects.equal(bundle.getSymbolicName(), name)) {
                result.add(bundle);
            }
        }
        return result;
    }

    /**
     * Find a single installed bundle by symbolic name.
     *
     * @param context the bundle context to search
     * @param name the bundle symbolic name
     * @return the bundle
     * @throws IllegalStateException if there are no matching bundles or more than one matching bundle
     */
    public static Bundle findOneBundle(BundleContext context, String name) {
        Set<Bundle> result = findBundles(context, name);
        if (result.size() != 1) {
            throw new IllegalStateException(String.format("Expected exactly one bundle with symbolic name %s but we found %s bundles", name, result.size()));
        }
        return result.iterator().next();
    }

    /**
     * Returns true if the given bundle is a fragment (and so cannot be loaded)
     */
    public static boolean isFragment(Bundle bundle) {
        return Strings.notEmpty((String) bundle.getHeaders().get(ServiceConstants.INSTR_FRAGMENT_HOST));
    }
}
