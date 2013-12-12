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
package io.fabric8.fab.osgi.util;

import org.fusesource.common.util.Strings;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

import java.util.*;

/**
 * A set of helper methods for working with OSGi services
 */
public class Services {

    /**
     * Parse an OSGi Export-Service header into a set of {@link Service} representations
     *
     * @param header the header value
     * @return the set of services
     */
    public static Set<Service> parseHeader(String header) {
        Set<Service> services = new HashSet<Service>();
        if (Strings.notEmpty(header)) {
            Scanner scanner = new Scanner(header).useDelimiter(",");
            while (scanner.hasNext()) {
                services.add(Service.parse(scanner.next()));
            }
        }
        return services;
    }

    public static boolean isAvailable(BundleContext context, String className) throws InvalidSyntaxException {
        return isAvailable(context, new Service(className));
    }

    public static boolean isAvailable(BundleContext context, String className, Map<String,String> properties) throws InvalidSyntaxException {
        return isAvailable(context, new Service(className, properties));
    }

    public static boolean isAvailable(BundleContext context, Service service) throws InvalidSyntaxException {
        return service.isAvailable(context);
    }

    public static Map<String,String> createProperties(String... elements) {
        if (elements.length % 2 != 0) {
            throw new IllegalArgumentException("Expected an even number of elements");
        }
        Map<String, String> properties = new LinkedHashMap<String, String>();
        for (int i = 0; i < elements.length; i = i + 2) {
            properties.put(elements[i], elements[i + 1]);
        }
        return properties;
    }

    public static boolean isAvailable(BundleContext context, Set<Service> services) throws InvalidSyntaxException {
        for (Service service : services) {
            if (!service.isAvailable(context)) {
                return false;
            }
        }
        return true;
    }
}
