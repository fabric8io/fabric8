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
package org.fusesource.fabric.api.jmx;

import org.fusesource.fabric.api.ContainerProvider;
import org.fusesource.fabric.api.FabricRequirements;
import org.fusesource.fabric.api.FabricStatus;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * An MBean for use by a <a href="http://jolokia.org/">Jolokia</a> JMX connector.
 *
 * <bNote></b> this MBean will not be usable remotely unless you use Jolokia or you have Fabric on your classpath!
 */
public interface FabricManagerMBean {

    ServiceStatusDTO getFabricServiceStatus();

    Map<String, String> createContainers(Map<String, String> options);

    ProfileDTO createProfile(String version, String name);

    ProfileDTO createProfile(String version, String name, List<String> parents);

    ProfileDTO changeProfileParents(String version, String name, List<String> parents);

    VersionDTO createVersion();
    VersionDTO createVersion(String version);
    VersionDTO createVersion(String parentVersionId, String toVersion);

    void deleteProfile(String versionId, String profileId);

    void deleteVersion(String version);

    void destroyContainer(String containerId);

    ContainerDTO getContainer(String name);

    void addProfilesToContainer(String container, List<String> profiles);

    void removeProfilesFromContainer(String container, List<String> profiles);

    void applyVersionToContainers(String version, List<String> containers);

    void applyProfilesToContainers(String version, List<String> profiles, List<String> containers);

    List<String> getContainerProvisionList(String name);

    List<ContainerDTO> containers();
    
    String[] containerIds();

    List<String> containerIdsForProfile(String versionId, String profileId);

    List<ContainerDTO> containersForProfile(String versionId, String profileId);

    List<String> containerIdsForVersion(String versionId);

    List<ContainerDTO> containersForVersion(String versionId);
/*
    ContainerTemplate getContainerTemplate(String containerId, String jmxUser, String jmxPassword);
*/

    ContainerDTO currentContainer();

    String getCurrentContainerName();

    String getDefaultJvmOptions();

    String getDefaultRepo();

    VersionDTO defaultVersion();

    FabricStatus fabricStatus();

    String getMavenRepoUploadURI();

    String getMavenRepoURI();

    ProfileDTO getProfile(String versionId, String profileId);

    List<String> getProfileIds(String versionId);

    List<ProfileDTO> getProfiles(String versionId);
    
    void deleteConfigurationFile(String versionId, String profileId, String fileName);
    
    byte[] getConfigurationFile(String versionId, String profileId, String fileName);
    
    void setConfigurationFile(String versionId, String profileId, String fileName, byte[] data);

/*
    ContainerProvider getProvider(String containerId);

    ContainerProvider getProvider(String scheme);

    Map<String, ContainerProvider> providers();

*/
    FabricRequirements requirements();

    VersionDTO getVersion(String name);

    List<VersionDTO> versions();

    String getZookeeperInfo(String name);

    String getZookeeperUrl();

    void registerProvider(ContainerProvider provider, Map<String, Object> properties);

    void registerProvider(String scheme, ContainerProvider provider);

    void setDefaultJvmOptions(String jvmOptions);

    void setDefaultRepo(String defaultRepo);

    void setDefaultVersion(String versionId);

    void requirements(FabricRequirements requirements) throws IOException;

    void startContainer(String containerId);

    void stopContainer(String containerId);

    void unregisterProvider(ContainerProvider provider, Map<String, Object> properties);

    void unregisterProvider(String scheme);

}
