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
package io.fabric8.service;

import io.fabric8.api.FabricService;
import io.fabric8.api.PlaceholderResolver;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.utils.Strings;

import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
@Component(name = "io.fabric8.placholder.resolver.versionprop", label = "Fabric8 Version Property Placeholder Resolver", immediate = true, metatype = false)
@Service({ PlaceholderResolver.class, VersionPropertyPointerResolver.class })
@Properties({ @Property(name = "scheme", value = VersionPropertyPointerResolver.RESOLVER_SCHEME) })
public final class VersionPropertyPointerResolver extends AbstractComponent implements PlaceholderResolver {

    public static final String RESOLVER_SCHEME = "version";

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionPropertyPointerResolver.class);

    private static final String EMPTY = "";

    public static final String VERSIONS_PID = "io.fabric8.version";
    public static final String VERSION_PREFIX = "${version:";
    public static final String VERSION_POSTFIX = "}";

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    /**
     * Lets replace any other instances of ${version:key} with the value
     */
    public static String replaceVersions(Map<String, Map<String, String>> configs, String value) {
        // TODO we should really support other completions here too other than ${version:
        boolean replaced;
        do {
            replaced = false;
            int startIdx = value.indexOf(VERSION_PREFIX);
            if (startIdx >= 0) {
                int keyIdx = startIdx + VERSION_PREFIX.length();
                int endIdx = value.indexOf(VERSION_POSTFIX, keyIdx);
                if (endIdx > 0) {
                    String newKey = value.substring(keyIdx, endIdx);
                    String newValue = substituteFromProfile(configs, VERSIONS_PID, newKey);
                    if (newValue != null) {
                        value = value.substring(0, startIdx) + newValue + value.substring(endIdx + 1);
                    }
                    replaced = true;
                }
            }
        } while (replaced);
        return value;
    }

    private static String substituteFromProfile(Map<String, Map<String, String>> configs, String pid, String key) {
        Map<String, String> configuration = configs.get(pid);
        if (configuration != null && configuration.containsKey(key)) {
            return configuration.get(key);
        } else {
            return EMPTY;
        }
    }

    @Override
    public String getScheme() {
        return RESOLVER_SCHEME;
    }

    @Override
    public String resolve(FabricService fabricService, Map<String, Map<String, String>> configs, String pid, String key, String value) {
        try {
            if (Strings.isNotBlank(value)) {
                // TODO: we should not use getCurrentContainer as we could substitue for another container
                String pidPey = value.substring(RESOLVER_SCHEME.length() + 1);
                String answer = substituteFromProfile(configs, VERSIONS_PID, pidPey);
                if (answer != null) {
                    answer = replaceVersions(configs, answer);
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Replaced value " + value + " with answer: " + answer);
                }
                return answer;
            }
        } catch (Exception e) {
            LOGGER.debug("Could not load property value: {} in version pid. Returning empty String.", value, e);
        }
        return EMPTY;
    }
}
