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

public interface RuntimeService {

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

    /**
     * Get a property from the current runtime
     * @return the property value or null
     */
    String getProperty(String key);

    /**
     * Get a property from the current runtime
     * @return the property value or the given default value
     */
    String getProperty(String key, String defaultValue);
}
