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

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * The Zookeeper based data store
 */
public interface DataStore {

    /**
     * Gets the fabric release version, eg such as 1.1.0
     */
    String getFabricReleaseVersion();

    //
    // Tracking
    //

    void fireChangeNotifications();
    
    void trackConfiguration(Runnable callback);
    void untrackConfiguration(Runnable callback);

    //
    // Container management
    //

    List<String> getContainers();

    boolean hasContainer(String containerId);

    String getContainerParent(String containerId);

    void deleteContainer(FabricService fabricService, String containerId);

    void createContainerConfig(CreateContainerOptions options);

    void createContainerConfig(CreateContainerMetadata metadata);

    CreateContainerMetadata getContainerMetadata(String containerId, ClassLoader classLoader);

    void setContainerMetadata(CreateContainerMetadata metadata);

    String getContainerVersion(String containerId);

    void setContainerVersion(String containerId, String versionId);

    List<String> getContainerProfiles(String containerId);

    void setContainerProfiles(String containerId, List<String> profileIds);

    boolean isContainerAlive(String id);

    void setContainerAlive(String id, boolean flag);



    public enum ContainerAttribute {
        BlueprintStatus,
        SpringStatus,
        ProvisionStatus,
        ProvisionException,
        ProvisionList,
        ProvisionChecksums,
        DebugPort,
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
    // Global information storage
    //

    String getDefaultJvmOptions();
    void setDefaultJvmOptions(String jvmOptions);

    FabricRequirements getRequirements();
    void setRequirements(FabricRequirements requirements) throws IOException;

    AutoScaleStatus getAutoScaleStatus();

    //Ensemble
    String getClusterId();
    List<String> getEnsembleContainers();
}
