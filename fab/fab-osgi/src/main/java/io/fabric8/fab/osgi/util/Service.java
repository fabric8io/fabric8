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

import org.fusesource.common.util.Objects;
import org.fusesource.common.util.Strings;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an OSGi Service
 */
public class Service {

    private final String className;
    private final Map<String, String> properties;

    public Service(String className) {
        this(className, new HashMap<String, String>());
    }

    public Service(String className, Map<String, String> properties) {
        super();
        this.className = className;
        this.properties = properties;
    }

    public String getClassName() {
        return className;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(className, properties);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Service) {
            Service other = (Service) o;
            return Objects.equal(className, other.getClassName()) && Objects.equal(properties, other.getProperties());
        } else {
            return false;
        }
    }

    public static Service parse(String header) {
        String[] parts = header.split(";");
        Map<String, String> properties = new HashMap<String, String>();
        if (parts.length > 1) {
            for (int i = 1 ; i < parts.length ; i++) {
                String[] keyvalue = parts[i].split("=");                
                if (keyvalue.length == 2 && Strings.notEmpty(Strings.unquote(keyvalue[1]))) {
                    properties.put(keyvalue[0], Strings.unquote(keyvalue[1]));
                }
            }
        }
        return new Service(parts[0], properties);
    }

    protected boolean isAvailable(BundleContext context) throws InvalidSyntaxException {
        return context.getServiceReferences(className, getFilter()) != null;
    }

    /**
     * Determine the LDAP-like filter to be used to find a service reference
     *
     * @return the filter
     */
    public String getFilter() {
        if (properties.size() == 0) {
            return null;
        }
        StringBuffer buffer = new StringBuffer();
        if (properties.size() > 1) {
            buffer.append("(&");
        }
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            buffer.append(String.format("(%s=%s)", entry.getKey(), entry.getValue()));
        }
        if (properties.size() > 1) {
            buffer.append(")");
        }
        return buffer.toString();
    }
}
