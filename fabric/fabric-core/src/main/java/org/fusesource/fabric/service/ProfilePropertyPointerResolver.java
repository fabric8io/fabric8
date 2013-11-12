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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.api.DataStore;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.PlaceholderResolver;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.api.scr.ValidatingReference;
import org.fusesource.fabric.internal.ProfileOverlayImpl;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
@Component(name = "org.fusesource.fabric.placholder.resolver.profileprop", description = "Fabric Profile Property Placeholder Resolver", immediate = true)
@Service(PlaceholderResolver.class)
public final class ProfilePropertyPointerResolver extends AbstractComponent implements PlaceholderResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfilePropertyPointerResolver.class);
    private static final String SCHEME = "profile";

    private static final Pattern OVERLAY_PROFILE_PROPERTY_URL_PATTERN = Pattern.compile("profile:([^ /]+)/([^ =/]+)");

    private static final String EMPTY = "";

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
    @Reference(referenceInterface = DataStore.class)
    private final ValidatingReference<DataStore> dataStore = new ValidatingReference<DataStore>();

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
        return SCHEME;
    }

    /**
     * Resolves the placeholder found inside the value, for the specific key of the pid.
     */
    @Override
    public String resolve(Map<String, Map<String, String>> configs, String pid, String key, String value) {
        assertValid();
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

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }

    void bindDataStore(DataStore dataStore) {
        this.dataStore.bind(dataStore);
    }

    void unbindDataStore(DataStore dataStore) {
        this.dataStore.unbind(dataStore);
    }
}
