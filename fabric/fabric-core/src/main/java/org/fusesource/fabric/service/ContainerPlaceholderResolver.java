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
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.DataStore;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.PlaceholderResolver;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.api.scr.ValidatingReference;
import org.osgi.service.component.ComponentContext;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ThreadSafe
@Component(name = "org.fusesource.fabric.placholder.resolver.container", description = "Fabric Container Placeholder Resolver")
@Service(PlaceholderResolver.class)
public final class ContainerPlaceholderResolver extends AbstractComponent implements PlaceholderResolver {

    private static final String NAME_ATTRIBUTE = "name";
    private static final String CONTAINER_SCHEME = "container";
    private static final String NAME_PATTERN = "[a-zA-Z0-9]+[a-zA-Z0-9_-]*";
    private static final Pattern NAMED_CONTAINER_PATTERN = Pattern.compile(CONTAINER_SCHEME + ":(" + NAME_PATTERN + ")/(" + NAME_PATTERN + ")");
    private static final Pattern CURRENT_CONTAINER_PATTERN = Pattern.compile(CONTAINER_SCHEME + ":(" + NAME_PATTERN + ")");

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    //Have a map of attribute based on the name in lower-case, can work regardless of the attribute case.
    private static final Map<String, DataStore.ContainerAttribute> attributes;
    static {
        HashMap<String, DataStore.ContainerAttribute> auxatts = new HashMap<String, DataStore.ContainerAttribute>();
        for (DataStore.ContainerAttribute attr : EnumSet.allOf(DataStore.ContainerAttribute.class)) {
            auxatts.put(attr.name().toLowerCase(), attr);
        }
        attributes = Collections.unmodifiableMap(auxatts);
    }

    @Activate
    synchronized void activate(ComponentContext context) {
        activateComponent();
    }

    @Deactivate
    synchronized void deactivate() {
        deactivateComponent();
    }

    @Override
    public String getScheme() {
        return CONTAINER_SCHEME;
    }

    @Override
    public String resolve(Map<String, Map<String, String>> configs, String pid, String key, String value) {
        assertValid();
        Matcher namedMatcher = NAMED_CONTAINER_PATTERN.matcher(value);
        Matcher currentMatcher = CURRENT_CONTAINER_PATTERN.matcher(value);
        if (namedMatcher.matches()) {
            String name = namedMatcher.group(1);
            String attribute = namedMatcher.group(2);
           return getContainerAttribute(fabricService.get(), name, attribute);
        } else if (currentMatcher.matches()) {
            String attribute = currentMatcher.group(1);
            return getContainerAttribute(fabricService.get(), fabricService.get().getCurrentContainerName(), attribute);
        }

        return "";
    }

    private String getContainerAttribute(FabricService fabricService, String name, String attribute) {
        Container container = fabricService.getContainer(name);
        if (NAME_ATTRIBUTE.equals(attribute)) {
            return container.getId();
        } else {
            return fabricService.getDataStore().getContainerAttribute(container.getId(), attributes.get(attribute.toLowerCase()), "", false, true);
        }
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }
}


