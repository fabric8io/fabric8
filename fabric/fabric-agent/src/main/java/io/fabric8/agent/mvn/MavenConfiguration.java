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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Handler configuration.
 *
 * @author Alin Dreghiciu
 * @since August 11, 2007
 */
public interface MavenConfiguration {

    /**
     * Returns true if the certificate should be checked on SSL connection, false otherwise.
     *
     * @return true if the certificate should be checked
     */
    Boolean getCertificateCheck();

    /**
     * Returns the URL of maven settings file.
     *
     * @return the url to settings file
     */
    URL getSettingsFileUrl();

    /**
     * Returns a list of default repositories to be searched before any other repositories.
     *
     * @return a list of default repositories.  List can be null or empty if there are not default repositories to be searched.
     */
    List<MavenRepositoryURL> getDefaultRepositories()
            throws MalformedURLException;

    /**
     * Returns a list of repositories to be searched.
     *
     * @return a list of repositories. List can be null or empty if there are no repositories to be searched.
     */
    List<MavenRepositoryURL> getRepositories()
            throws MalformedURLException;

    /**
     * Returns the url of local repository.
     *
     * @return url of local repository. Can be null if there is no local repository.
     */
    MavenRepositoryURL getLocalRepository();

    /**
     * Returns true if the fallback repositories should be used instead of default ones.
     * Default value is false.
     *
     * @return true if the fallback repositories should
     */
    Boolean useFallbackRepositories();

    /**
     * Enables the proxy server for a given URL.
     */
    void enableProxy(URL url);

    /**
     * Returns if aether should be disabled or not.
     *
     * @return true if aether should be disabled
     */
    Boolean isAetherDisabled();
}
