/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.agent.internal;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.version.VersionRange;
import org.osgi.framework.Version;
import org.osgi.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.fabric8.agent.resolver.ResourceUtils.getSymbolicName;
import static io.fabric8.agent.resolver.ResourceUtils.getVersion;

/**
 * Helper class to deal with overriden bundles at feature installation time.
 */
public final class Overrides {

    protected static final String OVERRIDE_RANGE = "range";

    private static final Logger LOGGER = LoggerFactory.getLogger(Overrides.class);

    private Overrides() {
    }

    /**
     * Compute a list of bundles to install, taking into account overrides.
     * <p/>
     * The file containing the overrides will be loaded from the given url.
     * Blank lines and lines starting with a '#' will be ignored, all other lines
     * are considered as urls to override bundles.
     * <p/>
     * The list of resources to resolve will be scanned and for each bundle,
     * if a bundle override matches that resource, it will be used instead.
     * <p/>
     * Matching is done on bundle symbolic name (they have to be the same)
     * and version (the bundle override version needs to be greater than the
     * resource to be resolved, and less than the next minor version.  A range
     * directive can be added to the override url in which case, the matching
     * will succeed if the resource to be resolved is within the given range.
     *
     * @param resources the list of resources to resolve
     * @param overrides list of bundle overrides
     */
    public static <T extends Resource> void override(Map<String, T> resources, Collection<String> overrides) {
        // Do override replacement
        for (Clause override : Parser.parseClauses(overrides.toArray(new String[overrides.size()]))) {
            String url = override.getName();
            String vr = override.getAttribute(OVERRIDE_RANGE);
            T over = resources.get(url);
            if (over == null) {
                // Ignore invalid overrides
                continue;
            }
            for (String uri : new ArrayList<String>(resources.keySet())) {
                Resource res = resources.get(uri);
                if (getSymbolicName(res).equals(getSymbolicName(over))) {
                    VersionRange range;
                    if (vr == null) {
                        // default to micro version compatibility
                        Version v1 = getVersion(res);
                        Version v2 = new Version(v1.getMajor(), v1.getMinor() + 1, 0);
                        range = new VersionRange(false, v1, v2, true);
                    } else {
                        range = VersionRange.parseVersionRange(vr);
                    }
                    // The resource matches, so replace it with the overridden resource
                    // if the override is actually a newer version than what we currently have
                    if (range.contains(getVersion(over)) && getVersion(res).compareTo(getVersion(over)) < 0) {
                        resources.put(uri, over);
                    }
                }
            }
        }
    }

    public static Set<String> loadOverrides(String overridesUrl) {
        Set<String> overrides = new HashSet<String>();
        try {
            if (overridesUrl != null) {
                try (
                        InputStream is = new URL(overridesUrl).openStream()
                ) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            overrides.add(line);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Unable to load overrides bundles list", e);
        }
        return overrides;
    }

    public static String extractUrl(String override) {
        Clause[] cs = Parser.parseClauses(new String[]{override});
        if (cs.length != 1) {
            throw new IllegalStateException("Override contains more than one clause: " + override);
        }
        return cs[0].getName();
    }

}
