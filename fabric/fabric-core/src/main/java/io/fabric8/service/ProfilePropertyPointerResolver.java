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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
@Component(name = "io.fabric8.placholder.resolver.profileprop", label = "Fabric8 Profile Property Placeholder Resolver", immediate = true, metatype = false)
@Service({ PlaceholderResolver.class, ProfilePropertyPointerResolver.class })
@Properties({ @Property(name = "scheme", value = ProfilePropertyPointerResolver.RESOLVER_SCHEME) })
public final class ProfilePropertyPointerResolver extends AbstractComponent implements PlaceholderResolver {

    public static final String RESOLVER_SCHEME = "profile";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfilePropertyPointerResolver.class);

    private static final Pattern OVERLAY_PROFILE_PROPERTY_URL_PATTERN = Pattern.compile("profile:([^ /]+)/([^ =/]+)");

    private static final String EMPTY = "";

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public String getScheme() {
        return RESOLVER_SCHEME;
    }

    @Override
    public String resolve(FabricService fabricService, Map<String, Map<String, String>> configs, String pid, String key, String value) {
        try {
            if (value != null) {
                Matcher overlayMatcher = OVERLAY_PROFILE_PROPERTY_URL_PATTERN.matcher(value);

                if (overlayMatcher.matches()) {
                    String targetPid = overlayMatcher.group(1);
                    String targetProperty = overlayMatcher.group(2);
                    return substituteFromProfile(configs, targetPid, targetProperty);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not load property value: {}. Returning empty String.", value, e);
        }
        return EMPTY;
    }

    private String substituteFromProfile(Map<String, Map<String, String>> configs, String pid, String key) {
        Map<String, String> configuration = configs.get(pid);
        if (configuration != null && configuration.containsKey(key)) {
            return configuration.get(key);
        } else {
            return EMPTY;
        }
    }
}
