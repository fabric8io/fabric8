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
package io.fabric8.core.jmx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.api.AutoScaleStatus;
import io.fabric8.api.Constants;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerProvider;
import io.fabric8.api.CreateContainerBasicMetadata;
import io.fabric8.api.CreateContainerBasicOptions;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.CreateContainerOptions;
import io.fabric8.api.DataStore;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricRequirements;
import io.fabric8.api.Ids;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileRegistry;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Profiles;
import io.fabric8.api.Version;
import io.fabric8.api.VersionSequence;
import io.fabric8.api.jmx.FabricManagerMBean;
import io.fabric8.api.jmx.FabricStatusDTO;
import io.fabric8.api.jmx.ServiceStatusDTO;
import io.fabric8.common.util.PublicPortMapper;
import io.fabric8.common.util.ShutdownTracker;
import io.fabric8.common.util.Strings;
import io.fabric8.service.FabricServiceImpl;
import org.apache.commons.codec.binary.Base64;
import org.apache.curator.framework.CuratorFramework;
import io.fabric8.api.gravia.IllegalStateAssertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getChildrenSafe;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getSubstitutedData;

/**
 * [TODO] Review FabricManager for profile consistency
 */
public final class FabricManager implements FabricManagerMBean {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabricManager.class);

    private final ProfileService profileService;
    private final FabricServiceImpl fabricService;
    private ObjectName objectName;

    public FabricManager(FabricServiceImpl fabricService) {
        this.profileService = fabricService.adapt(ProfileService.class);
        this.fabricService = fabricService;
    }

    public ObjectName getObjectName() throws MalformedObjectNameException {
        if (objectName == null) {
            // TODO to avoid mbean clashes if ever a JVM had multiple FabricService instances, we may
            // want to add a parameter of the fabric ID here...
            objectName = new ObjectName("io.fabric8:type=Fabric");
        }
        return objectName;
    }

    public void setObjectName(ObjectName objectName) {
        this.objectName = objectName;
    }

    public void registerMBeanServer(ShutdownTracker shutdownTracker, MBeanServer mbeanServer) {
        try {
            ObjectName name = getObjectName();
			if (!mbeanServer.isRegistered(name)) {
		        StandardMBean mbean = new StandardMBean(this, FabricManagerMBean.class);
				mbeanServer.registerMBean(mbean, name);
			}
		} catch (Exception e) {
            LOG.warn("An error occurred during mbean server registration: " + e, e);
        }
    }

    public void unregisterMBeanServer(MBeanServer mbeanServer) {
        if (mbeanServer != null) {
            try {
                ObjectName name = getObjectName();
				if (mbeanServer.isRegistered(name)) {
					mbeanServer.unregisterMBean(name);
				}
			} catch (Exception e) {
                LOG.warn("An error occurred during mbean server registration: " + e, e);
            }
        }
    }

    // Management API
    // -------------------------------------------------------------------------

    @Override
    public String getFabricEnvironment() {
        return fabricService.getEnvironment();
    }

    @Override
    public List<String> getFields(String className) {
        try {
            return BeanUtils.getFields(Class.forName(className));
        } catch (ClassNotFoundException e) {
            throw new FabricException("Failed to load class " + className, e);
        }
    }

    @Override
    public ServiceStatusDTO getFabricServiceStatus() {
        ServiceStatusDTO rc = new ServiceStatusDTO();

        CuratorFramework curator = fabricService.adapt(CuratorFramework.class);
        try {
            rc.setClientValid(curator != null);
        } catch (Throwable t) {
            rc.setClientValid(false);
        }
        if (rc.isClientValid()) {
            try {
                rc.setClientConnected(curator.getZookeeperClient().isConnected());
                if (!rc.isClientConnected()) {
                    rc.setClientConnectionError(curator.getState().toString());
                }
            } catch (Throwable t) {
                rc.setClientConnected(false);
            }

            if (rc.isClientValid() && rc.isClientConnected()) {

                Container c = fabricService.getCurrentContainer();

                try {
                    rc.setManaged(c.isManaged());
                } catch (Throwable t) {

                }
                try {
                    rc.setProvisionComplete(c.isProvisioningComplete());
                } catch (Throwable t) {

                }
            }

        }

        return rc;
    }

    @Override
    public Map<String, Object> fabricServiceStatus() {
        ServiceStatusDTO dto = getFabricServiceStatus();

        Map<String, Object> answer = new TreeMap<String, Object>();
        answer.put("clientValid", dto.isClientValid());
        answer.put("clientConnected", dto.isClientConnected());
        answer.put("clientConnectionError", dto.getClientConnectionError());
        answer.put("provisionComplete", dto.isProvisionComplete());
        answer.put("managed", dto.isManaged());

        return answer;
    }

    @Override
    public Map<String, String> createContainers(Map<String, Object> options) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating containers from JSON data: " + options);
        }

        String providerType = (String) options.get("providerType");

        if (providerType == null) {
            throw new RuntimeException("No providerType provided");
        }

        CreateContainerBasicOptions.Builder builder = null;

        ContainerProvider provider = fabricService.getValidProviders().get(providerType);
        if (provider == null) {
            throw new RuntimeException("Can't find valid provider of type: " + providerType);
        }

        Class clazz = provider.getOptionsType();
        try {
            builder = (CreateContainerBasicOptions.Builder) clazz.getMethod("builder").invoke(null);
        } catch (Exception e) {
            LOG.warn("Failed to find builder type", e);
        }

        if (builder == null) {
            throw new RuntimeException("Unknown provider type : " + providerType);
        }

        ObjectMapper mapper = getObjectMapper();

        builder = mapper.convertValue(options, builder.getClass());

        builder.zookeeperPassword(fabricService.getZookeeperPassword());
        builder.zookeeperUrl(fabricService.getZookeeperUrl());

        Object profileObject = options.get("profiles");

        if (profileObject != null) {
            List profiles = mapper.convertValue(profileObject, List.class);
            builder.profiles(profiles);
        }

        CreateContainerOptions build = builder.build();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Created container options: " + build + " with profiles " + build.getProfiles());
        }

        CreateContainerMetadata<?> metadatas[] = fabricService.createContainers(build);

        Map<String, String> rc = new LinkedHashMap<String, String>();

        for (CreateContainerMetadata<?> metadata : metadatas) {
            if (!metadata.isSuccess()) {
                LOG.error("Failed to create container {}: ", metadata.getContainerName(), metadata.getFailure());
                rc.put(metadata.getContainerName(), metadata.getFailure().getMessage());
            }
        }

        return rc;
    }

    private ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        return mapper;
    }

    @Override
    public void importProfiles(String versionId, List<String> profileZipUrls) {
        fabricService.adapt(ProfileRegistry.class).importProfiles(versionId, profileZipUrls);
    }

    @Override
    @Deprecated // Creates a profile with empty content. Is this meaningful? 
    public Map<String, Object> createProfile(String versionId, String profileId) {
        ProfileBuilder builder = ProfileBuilder.Factory.create(versionId, profileId);
        Profile profile = profileService.createProfile(builder.getProfile());
        return getProfile(versionId, profile.getId());
    }

    @Override
    public Map<String, Object> createProfile(String versionId, String profileId, List<String> parents) {
        ProfileBuilder builder = ProfileBuilder.Factory.create(versionId, profileId).addParents(parents);
        Profile profile = profileService.createProfile(builder.getProfile());
        return getProfile(versionId, profile.getId());
    }

    @Override
    public Map<String, Object> changeProfileParents(String versionId, String profileId, List<String> parents) {
        Profile profile = profileService.getRequiredProfile(versionId, profileId);
        ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile).setParents(parents);
        profile = profileService.updateProfile(builder.getProfile());
        return getProfile(versionId, profile.getId());
    }

    @Override
    public String profileWebAppURL(String webAppId, String profileId, String versionId) {
        return fabricService.profileWebAppURL(webAppId, profileId, versionId);
    }

    @Override
    public String containerWebAppURL(String webAppId, String name) {
        return fabricService.containerWebAppURL(webAppId, name);
    }

    @Override
    public Map<String, Object> createVersion(String sourceId, String targetId) {
        Version version = profileService.createVersionFrom(sourceId, targetId, null);
        return BeanUtils.convertVersionToMap(fabricService, version, BeanUtils.getFields(Version.class));
    }

    @Override
    public Map<String, Object> createVersion(String version) {
        return createVersion(getLatestVersion().getId(), version);
    }

    @Override
    public Map<String, Object> createVersion() {
        Version latestVersion = getLatestVersion();
        VersionSequence sequence = new VersionSequence(latestVersion.getId());
        return createVersion(sequence.next().getName());
    }

    private Version getLatestVersion() {
        List<String> versions = profileService.getVersions();
        IllegalStateAssertion.assertFalse(versions.isEmpty(), "No versions available");
        String latestId = versions.get(versions.size() - 1);
        return profileService.getRequiredVersion(latestId);
    }

    @Override
    public void deleteProfile(String versionId, String profileId) {
        deleteProfile(versionId, profileId, true);
    }

    @Override
    public void deleteProfile(String versionId, String profileId, boolean force) {
        profileService.deleteProfile(fabricService, versionId, profileId, force);
    }

    @Override
    public void deleteVersion(String versionId) {
        profileService.deleteVersion(versionId);
    }

    @Override
    public void destroyContainer(String containerId) {
        fabricService.destroyContainer(containerId);
    }

    @Override
    public Map<String, Object> getContainer(String name) {
        return getContainer(name, BeanUtils.getFields(Container.class));
    }

    @Override
    public Map<String, Object> getContainer(String name, List<String> fields) {
        Container c = fabricService.getContainer(name);
        return BeanUtils.convertContainerToMap(fabricService, c, fields);
    }

    @Override
    /**
     * Returns configured jvmOpts only for (local and remote) Karaf containers.
     */
    public String getJvmOpts(String containerName) {
        String result = "";

        Container container = fabricService.getContainer(containerName);
        CreateContainerBasicMetadata metadata = (CreateContainerBasicMetadata) container.getMetadata();
        if(metadata == null){
            return "Inapplicable";
        }

        switch( metadata.getCreateOptions().getProviderType()){
            case "child":
            case "ssh":
                CreateContainerOptions createOptions = metadata.getCreateOptions();
                result = createOptions.getJvmOpts();
                break;
            default:
                result = "Inapplicable";
        }

        return result;
    }

    @Override
    public void setJvmOpts(String containerName, String jvmOpts) {
        Container container = fabricService.getContainer(containerName);
        changeCreateOptionsField(container.getId(), "jvmOpts", jvmOpts);
    }

    @Override
    public List<String> getContainerProvisionList(String name) {
        Container container = fabricService.getContainer(name);
        if (container != null) {
            return new ArrayList<String>();
        }
        throw new IllegalStateException(String.format("Container %s not found.", name));
    }

    @Override
    public void applyVersionToContainers(String version, List<String> containers) {
        Version v = profileService.getVersion(version);
        for (String container : containers) {
            fabricService.getContainer(container).setVersion(v);
        }
    }

    @Override
    public void applyProfilesToContainers(String version, List<String> profiles, List<String> containers) {
        Profile[] p = stringsToProfiles(version, profiles);
        for (String container : containers) {
            fabricService.getContainer(container).setProfiles(p);
        }
    }

    @Override
    public void addProfilesToContainer(String container, List<String> profiles) {
        Container cont = fabricService.getContainer(container);
        cont.addProfiles(stringsToProfiles(cont.getVersion(), profiles));
    }

    @Override
    public void removeProfilesFromContainer(String container, List<String> profileIds) {
        Container cont = fabricService.getContainer(container);
        cont.removeProfiles(profileIds.toArray(new String[profileIds.size()]));
    }

    @Override
    public List<Map<String, Object>> containers() {
        return containers(BeanUtils.getFields(Container.class));
    }

    @Override
    public List<Map<String, Object>> containers(List<String> fields) {
        List<Map<String, Object>> answer = new ArrayList<Map<String, Object>>();
        for (Container c : fabricService.getContainers()) {
            answer.add(BeanUtils.convertContainerToMap(fabricService, c, fields));
        }
        return answer;
    }

    @Override
    public List<Map<String, Object>> containers(List<String> fields, List<String> profileFields) {
        List<Map<String, Object>> answer = new ArrayList<Map<String, Object>>();
        for (Container c : fabricService.getContainers()) {
            Map<String, Object> map = BeanUtils.convertContainerToMap(fabricService, c, fields);
            List<Map<String, Object>> profiles = new ArrayList<Map<String, Object>>();
            for (Profile p : c.getProfiles()) {
                profiles.add(BeanUtils.convertProfileToMap(fabricService, p, profileFields));
            }
            map.put("profiles", profiles);
            answer.add(map);
        }
        return answer;
    }

    private CreateContainerMetadata<?> getContainerMetaData(String id) {
        Container container = fabricService.getContainer(id);
        return container.getMetadata();
    }

    @Override
    public String containerMetadataType(String id) {
        CreateContainerMetadata<?> metadata = getContainerMetaData(id);
        if (metadata == null) {
            return null;
        } else {
            return metadata.getClass().getName();
        }
    }

    @Override
    public Map<String, String> getProfileProperties(String versionId, String profileId, String pid) {
        Map<String, String> answer = null;
        Version version = profileService.getVersion(versionId);
        if (version != null) {
            Profile profile = version.getRequiredProfile(profileId);
            if (profile != null) {
                answer = profile.getConfiguration(pid);
            }
        }
        return answer;
    }

    @Override
    public Map<String, String> getOverlayProfileProperties(String versionId, String profileId, String pid) {
        Map<String, String> answer = null;
        Version version = profileService.getVersion(versionId);
        if (version != null) {
            Profile profile = version.getRequiredProfile(profileId);
            if (profile != null) {
                Profile overlayProfile = profileService.getOverlayProfile(profile);
                answer = overlayProfile.getConfiguration(pid);
            }
        }
        return answer;
    }

    @Override
    public boolean setProfileProperties(String versionId, String profileId, String pid, Map<String, String> properties) {
        boolean answer = false;
        Version version = profileService.getVersion(versionId);
        if (version != null) {
            Profile profile = profileService.getRequiredProfile(versionId, profileId);
            ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile);
            builder.addConfiguration(pid, properties);
            profileService.updateProfile(builder.getProfile());
            answer = true;
        }
        return answer;
    }

    @Override
    public String getProfileProperty(String versionId, String profileId, String pid, String propertyName) {
        String answer = null;
        Map<String, String> properties = getProfileProperties(versionId, profileId, pid);
        if (properties != null) {
            answer = properties.get(propertyName);
        }
        return answer;
    }

    @Override
    public String setProfileProperty(String versionId, String profileId, String pid, String propertyName, String value) {
        Map<String, String> properties = getProfileProperties(versionId, profileId, pid);
        if (properties == null) {
            properties = new HashMap<String, String>();
        }
        String answer = properties.put(propertyName, value);
        setProfileProperties(versionId, profileId, pid, properties);
        return answer;
    }

    @Override
    public void setProfileAttribute(String versionId, String profileId, String attributeId, String value) {
        Profile profile = profileService.getRequiredProfile(versionId, profileId);
        ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile);
        builder.addAttribute(attributeId, value);
        profileService.updateProfile(builder.getProfile());
    }

    @Override
    public void setProfileSystemProperties(String versionId, String profileId, Map<String, String> systemProperties) {
        Version version = profileService.getVersion(versionId);
        Profile profile = version.getRequiredProfile(profileId);
        Map<String, String> profileProperties = getProfileProperties(versionId, profileId, Constants.AGENT_PID);
        if (profileProperties == null) {
            // is it necessary?
            profileProperties = new HashMap<String, String>();
        }
        // remove existing
        for (Iterator<Map.Entry<String, String>> iterator = profileProperties.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, String> entry = iterator.next();
            if (entry.getKey().startsWith("system.")) {
                iterator.remove();
            }
        }
        // add new
        for (String k : systemProperties.keySet()) {
            profileProperties.put("system." + k, systemProperties.get(k));
        }

        setProfileProperties(versionId, profileId, Constants.AGENT_PID, profileProperties);
    }

    @Override
    public String containerCreateOptionsType(String id) {
        CreateContainerMetadata<?> metadata = getContainerMetaData(id);
        if (metadata == null) {
            return null;
        }
        CreateContainerOptions options = metadata.getCreateOptions();
        if (options == null) {
            return null;
        } else {
            return options.getClass().getName();
        }
    }

    @Override
    public void changeCreateOptionsField(String containerId, String field, Object value) {
        CreateContainerMetadata<? extends CreateContainerOptions> metadata = getContainerMetaData(containerId);
        if (metadata == null) {
            return;
        }
        CreateContainerOptions options = metadata.getCreateOptions();
        if (options == null) {
            return;
        }

        ObjectMapper mapper = getObjectMapper();
        JsonNode optionsJson = mapper.convertValue(options, JsonNode.class);
        JsonNode valueJson = mapper.convertValue(value, JsonNode.class);
        ((ObjectNode) optionsJson).put(field, valueJson);

        Object builder = null;

        try {
            builder = options.getClass().getMethod("builder").invoke(null);
        } catch (Exception e) {
            LOG.warn("Failed to get builder when setting " + field + " on container " + containerId, e);
            throw new RuntimeException("Failed to get builder when setting " + field + " on container " + containerId, e);
        }

        builder = mapper.convertValue(optionsJson, builder.getClass());

        CreateContainerOptions newOptions = null;
        try {
            newOptions = (CreateContainerOptions) builder.getClass().getMethod("build").invoke(builder);
        } catch (Exception e) {
            LOG.warn("Failed to build CreatecontainerOptions when setting " + field + " on container " + containerId, e);
            throw new RuntimeException("Failed to build CreatecontainerOptions when setting " + field + " on container " + containerId, e);
        }
        metadata.setCreateOptions(newOptions);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Create container metadata: " + metadata);
        }
        fabricService.adapt(DataStore.class).setContainerMetadata(metadata);
    }

    @Override
    public String[] containerIds() {
        List<String> answer = new ArrayList<String>();
        for (Container container : fabricService.getContainers()) {
            answer.add(container.getId());
        }
        return answer.toArray(new String[answer.size()]);
    }

    @Override
    public List<String> containerIdsForProfile(String versionId, String profileId) {
        List<String> fields = new ArrayList<String>();
        fields.add("id");
        return BeanUtils.collapseToList(containersForProfile(versionId, profileId, fields), "id");
    }

    @Override
    public List<Map<String, Object>> containersForProfile(String versionId, String profileId) {
        return containersForProfile(versionId, profileId, BeanUtils.getFields(Container.class));
    }

    @Override
    public List<Map<String, Object>> containersForProfile(String versionId, String profileId, List<String> fields) {
        Version version = profileService.getVersion(versionId);
        Profile profile = version != null ? version.getRequiredProfile(profileId) : null;
        List<Map<String, Object>> answer = new ArrayList<Map<String, Object>>();
        if (profile != null) {
            for (Container c : fabricService.getContainers()) {
                for (Profile p : c.getProfiles()) {
                    if (p.equals(profile)) {
                        answer.add(BeanUtils.convertContainerToMap(fabricService, c, fields));
                    }
                }
            }
        }
        return answer;
    }

    @Override
    public List<String> containerIdsForVersion(String versionId) {
        List<String> fields = new ArrayList<String>();
        fields.add("id");
        return BeanUtils.collapseToList(containersForVersion(versionId, fields), "id");
    }

    @Override
    public List<Map<String, Object>> containersForVersion(String versionId) {
        return containersForVersion(versionId, BeanUtils.getFields(Container.class));
    }

    @Override
    public List<Map<String, Object>> containersForVersion(String versionId, List<String> fields) {
        Version version = profileService.getVersion(versionId);
        List<Map<String, Object>> answer = new ArrayList<Map<String, Object>>();
        if (version != null) {
            for (Container c : fabricService.getContainers()) {
                if (c.getVersion().equals(version)) {
                    answer.add(BeanUtils.convertContainerToMap(fabricService, c, fields));
                }
            }
        }
        return answer;
    }

    @Override
    public void setContainerProperty(String containerId, String property, Object value) {
        Container container = fabricService.getContainer(containerId);
        BeanUtils.setValue(container, property, value);
    }

    protected Profile[] stringsToProfiles(String version, List<String> names) {
        return stringsToProfiles(profileService.getVersion(version), names);
    }

    protected Profile[] stringsToProfiles(Version version, List<String> names) {
        List<Profile> allProfiles = version.getProfiles();
        List<Profile> profiles = new ArrayList<Profile>();
        if (names == null) {
            return new Profile[0];
        }
        for (String name : names) {
            Profile profile = null;
            for (Profile p : allProfiles) {
                if (name.equals(p.getId())) {
                    profile = p;
                    break;
                }
            }
            if (profile == null) {
                throw new IllegalArgumentException("Profile " + name + " not found.");
            }
            profiles.add(profile);
        }
        return profiles.toArray(new Profile[profiles.size()]);
    }

    @Override
    public Map<String, Object> currentContainer() {
        return BeanUtils.convertContainerToMap(fabricService, fabricService.getCurrentContainer(), BeanUtils.getFields(Container.class));
    }

    @Override
    public String getCurrentContainerName() {
        return fabricService.getCurrentContainerName();
    }

    @Override
    public int getPublicPortOnCurrentContainer(int localPort) {
        try {
            return PublicPortMapper.getPublicPort(localPort);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDefaultJvmOptions() {
        return fabricService.getDefaultJvmOptions();
    }

    @Override
    public String getDefaultRepo() {
        return fabricService.getDefaultRepo();
    }

    @Override
    public Map<String, Object> defaultVersion() {
        return BeanUtils.convertVersionToMap(fabricService, fabricService.getRequiredDefaultVersion(), BeanUtils.getFields(Version.class));
    }

    @Override
    public String getDefaultVersion() {
        return fabricService.getDefaultVersionId();
    }

    @Override
    public FabricStatusDTO fabricStatus() {
        return new FabricStatusDTO(fabricService.getFabricStatus());
    }

    @Override
    public String fabricStatusAsJson() {
        FabricStatusDTO dto = fabricStatus();

        if (dto != null) {
            try {
                return getObjectMapper()
                        .writerWithDefaultPrettyPrinter()
                        .withType(FabricStatusDTO.class)
                        .writeValueAsString(dto);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error writing data as json", e);
            }
        } else {
            return null;
        }
    }

    @Override
    public String getMavenRepoUploadURI() {
        URI answer = fabricService.getMavenRepoUploadURI();
        return (answer != null) ? answer.toString() : null;
    }

    @Override
    public String getMavenRepoURI() {
        URI answer = fabricService.getMavenRepoURI();
        return (answer != null) ? answer.toString() : null;
    }

    @Override
    public Map<String, Object> getProfileFeatures(String versionId, String profileId) {
        Profile profile = profileService.getVersion(versionId).getRequiredProfile(profileId);
        Profile effectiveProfile = Profiles.getEffectiveProfile(fabricService, profileService.getOverlayProfile(profile));

        Map<String, Boolean> isParentFeature = new HashMap<String, Boolean>();

        for (String feature : profile.getFeatures()) {
            isParentFeature.put(feature, Boolean.FALSE);
        }

        for (String feature : effectiveProfile.getFeatures()) {
            if (isParentFeature.get(feature) == null) {
                isParentFeature.put(feature, Boolean.TRUE);
            }
        }

        Map<String, Object> rc = new HashMap<String, Object>();

        List<Map<String, Object>> featureDefs = new ArrayList<Map<String, Object>>();

        for (Map.Entry<String, Boolean> featureEntry : isParentFeature.entrySet()) {
            Map<String, Object> featureDef = new HashMap<String, Object>();
            featureDef.put("id", featureEntry.getKey());
            featureDef.put("isParentFeature", featureEntry.getValue());
            featureDefs.add(featureDef);
        }

        rc.put("featureDefinitions", featureDefs);

        List<Map<String, Object>> repositoryDefs = new ArrayList<Map<String, Object>>();
        for (String repo : effectiveProfile.getRepositories()) {
            Map<String, Object> repoDef = new HashMap<String, Object>();

            repoDef.put("id", repo);
            Closeable closeable = null;
            try {
                URL url = new URL(repo);
                InputStream os = url.openStream();
                closeable = os;
                InputStream is = new BufferedInputStream(url.openStream());
                closeable = is;
                char[] buffer = new char[8192];
                StringBuilder data = new StringBuilder();

                Reader in = new InputStreamReader(is, "UTF-8");
                closeable = in;
                for (;;) {
                    int stat = in.read(buffer, 0, buffer.length);
                    if (stat < 0) {
                        break;
                    }
                    data.append(buffer, 0, stat);
                }
                repoDef.put("data", data.toString());
            } catch (Throwable t) {
                repoDef.put("error", t.getMessage());
            } finally {
                try {
                    if (closeable != null) {
                        closeable.close();
                    }
                } catch (Throwable t) {
                    // whatevs, I tried
                }
            }

            repositoryDefs.add(repoDef);
        }

        rc.put("repositoryDefinitions", repositoryDefs);

        return rc;
    }

    @Override
    public Map<String, Object> getProfile(String versionId, String profileId) {
        return getProfile(versionId, profileId, true);
    }

    @Override
    public Map<String, Object> getProfile(String versionId, String profileId, boolean mandatory) {
        return doGetProfile(versionId, profileId, BeanUtils.getFields(Profile.class), mandatory);
    }

    public Map<String, Object> getProfile(String versionId, String profileId, List<String> fields) {
        return doGetProfile(versionId, profileId, fields, true);
    }

    Map<String, Object> doGetProfile(String versionId, String profileId, List<String> fields, boolean mandatory) {
        Version version = profileService.getVersion(versionId);

        Profile profile;
        if (mandatory) {
            profile = version.getRequiredProfile(profileId);
        } else {
            profile = version.getProfile(profileId);
        }
        if (profile == null) {
            return null;
        }

        Map<String, Object> answer = BeanUtils.convertProfileToMap(fabricService, profile, fields);
        String iconURLField = "iconURL";
        if (fields.contains(iconURLField) && !profile.isOverlay()) {
            // TODO this could move to Profile.getIconURL() but that would require
            // introducing profileService into ProfileImpl and the ProfileBuilder stuff
            String restApi = restApiUrl();
            if (restApi != null && restApi.length() > 0) {
                // turn REST into relative URI so it works with docker containers etc (avoids local ports etc)
                try {
                    URL url = new URL(restApi);
                    restApi = url.getPath();
                } catch (MalformedURLException e) {
                    // Ignore
                }
                String icon = getIconURL(version, versionId, profile, profileId, restApi);


                answer.put(iconURLField, icon);
            }
        }
        return answer;
    }

    /**
     * Returns the URL relative to the rest api for the icon
     */
    protected String getIconURL(Version version, String versionId, Profile profile, String profileId, String restApi) {
        // lets find the URL of the icon in the parent profiles
        String relativeIcon = profile.getIconRelativePath();
        String iconProfileId = profileId;
        if (isNullOrEmpty(relativeIcon)) {
            List<String> parentIds = profile.getParentIds();
            if (parentIds != null && !parentIds.isEmpty()) {
                for (String parentId : parentIds) {
                    Profile parentProfile = version.getRequiredProfile(parentId);
                    relativeIcon = parentProfile.getIconRelativePath();
                    if (isNullOrEmpty(relativeIcon)) {
                        String answer = getIconURL(version, versionId, parentProfile, parentId, restApi);
                        if (!isNullOrEmpty(answer)) {
                            return answer;
                        }
                    }
                    if (!isNullOrEmpty(relativeIcon)) {
                        iconProfileId = parentId;
                        break;
                    }
                }
            }
        }
        // the path is relative to the profile
        String icon = null;
        if (!isNullOrEmpty(relativeIcon)) {
            icon = restApi + "/version/" + versionId + "/profile/" + iconProfileId + "/overlay/file/" + relativeIcon;
        }
        return icon;
    }

    public static boolean isNullOrEmpty(String text) {
        return text == null || text.isEmpty();
    }

    @Override
    public List<Map<String, Object>> getProfiles(String versionId) {
        return getProfiles(versionId, BeanUtils.getFields(Profile.class));
    }

    @Override
    public List<Map<String, Object>> getProfiles(String versionId, List<String> fields) {
        List<Map<String, Object>> answer = new ArrayList<Map<String, Object>>();

        for (Profile p : profileService.getVersion(versionId).getProfiles()) {
            answer.add(getProfile(versionId, p.getId(), fields));
        }

        return answer;
    }

    @Override
    public List<String> getProfileIds(String version) {
        return Ids.getIds(profileService.getVersion(version).getProfiles());
    }

    @Override
    public String getConfigurationFile(String versionId, String profileId, String fileName) {
        return Base64.encodeBase64String(profileService.getVersion(versionId).getRequiredProfile(profileId).getFileConfigurations().get(fileName));
    }

    @Override
    public List<String> getConfigurationFileNames(String versionId, String profileId) {
        Version version = profileService.getVersion(versionId);
        Profile profile = version.getProfile(profileId);
        if (profile != null) {
            ArrayList<String> fileNames = new ArrayList<>(profile.getConfigurationFileNames());
            return Collections.unmodifiableList(fileNames);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Returns a map of all the current configuration files in the profiles of
     * the current container with the file name as the key and the profile ID as
     * the value
     */
    @Override
    public Map<String, String> currentContainerConfigurationFiles() {
        String containerName = getCurrentContainerName();
        FabricServiceImpl service = fabricService;
        Container container = service.getContainer(containerName);
        if (container != null) {
            Profile[] profiles = container.getProfiles();
            return Profiles.getConfigurationFileNameMap(profiles);
        }
        return new HashMap<String, String>();
    }

    @Override
    public Map<String, Object> getConfigurationFiles(String versionId, List<String> profileIds, String filename) {
        Pattern pattern = Pattern.compile(filename);
        Map<String, Object> answer = new TreeMap<String, Object>();
        Version version = profileService.getVersion(versionId);
        for (String profileId : profileIds) {
            Profile profile = version.getRequiredProfile(profileId);
            if (profile != null) {
                Map<String, String> files = new TreeMap<String, String>();
                Map<String, byte[]> configs = profile.getFileConfigurations();

                for (Map.Entry<String, byte[]> configEntry : configs.entrySet()) {
                    if (pattern.matcher(configEntry.getKey()).matches()) {
                        files.put(configEntry.getKey(), Base64.encodeBase64String(configEntry.getValue()));
                    }
                }
                answer.put(profileId, files);
            }
        }
        return answer;
    }

    @Override
    public void deleteConfigurationFile(String versionId, String profileId, String fileName) {
        Profile profile = profileService.getRequiredProfile(versionId, profileId);
        ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile);
        builder.deleteFileConfiguration(fileName);
        profileService.updateProfile(builder.getProfile());
    }

    @Override
    public void setConfigurationFile(String versionId, String profileId, String fileName, String data) {
        Profile profile = profileService.getRequiredProfile(versionId, profileId);
        ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile);
        builder.addFileConfiguration(fileName, Base64.decodeBase64(data));
        profileService.updateProfile(builder.getProfile());
    }

    @Override
    public void setProfileBundles(String versionId, String profileId, List<String> bundles) {
        Profile profile = profileService.getRequiredProfile(versionId, profileId);
        ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile);
        builder.setBundles(bundles);
        profileService.updateProfile(builder.getProfile());
    }

    @Override
    public void setProfileFeatures(String versionId, String profileId, List<String> features) {
        Profile profile = profileService.getRequiredProfile(versionId, profileId);
        ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile);
        builder.setFeatures(features);
        profileService.updateProfile(builder.getProfile());
    }

    @Override
    public void setProfileRepositories(String versionId, String profileId, List<String> repositories) {
        Profile profile = profileService.getRequiredProfile(versionId, profileId);
        ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile);
        builder.setRepositories(repositories);
        profileService.updateProfile(builder.getProfile());
    }

    @Override
    public void setProfileFabs(String versionId, String profileId, List<String> fabs) {
        Profile profile = profileService.getRequiredProfile(versionId, profileId);
        ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile);
        builder.setFabs(fabs);
        profileService.updateProfile(builder.getProfile());
    }

    @Override
    public void setProfileOverrides(String versionId, String profileId, List<String> overrides) {
        Profile profile = profileService.getRequiredProfile(versionId, profileId);
        ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile);
        builder.setOverrides(overrides);
        profileService.updateProfile(builder.getProfile());
    }

    @Override
    public void setProfileOptionals(String versionId, String profileId, List<String> optionals) {
        Profile profile = profileService.getRequiredProfile(versionId, profileId);
        ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile);
        builder.setOptionals(optionals);
        profileService.updateProfile(builder.getProfile());
    }

    @Override
    public void setProfileTags(String versionId, String profileId, List<String> tags) {
        Profile profile = profileService.getRequiredProfile(versionId, profileId);
        ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile);
        builder.setTags(tags);
        profileService.updateProfile(builder.getProfile());
    }

    /**
     * Scales the given profile up or down in the number of instances required
     *
     *
     * @param profile
     *            the profile ID to change the requirements
     * @param numberOfInstances
     *            the number of instances to increase or decrease
     * @return true if the requirements changed
     */
    @Override
    public boolean scaleProfile(String profile, int numberOfInstances) throws IOException {
        return fabricService.scaleProfile(profile, numberOfInstances);
    }

    @Override
    public FabricRequirements requirements() {
        return fabricService.getRequirements();
    }

    @Override
    public String requirementsAsJson() {
        FabricRequirements dto = requirements();

        try {
            return getObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .withType(FabricRequirements.class)
                    .writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error writing data as json", e);
        }
    }

    @Override
    public AutoScaleStatus autoScaleStatus() {
        return fabricService.getAutoScaleStatus();
    }

    @Override
    public String autoScaleStatusAsJson() {
        AutoScaleStatus dto = autoScaleStatus();

        if (dto != null) {
            try {
                return getObjectMapper()
                        .writerWithDefaultPrettyPrinter()
                        .withType(AutoScaleStatus.class)
                        .writeValueAsString(dto);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error writing data as json", e);
            }
        } else {
            return null;
        }
    }

    @Override
    public List<String> versionIds() {
        return profileService.getVersions();
    }

    @Override
    public Map<String, Object> getVersion(String versionId) {
        return getVersion(versionId, BeanUtils.getFields(Version.class));
    }

    @Override
    public Map<String, Object> getVersion(String versionId, List<String> fields) {
        return BeanUtils.convertVersionToMap(fabricService, profileService.getVersion(versionId), fields);
    }

    @Override
    public List<Map<String, Object>> versions() {
        return versions(BeanUtils.getFields(Version.class));
    }

    @Override
    public List<Map<String, Object>> versions(List<String> fields) {
        List<Map<String, Object>> answer = new ArrayList<Map<String, Object>>();
        for (String versionId : profileService.getVersions()) {
            answer.add(getVersion(versionId, fields));
        }
        return answer;
    }
    
    @Override
    public void copyProfile(String versionId, String sourceId, String targetId, boolean force) {
        Version v = profileService.getVersion(versionId);
        if (v != null) {
            Profiles.copyProfile(fabricService, versionId, sourceId, targetId, force);
        }
    }

    @Override
    public void renameProfile(String versionId, String profileId, String newId, boolean force) {
        Version v = profileService.getVersion(versionId);
        if (v != null) {
            Profiles.renameProfile(fabricService, versionId, profileId, newId, force);
        }
    }

    public void refreshProfile(String versionId, String profileId) {
        Version version = profileService.getVersion(versionId);
        if (version != null) {
            Profile profile = version.getRequiredProfile(profileId);
            if (profile != null) {
                Profiles.refreshProfile(fabricService, profile);
            }
        }
    }

    @Override
    public String getZookeeperInfo(String name) {
        return fabricService.getZookeeperInfo(name);
    }

    @Override
    public String webConsoleUrl() {
        return fabricService.getWebConsoleUrl();
    }

    @Override
    public String gitUrl() {
        return fabricService.getGitUrl();
    }

    @Override
    public String getZookeeperUrl() {
        return fabricService.getZookeeperUrl();
    }

    @Override
    public void registerProvider(ContainerProvider provider, Map<String, Object> properties) {
        fabricService.registerProvider(provider, properties);
    }

    @Override
    public void registerProvider(String scheme, ContainerProvider provider) {
        fabricService.registerProvider(scheme, provider);
    }

    @Override
    public void setDefaultJvmOptions(String jvmOptions) {
        fabricService.setDefaultJvmOptions(jvmOptions);
    }

    @Override
    public void setDefaultRepo(String defaultRepo) {
        fabricService.setDefaultRepo(defaultRepo);
    }

    @Override
    public void setDefaultVersion(String versionId) {
        fabricService.setDefaultVersionId(versionId);
    }

    @Override
    public void requirements(FabricRequirements requirements) throws IOException {
        fabricService.setRequirements(requirements);
    }

    @Override
    public void requirementsJson(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        Object value = mapper.reader(FabricRequirements.class).readValue(json);
        if (value instanceof FabricRequirements) {
            requirements((FabricRequirements) value);
        } else {
            throw new IOException("Failed to parse FabricRequirements from JSON. Got " + value + ". JSON: " + json);
        }
    }

    @Override
    public void startContainer(String containerId) {
        fabricService.startContainer(containerId);
    }

    @Override
    public List<Map<String, Object>> startContainers(List<String> containerIds) {
        List<Map<String, Object>> rc = new ArrayList<Map<String, Object>>();
        for (String containerId : containerIds) {
            Map<String, Object> status = new LinkedHashMap<String, Object>();
            status.put("id", containerId);
            try {
                startContainer(containerId);
                status.put("success", true);
            } catch (Throwable t) {
                status.put("error", t);
                status.put("errorMessage", t.getMessage());
            }
            rc.add(status);
        }
        return rc;
    }

    @Override
    public void stopContainer(String containerId) {
        fabricService.stopContainer(containerId);
    }

    @Override
    public List<Map<String, Object>> stopContainers(List<String> containerIds) {
        List<Map<String, Object>> rc = new ArrayList<Map<String, Object>>();
        for (String containerId : containerIds) {
            Map<String, Object> status = new LinkedHashMap<String, Object>();
            status.put("id", containerId);
            try {
                stopContainer(containerId);
                status.put("success", true);
            } catch (Throwable t) {
                status.put("error", t);
                status.put("errorMessage", t.getMessage());
            }
            rc.add(status);
        }
        return rc;
    }

    @Override
    public Map<String, String> registeredProviders() {
        Map<String, ContainerProvider> providers = fabricService.getProviders();
        return toJsonMap(providers);
    }

    @Override
    public Map<String, String> registeredValidProviders() {
        Map<String, ContainerProvider> providers = fabricService.getValidProviders();
        return toJsonMap(providers);
    }

    private Map<String, String> toJsonMap(Map<String, ContainerProvider> providers) {
        Map<String, String> answer = new TreeMap<String, String>();

        for (Map.Entry<String, ContainerProvider> providerEntry : providers.entrySet()) {
            answer.put(providerEntry.getKey(), providerEntry.getValue().getOptionsType().getName());
        }
        return answer;
    }

    @Override
    public void unregisterProvider(ContainerProvider provider, Map<String, Object> properties) {
        fabricService.unregisterProvider(provider, properties);
    }

    @Override
    public void unregisterProvider(String scheme) {
        fabricService.unregisterProvider(scheme);
    }

    @Override
    public void applyPatches(List<String> files, String sourceId, String targetId, String proxyUser, String proxyPassword) {

        List<File> patchFiles = new ArrayList<File>();

        for (String fileName : files) {
            File file = new File(fileName);
            if (file.exists()) {
                patchFiles.add(file);
            } else {
                LOG.warn("Patch file does not exist, skipping: {}", fileName);
            }
        }

        if (patchFiles.isEmpty()) {
            LOG.warn("No valid patches to apply");
            throw new FabricException("No valid patches to apply");
        }

        if (targetId == null || targetId.equals("")) {
            Version latestVersion = getLatestVersion();
            VersionSequence sequence = new VersionSequence(latestVersion.getId());
            targetId = sequence.next().getName();
        }
        
        Version targetVersion = profileService.createVersionFrom(sourceId, targetId, null);

        File currentPatchFile = null;
        try {
            for (File file : patchFiles) {
                currentPatchFile = file;
                if (!file.isFile()) {
                    LOG.info("File is a directory, skipping: {}", file);
                    continue;
                }
                LOG.info("Applying patch file {}", file);
                fabricService.getPatchService().applyPatch(targetVersion, file.toURI().toURL(), proxyUser, proxyPassword);
                LOG.info("Successfully applied {}", file);
            }
        } catch (Throwable t) {
            LOG.warn("Failed to apply patch file {}", currentPatchFile, t);
            profileService.deleteVersion(targetId);
            throw new FabricException("Failed to apply patch file " + currentPatchFile, t);
        }

        for (File file : patchFiles) {
            try {
                LOG.info("Deleting patch file {}", file);
                boolean deleted = file.delete();
                if (!deleted) {
                    LOG.warn("Failed to delete patch file {}", file);
                }
            } catch (Throwable t) {
                LOG.warn("Failed to delete patch file {} due to {}", file, t);
            }
        }

    }

    @Override
    public String getConfigurationValue(String versionId, String profileId, String pid, String key) {
        return fabricService.getConfigurationValue(versionId, profileId, pid, key);
    }

    @Override
    public void setConfigurationValue(String versionId, String profileId, String pid, String key, String value) {
        fabricService.setConfigurationValue(versionId, profileId, pid, key, value);
    }

    @Override
    public String mavenProxyDownloadUrl() {
        URI uri = fabricService.getMavenRepoURI();
        if (uri != null) {
            return uri.toASCIIString();
        } else {
            return null;
        }
    }

    @Override
    public String mavenProxyUploadUrl() {
        URI uri = fabricService.getMavenRepoUploadURI();
        if (uri != null) {
            return uri.toASCIIString();
        } else {
            return null;
        }
    }

    @Override
    public String restApiUrl() {
        return fabricService.getRestAPI();
    }

    @Override
    public String clusterJson(String clusterPathSegment) throws Exception {
        String prefix = "/fabric/registry/clusters";
        String path;
        if (Strings.isEmpty(clusterPathSegment)) {
            path = prefix;
        } else {
            if (clusterPathSegment.startsWith("/")) {
                path = clusterPathSegment;
            } else {
                path = prefix + "/" + clusterPathSegment;
            }
        }
        Map<String, Object> answer = new HashMap<String, Object>();
        CuratorFramework curator = fabricService.adapt(CuratorFramework.class);
        ObjectMapper mapper = new ObjectMapper();
        addChildrenToMap(answer, path, curator, mapper);
        return mapper.writeValueAsString(answer);
    }

    protected void addChildrenToMap(Map<String, Object> answer, String path, CuratorFramework curator, ObjectMapper mapper) throws Exception {
        Set<String> dontSubstituteKeys = new HashSet<String>(Arrays.asList("id", "container"));

        List<String> children = getChildrenSafe(curator, path);
        for (String child : children) {
            String childPath = path + "/" + child;
            byte[] data = curator.getData().forPath(childPath);
            if (data != null && data.length > 0) {
                String text = new String(data).trim();
                if (!text.isEmpty()) {
                    Map<String, Object> map = mapper.readValue(data, HashMap.class);
                    if (map != null) {
                        Map<String, Object> substitutedMap = new HashMap<String, Object>();
                        Set<Map.Entry<String, Object>> set = map.entrySet();
                        for (Map.Entry<String, Object> entry : set) {
                            String key = entry.getKey();
                            Object value = entry.getValue();
                            if (value != null) {
                                if (value instanceof String && !dontSubstituteKeys.contains(key)) {
                                    value = getSubstitutedData(curator, value.toString());
                                } else if (value instanceof List) {
                                    List list = (List) value;
                                    List<String> substitutedValues = new ArrayList<String>();
                                    value = substitutedValues;
                                    for (Object item : list) {
                                        String serviceText = getSubstitutedData(curator, item.toString());
                                        substitutedValues.add(serviceText);
                                    }
                                }
                                substitutedMap.put(key, value);
                            }
                        }
                        answer.put(child, substitutedMap);
                    }
                }
            } else {
                // recurse into children
                Map<String, Object> map = new HashMap<String, Object>();
                addChildrenToMap(map, childPath, curator, mapper);
                if (!map.isEmpty()) {
                    answer.put(child, map);
                }
            }
        }
    }

    public static List listValue(Map<String, Object> map, String key) {
        Object value = null;
        if (map != null) {
            value = map.get(key);
        }
        if (value instanceof List) {
            return (List) value;
        } else if (value instanceof Object[]) {
            return java.util.Arrays.asList((Object[]) value);
        } else if (value != null) {
            List list = new ArrayList();
            list.add(value);
            return list;
        }
        return null;
    }
}
