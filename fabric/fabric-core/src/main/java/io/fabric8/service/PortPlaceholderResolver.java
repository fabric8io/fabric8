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
import io.fabric8.utils.Ports;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

@ThreadSafe
@Component(name = "io.fabric8.placholder.resolver.port", label = "Fabric8 Port Placeholder Resolver", immediate = true, metatype = false)
@Service({ PlaceholderResolver.class, PortPlaceholderResolver.class })
@Properties({ @Property(name = "scheme", value = PortPlaceholderResolver.RESOLVER_SCHEME) })
public final class PortPlaceholderResolver extends AbstractComponent implements PlaceholderResolver {

    public static final String RESOLVER_SCHEME = "port";
    private static final Pattern PORT_PROPERTY_URL_PATTERN = Pattern.compile("port:([\\d]+),([\\d]+)");

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

    /**
     * Returns the next free port number, starting from the specified value.
     * The port returned is also bound for the pid, so that it can be reused by the same pid in the future.
     * If the pid gets deleted or the property gets removed, the port will be unbound.
     */
    @Override
    public String resolve(FabricService fabricService, Map<String, Map<String, String>> configs, String pid, String key, String value) {
        Matcher matcher = PORT_PROPERTY_URL_PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Value doesn't match the port substitution pattern: port:<from port>,<to port>");
        }

        String fromPortValue = matcher.group(1);
        String toPortValue = matcher.group(2);

        int fromPort = Integer.parseInt(fromPortValue);
        int toPort = Integer.parseInt(toPortValue);
        Set<Integer> locallyAllocatedPorts = Ports.findUsedPorts(fromPort, toPort);
        int port = fabricService.getPortService().registerPort(fabricService.getCurrentContainer(), pid, key, fromPort, toPort, locallyAllocatedPorts);
        return String.valueOf(port);
    }
}
