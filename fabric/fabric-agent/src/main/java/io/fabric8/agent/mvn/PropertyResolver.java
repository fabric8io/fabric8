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
 * Properties resolver.
 *
 * @author Alin Dreghiciu
 * @since 0.5.0, January 16, 2008
 */
public interface PropertyResolver {

    /**
     * Resolves a property based on it's name.
     *
     * @param propertyName property name to be resolved
     * @return value of property or null if property is not set or is empty.
     */
    String get(String propertyName);

}
