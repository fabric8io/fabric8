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

package org.fusesource.fabric.api;

import java.io.IOException;
import java.net.URI;
import java.util.List;

public interface FabricService {

    final String DEFAULT_REPO_URI = "http://repo.fusesource.com/nexus/content/groups/public/";


    /**
     * Track configuration changes.
     * @param callback The Callback to call when a configuration change is detected.
     */
    void trackConfiguration(Runnable callback);

    /**
     * Gets the existing {@link Container}s.
     * @return An array of @{link Container}s
     */
    Container[] getContainers();

    /**
     * Finds the {@link Container} with the specified name.
     * @param name  The name of the {@link Container}.
     * @return      The {@link Container}.
     */
    Container getContainer(String name);

    /**
     * Creates one or more new {@link Container}s with the specified {@link CreateContainerOptions}.
     * @param options   The options for the creation of the {@link Container}.
     * @return          An array of metadata for the created {@llink Container}s
     */
    CreateContainerMetadata[] createContainers(CreateContainerOptions options);

    /**
     * Returns the default {@link Version}.
     * @return
     */
    Version getDefaultVersion();

    /**
     * Sets the default {@link Version}.
     * @param version
     */
    void setDefaultVersion(Version version);

    /**
     * Returns all {@link Version}s.
     * @return
     */
    Version[] getVersions();

    /**
     * Finds the {@link Version} with the specified name.
     * @param name  The name of the {@link Version}.
     * @return      The {@link Version} that matches the name.
     */
    Version getVersion(String name);

    /**
     * Creates a new {@link Version}.
     * @param version   The name of the {@link Version} to be created.
     * @return          The new {@link Version}.
     */
    Version createVersion(String version);

    /**
     * Creates a new {@link Version} with the specified parent {@link Version} and name.
     * @param parent        The parent {@link Version}
     * @param version       The name of the new {@link Version}.
     * @return
     */
    Version createVersion(Version parent, String version);

    /**
     * Returns the current maven proxy repository to use to create new container
     */
    URI getMavenRepoURI();

    List<URI> getMavenRepoURIs();

    /**
     * Returns the current maven proxy repository to use to deploy new builds to the fabric
     */
    URI getMavenRepoUploadURI();

    /**
     * Returns the pseudo url of the Zookeeper. It's not an actual url as it doesn't contain a scheme.
     * It's of the format <p>ip:port</p>
     * @return
     */
    String getZookeeperUrl();

    /**
     * Returns the password used to connect to Zookeeper.
     * @return
     */
    String getZookeeperPassword();

    /**
     * Returns all the {@link Profile}s for the specified {@link Version}.
     * @param version   The {@link Version} that will be used for querying {@link Profile}s.
     * @return          The matching {@link Profile}s.
     * @deprecated Use {@link Version#getProfiles()}
     */
    @Deprecated
    Profile[] getProfiles(String version);

    /**
     * Gets the {@link Profile} that matches the specified {@link Version} and name.
     * @param version
     * @param name
     * @return
     * @deprecated Use {@link Version#getProfile(String)}
     */
    @Deprecated
    Profile getProfile(String version, String name);

    /**
     * Creates a new {@link Profile} with the specified {@link Version} and name.
     * @param version   The string value of the {@link Version}.
     * @param name      The name of the new {@link Profile}.
     * @return
     * @deprecated Use {@link Version#createProfile(String)}
     */
    @Deprecated
    Profile createProfile(String version, String name);

    /**
     * Deletes the specified {@link Profile}.
     * @param profile
     * @deprecated Use Profile#delete() instead
     */
    @Deprecated
    void deleteProfile(Profile profile);

    /**
     * Returns the {@link Container} on which the method is executed.
     * @return
     */
    Container getCurrentContainer();

    /**
     * Returns the name of the current {@link Container}.
     * @return
     */
    String getCurrentContainerName();

    /**
     * Returns the fabric provisioning requirements if there are any defined
     * or empty requirements if none are defined.
     */
    FabricRequirements getRequirements();

    /**
     * Stores the fabric provisioning requirements
     */
    void setRequirements(FabricRequirements requirements) throws IOException;

    /**
     * Get the profile statuses of the fabric in terms of the current number of instances and their max/min requirements
     */
    FabricStatus getFabricStatus();

    /**
     * Get the patch support service which allow easy upgrades
     */
    PatchService getPatchService();

    /**
     * Get hte port service.
     * @return
     */
    PortService getPortService();

    /**
     * Get the default JVM options used when creating containers
     * @return
     */
    String getDefaultJvmOptions();

    /**
     * Set the default JVM options used when creating containers
     * @param jvmOptions
     */
    void setDefaultJvmOptions(String jvmOptions);

    /**
     * Returns the web app URL for the given web application (from its id)
     */
    String containerWebAppURL(String webAppId, String name);
}
