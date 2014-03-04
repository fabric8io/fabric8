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
package io.fabric8.internal;

import io.fabric8.api.PlaceholderResolver;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling {@link PlaceholderResolver} tasks.
 */
public final class PlaceholderResolverHelpers {

    public static final Pattern SCHEME_PATTERN = Pattern.compile("\\$\\{([^ :\\}]+):([^\\}]*\\})");

    private PlaceholderResolverHelpers() {
        //Utility Class
    }

    /**
     * Extracts all placeholder resolver schemes that are referred from the value.
     */
    public static Set<String> getSchemeForValue(String value) {
        Set<String> schemes = new HashSet<String>();
        Matcher matcher = SCHEME_PATTERN.matcher(value);
        while (matcher.find()) {
            String scheme = matcher.group(1);
            schemes.add(scheme);
            String remaining = matcher.group(2);
            if (remaining != null) {
                schemes.addAll(getSchemeForValue(remaining));
            }
        }
        return schemes;
    }

    /**
     * Extracts all placeholder resolver schemes that are referred from the configuration.
     */
    public static Set<String> getSchemesForConfig(Map<String, String> props) {
        Set<String> schemes = new HashSet<String>();
        for (Map.Entry<String, String> e : props.entrySet()) {
            String value = e.getValue();
            schemes.addAll(getSchemeForValue(value));
        }
        return schemes;
    }

    /**
     * Extracts all placeholder resolver schemes that are referred from the profile.
     */
    public static Set<String> getSchemesForProfileConfigurations(Map<String, Map<String, String>> configs) {
        Set<String> schemes = new HashSet<String>();
        for (Map.Entry<String, Map<String, String>> e : configs.entrySet()) {
            Map<String, String> value = e.getValue();
            schemes.addAll(getSchemesForConfig(value));
        }
        return schemes;
    }
}
