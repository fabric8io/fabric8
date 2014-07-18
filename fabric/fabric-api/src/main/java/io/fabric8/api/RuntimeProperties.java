/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.api;

import java.nio.file.Path;

/**
 * A runtime properties provider
 */
public interface RuntimeProperties {

    String RUNTIME_IDENTITY = "runtime.id";
    String RUNTIME_HOME_DIR = "runtime.home";
    String RUNTIME_DATA_DIR = "runtime.data";
    String RUNTIME_CONF_DIR = "runtime.conf";

    String DEFAULT_ENV_PREFIX = "FABRIC8_";

    /**
     * Get the identity for the current container
     */
    String getRuntimeIdentity();

    /**
     * Get the home path of the current runtime.
     */
    Path getHomePath();

    /**
     * Get the conf path of the current runtime.
     */
    Path getConfPath();

    /**
     * Get the data path of the current runtime.
     */
    Path getDataPath();

    String getProperty(String key);

    String getProperty(String key, String defaultValue);
    
    /**
     * Add a runtime attribute for the given key.
     * @throws IllegalStateException if an attribute with the given key 
     */
    <T> void putRuntimeAttribute(Class<T> key, T options);

    /**
     * Get a runtime attribute for the given key.
     * @return null if the attribute does not exist 
     */
    <T> T getRuntimeAttribute(Class<T> key);
    
    /**
     * Remove a runtime attribute for the given key. 
     * @return null if the attribute does not exist 
     */
    <T> T removeRuntimeAttribute(Class<T> key);

    /**
     * Clears the runtime attributes. 
     */
    <T> void clearRuntimeAttributes();
}
