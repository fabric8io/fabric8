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

/**
 * Resolves properties by first looking at itself and then to a falback resolver.
 *
 * @author Alin Dreghiciu
 * @since 0.5.0, January 16, 2008
 */
public abstract class FallbackPropertyResolver
        implements PropertyResolver {

    /**
     * Fallback resolver
     */
    private PropertyResolver m_fallbackResolver;

    /**
     * Creates a property resolver with a fallback resolver.
     *
     * @param fallbackResolver rersolver to use to resolve properties
     */
    public FallbackPropertyResolver(final PropertyResolver fallbackResolver) {
        m_fallbackResolver = fallbackResolver;
    }

    /**
     * Resolves a property based on its name by first calling the findProperty() and then fallback to falback resolver
     * if property value is null.
     *
     * @param propertyName property name to be resolved
     * @return value of property or null if property is not set or is empty.
     */
    public String get(final String propertyName) {
        String value = findProperty(propertyName);
        if (value == null && m_fallbackResolver != null) {
            value = m_fallbackResolver.get(propertyName);
        }
        return value;
    }

    /**
     * Resolves a property by name.
     *
     * @param propertyName name of the property to be resolved.
     * @return value of property
     */
    protected abstract String findProperty(String propertyName);

}
