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

import io.fabric8.api.Container;
import io.fabric8.api.DataStore;
import io.fabric8.api.FabricService;
import io.fabric8.api.PlaceholderResolver;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

@ThreadSafe
@Component(name = "io.fabric8.placholder.resolver.container", label = "Fabric8 Container Placeholder Resolver", metatype = false)
@Service({ PlaceholderResolver.class, ContainerPlaceholderResolver.class })
@Properties({ @Property(name = "scheme", value = ContainerPlaceholderResolver.RESOLVER_SCHEME) })
public final class ContainerPlaceholderResolver extends AbstractComponent implements PlaceholderResolver {

    public static final String RESOLVER_SCHEME = "container";

    private static final String NAME_ATTRIBUTE = "name";
    private static final String NAME_PATTERN = "[a-zA-Z0-9]+[a-zA-Z0-9_-]*";
    private static final Pattern NAMED_CONTAINER_PATTERN = Pattern.compile(RESOLVER_SCHEME + ":(" + NAME_PATTERN + ")/(" + NAME_PATTERN + ")");
    private static final Pattern CURRENT_CONTAINER_PATTERN = Pattern.compile(RESOLVER_SCHEME + ":(" + NAME_PATTERN + ")");

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
        Matcher namedMatcher = NAMED_CONTAINER_PATTERN.matcher(value);
        Matcher currentMatcher = CURRENT_CONTAINER_PATTERN.matcher(value);
        if (namedMatcher.matches()) {
            String name = namedMatcher.group(1);
            String attribute = namedMatcher.group(2);
            return getContainerAttribute(fabricService, name, attribute);
        } else if (currentMatcher.matches()) {
            String attribute = currentMatcher.group(1);
            return getContainerAttribute(fabricService, fabricService.getCurrentContainerName(), attribute);
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
}
