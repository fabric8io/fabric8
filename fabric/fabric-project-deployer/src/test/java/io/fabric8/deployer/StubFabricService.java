/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.deployer;

import io.fabric8.api.Container;
import io.fabric8.api.ContainerAutoScaler;
import io.fabric8.api.ContainerProvider;
import io.fabric8.api.CreateContainerBasicMetadata;
import io.fabric8.api.CreateContainerBasicOptions;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.CreateContainerOptions;
import io.fabric8.api.CreationStateListener;
import io.fabric8.api.DataStore;
import io.fabric8.api.FabricRequirements;
import io.fabric8.api.FabricService;
import io.fabric8.api.FabricStatus;
import io.fabric8.api.PatchService;
import io.fabric8.api.PortService;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 */
public class StubFabricService implements FabricService {
    @Override
    public String getEnvironment() {
        // TODO
        return null;
    }

    @Override
    public void trackConfiguration(Runnable callback) {
        // TODO

    }

    @Override
    public void untrackConfiguration(Runnable callback) {
        // TODO

    }

    @Override
    public Container[] getContainers() {
        // TODO
        return new Container[0];
    }

    @Override
    public Container getContainer(String name) {
        // TODO
        return null;
    }

    @Override
    public void startContainer(String containerId) {
        // TODO

    }

    @Override
    public void startContainer(String containerId, boolean force) {
        // TODO

    }

    @Override
    public void startContainer(Container container) {
        // TODO

    }

    @Override
    public void startContainer(Container container, boolean force) {
        // TODO

    }

    @Override
    public void stopContainer(String containerId) {
        // TODO

    }

    @Override
    public void stopContainer(String containerId, boolean force) {
        // TODO

    }

    @Override
    public void stopContainer(Container container) {
        // TODO

    }

    @Override
    public void stopContainer(Container container, boolean force) {
        // TODO

    }

    @Override
    public void destroyContainer(String containerId) {
        // TODO

    }

    @Override
    public void destroyContainer(String containerId, boolean force) {
        // TODO

    }

    @Override
    public void destroyContainer(Container container) {
        // TODO

    }

    @Override
    public void destroyContainer(Container container, boolean force) {
        // TODO

    }

    @Override
    public CreateContainerMetadata[] createContainers(CreateContainerOptions options) {
        // TODO
        return new CreateContainerMetadata[0];
    }

    @Override
    public CreateContainerMetadata[] createContainers(CreateContainerOptions options, CreationStateListener listener) {
        // TODO
        return new CreateContainerMetadata[0];
    }

    @Override
    public Set<Class<? extends CreateContainerBasicOptions>> getSupportedCreateContainerOptionTypes() {
        // TODO
        return null;
    }

    @Override
    public Set<Class<? extends CreateContainerBasicMetadata>> getSupportedCreateContainerMetadataTypes() {
        // TODO
        return null;
    }

    @Override
    public Version getDefaultVersion() {
        // TODO
        return null;
    }

    @Override
    public void setDefaultVersion(Version version) {
        // TODO

    }

    @Override
    public Version[] getVersions() {
        // TODO
        return new Version[0];
    }

    @Override
    public Version getVersion(String name) {
        // TODO
        return null;
    }

    @Override
    public Version createVersion(String version) {
        // TODO
        return null;
    }

    @Override
    public Version createVersion(Version parent, String version) {
        // TODO
        return null;
    }

    @Override
    public void deleteVersion(String version) {
        // TODO

    }

    @Override
    public ContainerProvider getProvider(String scheme) {
        // TODO
        return null;
    }

    @Override
    public URI getMavenRepoURI() {
        // TODO
        return null;
    }

    @Override
    public List<URI> getMavenRepoURIs() {
        // TODO
        return null;
    }

    @Override
    public URI getMavenRepoUploadURI() {
        // TODO
        return null;
    }

    @Override
    public String getZookeeperUrl() {
        // TODO
        return null;
    }

    @Override
    public String getZookeeperPassword() {
        // TODO
        return null;
    }

    @Override
    public Profile[] getProfiles(String version) {
        // TODO
        return new Profile[0];
    }

    @Override
    public Profile getProfile(String version, String name) {
        // TODO
        return null;
    }

    @Override
    public Profile createProfile(String version, String name) {
        // TODO
        return null;
    }

    @Override
    public void deleteProfile(Profile profile) {
        // TODO

    }

    @Override
    public Container getCurrentContainer() {
        // TODO
        return null;
    }

    @Override
    public String getCurrentContainerName() {
        // TODO
        return null;
    }

    @Override
    public FabricRequirements getRequirements() {
        // TODO
        return null;
    }

    @Override
    public void setRequirements(FabricRequirements requirements) throws IOException {
        // TODO

    }

    @Override
    public FabricStatus getFabricStatus() {
        // TODO
        return null;
    }

    @Override
    public PatchService getPatchService() {
        // TODO
        return null;
    }

    @Override
    public PortService getPortService() {
        // TODO
        return null;
    }

    @Override
    public DataStore getDataStore() {
        // TODO
        return null;
    }

    @Override
    public String getDefaultJvmOptions() {
        // TODO
        return null;
    }

    @Override
    public void setDefaultJvmOptions(String jvmOptions) {
        // TODO

    }

    @Override
    public String containerWebAppURL(String webAppId, String name) {
        // TODO
        return null;
    }

    @Override
    public String getConfigurationValue(String versionId, String profileId, String pid, String key) {
        // TODO
        return null;
    }

    @Override
    public void setConfigurationValue(String versionId, String profileId, String pid, String key, String value) {
        // TODO

    }

    @Override
    public boolean scaleProfile(String profile, int numberOfInstances) throws IOException {
        // TODO
        return false;
    }

    @Override
    public ContainerAutoScaler createContainerAutoScaler() {
        // TODO
        return null;
    }
}
