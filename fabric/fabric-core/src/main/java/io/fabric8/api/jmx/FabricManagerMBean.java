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
package io.fabric8.api.jmx;

import io.fabric8.api.ContainerProvider;
import io.fabric8.api.FabricRequirements;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * An MBean for use by a <a href="http://jolokia.org/">Jolokia</a> JMX connector.
 *
 * <bNote></b> this MBean will not be usable remotely unless you use Jolokia or you have Fabric on your classpath!
 */
public interface FabricManagerMBean {

    List<String> getFields(String className);

    ServiceStatusDTO getFabricServiceStatus();

    Map<String, String> createContainers(Map<String, Object> options);

    Map<String, Object> createProfile(String version, String name);

    Map<String, Object> createProfile(String version, String name, List<String> parents);

    Map<String, Object> changeProfileParents(String version, String name, List<String> parents);

    /**
     * Returns the web app URL of the given webAppId, profile and version
     */
    String profileWebAppURL(String webAppId, String profileId, String versionId);

    /**
     * Returns the web app URL of the given given webAppId and container name
     */
    String containerWebAppURL(String webAppId, String containerName);

    Map<String, Object> createVersion();
    Map<String, Object> createVersion(String version);
    Map<String, Object> createVersion(String parentVersionId, String toVersion);

    void deleteProfile(String versionId, String profileId);

    void deleteProfile(String versionId, String profileId, boolean force);

    void refreshProfile(String versionId, String profileId);

    void deleteVersion(String version);

    void destroyContainer(String containerId);

    Map<String, Object> getContainer(String name);

    Map<String, Object> getContainer(String name, List<String> fields);

    void addProfilesToContainer(String container, List<String> profiles);

    void removeProfilesFromContainer(String container, List<String> profiles);

    void applyVersionToContainers(String version, List<String> containers);

    void applyProfilesToContainers(String version, List<String> profiles, List<String> containers);

    @Deprecated
    List<String> getContainerProvisionList(String name);

    List<Map<String, Object>> containers();

    List<Map<String, Object>> containers(List<String> fields);

    List<Map<String, Object>> containers(List<String> fields, List<String> profileFields);

    String containerMetadataType(String id);

    String containerCreateOptionsType(String id);

    void changeCreateOptionsField(String containerId, String field, Object value);

    @Deprecated
    String[] containerIds();

    @Deprecated
    List<String> containerIdsForProfile(String versionId, String profileId);

    List<Map<String, Object>> containersForProfile(String versionId, String profileId);

    List<Map<String, Object>> containersForProfile(String versionId, String profileId, List<String> fields);

    void setContainerProperty(String containerId, String property, Object value);

    @Deprecated
    List<String> containerIdsForVersion(String versionId);

    List<Map<String, Object>> containersForVersion(String versionId);

    List<Map<String, Object>> containersForVersion(String versionId, List<String> fields);

/*
    ContainerTemplate getContainerTemplate(String containerId, String jmxUser, String jmxPassword);
*/

    Map<String, Object> currentContainer();

    String getCurrentContainerName();

    String getDefaultJvmOptions();

    String getDefaultRepo();

    Map<String, Object> defaultVersion();

    String getDefaultVersion();

    FabricStatusDTO fabricStatus();

    String getMavenRepoUploadURI();

    String getMavenRepoURI();

    Map<String, Object> getProfile(String versionId, String profileId);

    Map<String, Object> getProfile(String versionId, String profileId, List<String> fields);

    @Deprecated
    List<String> getProfileIds(String versionId);

    Map<String, Object> getProfileFeatures(String versionId, String profileId);

    List<Map<String, Object>> getProfiles(String versionId);

    List<Map<String, Object>> getProfiles(String versionId, List<String> fields);

    void deleteConfigurationFile(String versionId, String profileId, String fileName);

    /**
     * Returns all of the configuration files available in the given profile (with inheritance) returning a map of the file
     * name to the profile which defines the
     */
    List<String> getConfigurationFileNames(String versionId, String profileId);

    String getConfigurationFile(String versionId, String profileId, String fileName);

    /**
     * Returns a map of all the current configuration files in the profiles of the current container with the file name as the key and the profile ID as the value
     */
    Map<String, String> currentContainerConfigurationFiles();

    Map<String, Object> getConfigurationFiles(String versionId, List<String> profileIds, String regex);
    
    void setConfigurationFile(String versionId, String profileId, String fileName, String data);

    /**
     * Returns the profile properties for the given PID
     */
    Map<String,String> getProfileProperties(String versionId, String profileId, String pid);

    /**
     * Sets the profile properties for the given PID
     */
    boolean setProfileProperties(String versionId, String profileId, String pid, Map<String, String> properties);

    /**
     * Returns the profile property value for the given PID and propery name
     */
    String getProfileProperty(String versionId, String profileId, String pid, String propertyName);

    /**
     * Sets the profile properties for the given PID and property name
     */
    String setProfileProperty(String versionId, String profileId, String pid, String propertyName, String value);

    void setProfileBundles(String versionId, String profileId, List<String> bundles);

    void setProfileFeatures(String versionId, String profileId, List<String> features);

    void setProfileRepositories(String versionId, String profileId, List<String> repositories);

    void setProfileFabs(String versionId, String profileId, List<String> fabs);

    void setProfileOverrides(String versionId, String profileId, List<String> overrides);

    void setProfileAttribute(String versionId, String profileId, String attributeId, String value);

    /*
        ContainerProvider getProvider(String containerId);

        ContainerProvider getProvider(String scheme);

        Map<String, ContainerProvider> providers();

    */
    FabricRequirements requirements();

    Map<String, Object> getVersion(String versionId);

    Map<String, Object> getVersion(String versionId, List<String> fields);

    List<Map<String, Object>> versions();

    List<Map<String, Object>> versions(List<String> fields);

    void copyProfile(String versionId, String sourceId, String targetId, boolean force);

    void renameProfile(String versionId, String profileId, String newId, boolean force);

    String getZookeeperInfo(String name);

    String getZookeeperUrl();

    void registerProvider(ContainerProvider provider, Map<String, Object> properties);

    void registerProvider(String scheme, ContainerProvider provider);

    void setDefaultJvmOptions(String jvmOptions);

    void setDefaultRepo(String defaultRepo);

    void setDefaultVersion(String versionId);

    /**
     * Scales the given profile up or down in the number of instances required
     *
     *
     * @param profile the profile ID to change the requirements
     * @param numberOfInstances the number of instances to increase or decrease
     * @return true if the requiremetns changed
     */
    boolean scaleProfile(String profile, int numberOfInstances) throws IOException;

    void requirements(FabricRequirements requirements) throws IOException;

    /**
     * Allow the requirements to be passed in via JSON
     */
    void requirementsJson(String json) throws IOException;

    void startContainer(String containerId);

    List<Map<String, Object>> startContainers(List<String> containerIds);

    List<Map<String, Object>> stopContainers(List<String> containerIds);

    void stopContainer(String containerId);

    Map<String, String> registeredProviders();

    void unregisterProvider(ContainerProvider provider, Map<String, Object> properties);

    void unregisterProvider(String scheme);

    void applyPatches(List<String> files, String targetVersionId, String newVersionId, String proxyUser, String proxyPassword);

    String getConfigurationValue(String versionId, String profileId, String pid, String key);

    void setConfigurationValue(String versionId, String profileId, String pid, String key, String value);

    /**
     * Returns the JSON for the cluster status for the given path.
     *
     * @param clusterPathSegment if null or an empty string is used then the path "/fabric/registry/clusters" is used;
     *                           if the value starts with "/" then its treated as an absolute path otherwise
     *                           it is appended to "/fabric/registry/clusters/" to access a relative path.
     *                           e.g. you can supply "apis" to find all the APIs, or "webapps" for all the webapps.
     */
    String clusterJson(String clusterPathSegment) throws Exception;
}
