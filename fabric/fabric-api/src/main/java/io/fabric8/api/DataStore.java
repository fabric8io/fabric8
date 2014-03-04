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
package io.fabric8.api;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Stan Lewis
 */
public interface DataStore {

    public static final String ATTRIBUTE_PREFIX = "attribute.";

    public static final String DATASTORE_TYPE_PROPERTY = "type";
    public static final String DEFAULT_DATASTORE_TYPE = "caching-git";

    /**
     * Return the DataStore type.
     * @return
     */
    String getType();


    //
    // Import
    //

    void importFromFileSystem(String from);

    //
    // Tracking
    //

    void trackConfiguration(Runnable callback);
    void untrackConfiguration(Runnable callback);

    //
    // Container management
    //

    List<String> getContainers();

    boolean hasContainer(String containerId);

    String getContainerParent(String containerId);

    void deleteContainer(String containerId);

    void createContainerConfig(CreateContainerOptions options);

    void createContainerConfig(CreateContainerMetadata metadata);

    CreateContainerMetadata getContainerMetadata(String containerId, ClassLoader classLoader);

    void setContainerMetadata(CreateContainerMetadata metadata);

    String getContainerVersion(String containerId);

    void setContainerVersion(String containerId, String versionId);

    List<String> getContainerProfiles(String containerId);

    void setContainerProfiles(String containerId, List<String> profileIds);

    boolean isContainerAlive(String id);


    public enum ContainerAttribute {
        BlueprintStatus,
        SpringStatus,
        ProvisionStatus,
        ProvisionException,
        ProvisionList,
        ProvisionChecksums,
        Location,
        GeoLocation,
        Resolver,
        Ip,
        LocalIp,
        LocalHostName,
        PublicIp,
        PublicHostName,
        ManualIp,
        BindAddress,
        SshUrl,
        JmxUrl,
        JolokiaUrl,
        HttpUrl,
        PortMin,
        PortMax,
        Domains,
        ProcessId,
        OpenShift
    }

    String getContainerAttribute(String containerId, ContainerAttribute attribute, String def, boolean mandatory, boolean substituted);

    void setContainerAttribute(String containerId, ContainerAttribute attribute, String value);

    //
    // Default version
    //

    String getDefaultVersion();

    void setDefaultVersion(String versionId);

    //
    // Version management
    //

    List<String> getVersions();

    boolean hasVersion(String name);

    void createVersion(String version);

    void createVersion(String parentVersionId, String toVersion);

    void deleteVersion(String version);

    Map<String, String> getVersionAttributes(String version);

    void setVersionAttribute(String version, String key, String value);

    //
    // Profile management
    //

    List<String> getProfiles(String version);

    boolean hasProfile(String version, String profile);

    void createProfile(String version, String profile);

    String getProfile(String version, String profile, boolean create);

    void deleteProfile(String version, String profile);

    Map<String, String> getProfileAttributes(String version, String profile);

    void setProfileAttribute(String version, String profile, String key, String value);

    String getLastModified(String version, String profile);

    /**
     * Lists the files for the given profiles with the optional extra relative path
     *
     * @param version the version of the profiles to look at
     * @param profiles the list of profiles to look into; using values from the first profiles overlaying
     *                 later profiles
     * @param path if null then the root configuration directory is listed for the profile
     */
    Collection<String> listFiles(String version, Iterable<String> profiles, String path);

    // File configurations, including Map based configurations


    List<String> getConfigurationFileNames(String version, String id);

    Map<String, byte[]> getFileConfigurations(String version, String profile);

    byte[] getFileConfiguration(String version, String profile, String name);

    void setFileConfigurations(String version, String profile, Map<String, byte[]> configurations);

    void setFileConfiguration(String version, String profile, String name, byte[] configuration);

    // Map based configurations

    Map<String, Map<String, String>> getConfigurations(String version, String profile);

    Map<String, String> getConfiguration(String version, String profile, String pid);

    void setConfigurations(String version, String profile, Map<String, Map<String, String>> configurations);

    void setConfiguration(String version, String profile, String pid, Map<String, String> configuration);

    //
    // Global information storage
    //

    String getDefaultJvmOptions();
    void setDefaultJvmOptions(String jvmOptions);

    FabricRequirements getRequirements();
    void setRequirements(FabricRequirements requirements) throws IOException;

    //Ensemble
    String getClusterId();
    List<String> getEnsembleContainers();

    Map<String, String> getDataStoreProperties();
}
