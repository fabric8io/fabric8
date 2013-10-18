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
import org.fusesource.fabric.utils.Strings;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ThreadSafe
@Component(name = "org.fusesource.fabric.placholder.resolver.versionprop", description = "Fabric Version Property Placeholder Resolver", immediate = true)
@Service(PlaceholderResolver.class)
public final class VersionPropertyPointerResolver extends AbstractComponent implements PlaceholderResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionPropertyPointerResolver.class);
    private static final String SCHEME = "version";

    private static final String EMPTY = "";

    public static final String VERSIONS_PID = "org.fusesource.fabric.version";
    public static final String VERSION_PREFIX = "${version:";
    public static final String VERSION_POSTFIX = "}";

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
    @Reference(referenceInterface = DataStore.class)
    private final ValidatingReference<DataStore> dataStore = new ValidatingReference<DataStore>();

    @Activate
    void activate(ComponentContext context) {
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
    public String resolve(String pid, String key, String value) {
        assertValid();
        try {
            if (Strings.isNotBlank(value)) {
                Profile profile = fabricService.get().getCurrentContainer().getOverlayProfile();
                String pidPey = value.substring(SCHEME.length() + 1);
                ProfileOverlayImpl profileOverlay = new ProfileOverlayImpl(profile, false, dataStore.get());
                String answer = substituteFromProfile(profileOverlay, VERSIONS_PID, pidPey);
                if (answer != null) {
                    answer = replaceVersions(profileOverlay, answer);
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

    /**
     * Lets replace any other instances of ${version:key} with the value
     */
    public static String replaceVersions(Profile profileOverlay, String value) {
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
                    String newValue = substituteFromProfile(profileOverlay, VERSIONS_PID, newKey);
                    if (newValue != null) {
                        value = value.substring(0, startIdx) + newValue + value.substring(endIdx + 1);
                    }
                    replaced = true;
                }
            }
        } while (replaced);
        return value;
    }

    private static String substituteFromProfile(Profile profile, String pid, String key) {
        Map<String, String> configuration = profile.getConfiguration(pid);
        if (configuration != null && configuration.containsKey(key)) {
            return configuration.get(key);
        } else return EMPTY;
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
