/*
 * #%L
 * Gravia :: Runtime :: API
 * %%
 * Copyright (C) 2013 - 2014 JBoss by Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.fabric8.api.gravia;

/**
 * A provider for Runtime properties.
 *
 * @author thomas.diesler@jboss.com
 * @since 27-Sep-2013
 */
public interface PropertiesProvider {

    /**
     * Returns the value of the specified property. If the key is not found in
     * the Runtime properties, the system properties are then searched. The
     * method returns {@code null} if the property is not found.
     *
     * @param key The name of the requested property.
     * @return The value of the requested property, or {@code null} if the
     *         property is undefined.
     */
    Object getProperty(String key);

    /**
     * Returns the value of the specified property. If the key is not found in
     * the Runtime properties, the system properties are then searched. The
     * method throws an {@link IllegalStateException} if the property is not found.
     *
     * @param key The name of the requested property.
     * @return The value of the requested property
     * @throws IllegalStateException if the property is undefined.
     */
    Object getRequiredProperty(String key);
    
    /**
     * Returns the value of the specified property. If the key is not found in
     * the Runtime properties, the system properties are then searched. The
     * method returns provided default value if the property is not found.
     *
     * @param key The name of the requested property.
     * @return The value of the requested property, or the provided default value if the
     *         property is undefined.
     */
    Object getProperty(String key, Object defaultValue);
}
