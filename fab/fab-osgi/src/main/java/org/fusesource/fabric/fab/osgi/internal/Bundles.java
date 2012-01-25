/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.fab.osgi.internal;

import aQute.lib.osgi.Analyzer;
import org.fusesource.fabric.fab.osgi.ServiceConstants;
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
     * Returns true if the given bundle is a fragment (and so cannot be loaded)
     */
    public static boolean isFragment(Bundle bundle) {
        return Strings.notEmpty((String) bundle.getHeaders().get(ServiceConstants.INSTR_FRAGMENT_HOST));
    }
}
