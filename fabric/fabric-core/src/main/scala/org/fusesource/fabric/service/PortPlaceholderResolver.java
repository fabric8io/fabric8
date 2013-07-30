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
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.PlaceholderResolver;
import org.fusesource.fabric.utils.Ports;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component(name = "org.fusesource.fabric.placholder.resolver.port",
           description = "Fabric Port Placeholder Resolver", immediate = true)
@Service(PlaceholderResolver.class)
public class PortPlaceholderResolver implements PlaceholderResolver {

    private static final String PORT_SCHEME = "port";
    private static final Pattern PORT_PROPERTY_URL_PATTERN = Pattern.compile("port:([\\d]+),([\\d]+)");

    @Reference(cardinality = org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY)
    private FabricService fabricService;

    /**
     * The placeholder scheme.
     *
     * @return
     */
    @Override
    public String getScheme() {
        return PORT_SCHEME;
    }

    /**
     * Returns the next free port number, starting from the specified value.
     * The port returned is also bound for the pid, so that it can be reused by the same pid in the future.
     * If the pid gets deleted or the property gets removed, the port will be unbound.
     *
     * @param pid   The pid that contains the placeholder.
     * @param key   The key of the configuration value that contains the placeholder.
     * @param value The value with the placeholder.
     * @return The resolved value or EMPTY_STRING.
     */
    @Override
    public synchronized String resolve(String pid, String key, String value) {
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


    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }
}
