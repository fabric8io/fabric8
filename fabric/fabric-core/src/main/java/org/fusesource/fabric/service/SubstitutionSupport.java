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
package org.fusesource.fabric.service;

import org.fusesource.fabric.api.PlaceholderResolver;
import org.fusesource.fabric.zookeeper.utils.InterpolationHelper;
import org.osgi.framework.BundleContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fusesource.fabric.internal.DataStoreHelpers.substituteBundleProperty;

public abstract class SubstitutionSupport {

    private final Map<String, PlaceholderResolver> placeholderResolvers = new HashMap<String, PlaceholderResolver>();

    public abstract BundleContext getBundleContext();

    public synchronized void bind(PlaceholderResolver resolver) {
        if (resolver != null) {
            placeholderResolvers.put(resolver.getScheme(), resolver);
        }
    }

    public synchronized void unbind(PlaceholderResolver resolver) {
        if (resolver != null) {
            placeholderResolvers.remove(resolver.getScheme());
        }
    }

    public void setPlaceholderResolvers(List<PlaceholderResolver> resolvers) {
        for (PlaceholderResolver resolver : resolvers) {
            bind(resolver);
        }
    }


    /**
     * Performs substitution to configuration based on the registered {@link PlaceholderResolver} instances.
     *
     * @param configs
     */
    public synchronized void substituteConfigurations(final Map<String, Map<String, String>> configs) {
        for (Map.Entry<String, Map<String, String>> entry : configs.entrySet()) {
            final String pid = entry.getKey();
            Map<String, String> props = entry.getValue();

            for (Map.Entry<String, String> e : props.entrySet()) {
                final String key = e.getKey();
                final String value = e.getValue();
                props.put(key, InterpolationHelper.substVars(value, key, null, props, new InterpolationHelper.SubstitutionCallback() {
                    public String getValue(String toSubstitute) {
                        if (toSubstitute != null && toSubstitute.contains(":")) {
                            String scheme = toSubstitute.substring(0, toSubstitute.indexOf(":"));
                            if (placeholderResolvers.containsKey(scheme)) {
                                return placeholderResolvers.get(scheme).resolve(configs, pid, key, toSubstitute);
                            }
                        }
                        return substituteBundleProperty(toSubstitute, getBundleContext());
                    }
                }));
            }
        }
    }
}


