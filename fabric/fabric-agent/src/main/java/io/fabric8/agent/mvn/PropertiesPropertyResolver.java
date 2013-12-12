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
package io.fabric8.agent.mvn;

import java.util.Properties;

/**
 * Resolves properties from a Properties.
 *
 * @author Alin Dreghiciu
 * @since 0.5.0, January 16, 2008
 */
public class PropertiesPropertyResolver
        extends FallbackPropertyResolver {

    /**
     * Properties to resolve properties from. Can be null.
     */
    private Properties m_properties;

    /**
     * Creates a property resolver without a fallback resolver.
     *
     * @param properties properties; optional
     */
    public PropertiesPropertyResolver(final Properties properties) {
        this(properties, null);
    }

    /**
     * Creates a property resolver with a fallback resolver.
     *
     * @param properties       properties; optional
     * @param fallbackResolver fallback resolver
     */
    public PropertiesPropertyResolver(final Properties properties,
                                      final PropertyResolver fallbackResolver) {
        super(fallbackResolver);
        m_properties = properties;
    }

    /**
     * Sets the properties in use.
     *
     * @param properties properties
     */
    public void setProperties(final Properties properties) {
        m_properties = properties;
    }

    /**
     * Resolves a property based on it's name .
     *
     * @param propertyName property name to be resolved
     * @return value of property or null if property is not set or is empty.
     */
    public String findProperty(final String propertyName) {
        String value = null;
        if (m_properties != null) {
            value = m_properties.getProperty(propertyName);
        }
        return value;
    }

}