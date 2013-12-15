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

import java.util.HashMap;
import java.util.Map;

/**
 * A simple generics based property store.
 *
 * @author Alin Dreghiciu
 * @since August 26, 2007
 */
public class PropertyStore {

    /**
     * Map of properties.
     */
    private Map<String, Object> m_properties;

    /**
     * Creates a new configuration map.
     */
    public PropertyStore() {
        m_properties = new HashMap<String, Object>();
    }

    /**
     * Returns true if the the property was set.
     *
     * @param propertyName name of the property
     * @return true if property is set
     */
    public boolean contains(final String propertyName) {
        return m_properties.containsKey(propertyName);
    }

    /**
     * Sets a property.
     *
     * @param propertyName  name of the property to set
     * @param propertyValue value of the property to set
     * @return the value of property set (fluent api)
     */
    public <T> T set(final String propertyName, final T propertyValue) {
        m_properties.put(propertyName, propertyValue);
        return propertyValue;
    }

    /**
     * Returns the property by name.
     *
     * @param propertyName name of the property
     * @return property value
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final String propertyName) {
        return (T) m_properties.get(propertyName);
    }

}
