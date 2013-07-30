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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.api.DataStore;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.PlaceholderResolver;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.internal.ProfileOverlayImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component(name = "org.fusesource.fabric.placholder.resolver.profileprop",
           description = "Fabric Profile Property Placeholder Resolver", immediate = true)
@Service(PlaceholderResolver.class)
public class ProfilePropertyPointerResolver implements PlaceholderResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfilePropertyPointerResolver.class);
    private static final String SCHEME = "profile";

    private static final Pattern OVERLAY_PROFILE_PROPERTY_URL_PATTERN = Pattern.compile("profile:([^ /]+)/([^ =/]+)");
    private static final Pattern EXPLICIT_PROFILE_PROPERTY_URL_PATTERN = Pattern.compile("profile:([^ /]+)/([^ =/]+)/([^ =/]+)");

    private static final String EMPTY = "";

    @Reference(cardinality = org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY)
    private FabricService fabricService;
    @Reference(cardinality = org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY)
    private DataStore dataStore;

    /**
     * The placeholder scheme.
     *
     * @return
     */
    @Override
    public String getScheme() {
        return SCHEME;
    }

    /**
     * Resolves the placeholder found inside the value, for the specific key of the pid.
     *
     * @param pid   The pid that contains the placeholder.
     * @param key   The key of the configuration value that contains the placeholder.
     * @param value The value with the placeholder.
     * @return The resolved value or empty string.
     */
    @Override
    public String resolve(String pid, String key, String value) {
        try {
            if (value != null) {
                Matcher overlayMatcher = OVERLAY_PROFILE_PROPERTY_URL_PATTERN.matcher(value);
                Matcher explicitMatcher = EXPLICIT_PROFILE_PROPERTY_URL_PATTERN.matcher(value);

                if (overlayMatcher.matches()) {
                    String targetPid = overlayMatcher.group(1);
                    String targetProperty = overlayMatcher.group(2);
                    Profile profile = fabricService.getCurrentContainer().getOverlayProfile();
                    return substituteFromProfile(new ProfileOverlayImpl(profile, false, dataStore), targetPid, targetProperty);
                } else if (explicitMatcher.matches()) {
                    String profileId = explicitMatcher.group(1);
                    String targetPid = explicitMatcher.group(2);
                    String targetProperty = explicitMatcher.group(3);
                    Profile profile = fabricService.getCurrentContainer().getVersion().getProfile(profileId);
                    return substituteFromProfile(profile, targetPid, targetProperty);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not load property value: {}. Returning empty String.", value, e);
        }
        return EMPTY;
    }

    private String substituteFromProfile(Profile profile, String pid, String key) {
        Map<String, String> configuration = profile.getConfigurations().get(pid);
        if (configuration.containsKey(key)) {
            return configuration.get(key);
        } else return EMPTY;
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public void setDataStore(DataStore dataStore) {
        this.dataStore = dataStore;
    }
}
