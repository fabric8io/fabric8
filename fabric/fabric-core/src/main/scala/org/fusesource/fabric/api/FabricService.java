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

import java.net.URI;

public interface FabricService {

    static final String DEFAULT_REPO_URI = "http://repo.fusesource.com/nexus/content/groups/public-snapshots/";

    Container[] getContainers();

    Container getContainer(String name);

    Container createContainer(String name);

    Container createContainer(String url, String name);

    Container createContainer(String url, String name, boolean isEnsembleServer, boolean debugContainer);

    /**
     * Creates multiple Containers.
     * Will create a number of Containers equal to the given number.
     * @param url
     * @param name
     * @param isEnsembleServer
     * @param debugContainer
     * @param number
     * @return
     */
    Container[] createContainers(String url, String name, boolean isEnsembleServer, boolean debugContainer, int number);

    Container createContainer(Container parent, String name);

    Container createContainer(Container parent, String name, boolean debugContainer);

    Container createContainer(CreateContainerArguments args, String name);

    /**
     * Create multiple containers where the name is used as a prefix
     */
    Container[] createContainer(CreateContainerArguments args, String name, int number);

    /**
     * Uses the given parent container to create the new container (so that locally
     * we don't have to have all the plugins like ssh and jclouds available)
     */
    Container createContainer(Container parent, CreateContainerArguments args, String name);

    Version getDefaultVersion();

    void setDefaultVersion( Version version );

    Version[] getVersions();

    Version getVersion(String name);

    Version createVersion(String version);

    Version createVersion(Version parent, String version);

    /**
     * Returns the current maven proxy repository to use to create new container
     */
    URI getMavenRepoURI();

    Profile[] getProfiles(String version);

    Profile getProfile(String version, String name);

    Profile createProfile(String version, String name);

    void deleteProfile(Profile profile);

    Container getCurrentContainer();

    String getCurrentContainerName();
}
