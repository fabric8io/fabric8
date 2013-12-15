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

import java.util.Map;

/**
 * Abstracts access to maven settings.xml.
 *
 * @author Alin Dreghiciu
 * @since August 10, 2007
 */
public interface MavenSettings {

    /**
     * Gets the list of repositories from settings.xml. If there is no settings.xml file or there are no repositories
     * in settings.xml the list returned will be null.
     *
     * @return a comma separated list of repositories from settings.xml
     */
    String getRepositories();

    /**
     * Returns the local repository directory from settings.xml.
     *
     * @return the local repository directory
     */
    String getLocalRepository();

    /**
     * Returns the active proxy settings from settings.xml
     *
     * @return the active proxy settings
     */
    Map<String, Map<String, String>> getProxySettings();
}
