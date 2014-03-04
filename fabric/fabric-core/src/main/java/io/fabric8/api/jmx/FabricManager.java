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

import org.apache.commons.codec.binary.Base64;
import org.apache.curator.framework.CuratorFramework;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import io.fabric8.api.*;
import io.fabric8.service.FabricServiceImpl;
import org.fusesource.insight.log.support.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getChildrenSafe;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getSubstitutedData;

/**
 */
public class FabricManager implements FabricManagerMBean {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabricManager.class);

    private final FabricServiceImpl fabricService;
    private ObjectName objectName;

    public FabricManager(FabricServiceImpl fabricService) {
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

    public void registerMBeanServer(MBeanServer mbeanServer) {
        try {
            ObjectName name = getObjectName();
			if (!mbeanServer.isRegistered(name)) {
				mbeanServer.registerMBean(this, name);
			}
		} catch (Exception e) {
            LOG.warn("An error occured during mbean server registration: " + e, e);
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
                LOG.warn("An error occured during mbean server registration: " + e, e);
            }
        }
    }

    protected FabricServiceImpl getFabricService() {
        return fabricService;
    }


    // Management API
    //-------------------------------------------------------------------------
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

        CuratorFramework curator = getFabricService().adapt(CuratorFramework.class);
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
            } catch(Throwable t) {
                rc.setClientConnected(false);
            }

            if (rc.isClientValid() && rc.isClientConnected()) {

                Container c = getFabricService().getCurrentContainer();

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
    public Map<String, String> createContainers(Map<String, Object> options) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating containers from JSON data: " + options);
        }

        String providerType = (String) options.get("providerType");

        if (providerType == null) {
            throw new RuntimeException("No providerType provided");
        }

        CreateContainerBasicOptions.Builder builder = null;

        Class clazz = fabricService.getProviders().get(providerType).getOptionsType();
        try {
            builder = (CreateContainerBasicOptions.Builder)clazz.getMethod("builder").invoke(null);
        } catch (Exception e) {
            LOG.warn("Failed to find builder type", e);
        }

        if (builder == null) {
            throw new RuntimeException("Unknown provider type : " + providerType);
        }

        ObjectMapper mapper = getObjectMapper();

        builder = mapper.convertValue(options, builder.getClass());

        builder.zookeeperPassword(getFabricService().getZookeeperPassword());
        builder.zookeeperUrl(getFabricService().getZookeeperUrl());

        Object profileObject = options.get("profiles");

        if (profileObject != null) {
            List profiles = mapper.convertValue(profileObject, List.class);
            builder.profiles(profiles);
        }

        CreateContainerOptions build = builder.build();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Created container options: " + build + " with profiles " + build.getProfiles());
        }

        CreateContainerMetadata<?> metadatas[] = getFabricService().createContainers(build);

        Map<String, String> rc = new HashMap<String, String>();

        for(CreateContainerMetadata<?> metadata : metadatas) {
            if (!metadata.isSuccess()) {
                LOG.warn("Failed to create container {}: ", metadata.getContainerName(), metadata.getFailure());
                rc.put(metadata.getContainerName(), metadata.getFailure().getMessage());
            }
        }

        return rc;
    }

    private ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        return mapper;
    }

    @Override
    public Map<String, Object> createProfile(String versionId, String name) {
        Profile p = getFabricService().getVersion(versionId).createProfile(name);
        return getProfile(versionId, p.getId());
    }

    @Override
    public Map<String, Object>  createProfile(String versionId, String name, List<String> parents) {
        Profile p = getFabricService().getVersion(versionId).createProfile(name);
        p.setParents(stringsToProfiles(versionId, parents));
        return getProfile(versionId, p.getId());
    }

    @Override
    public Map<String, Object>  changeProfileParents(String versionId, String name, List<String> parents) {
        Profile p = getFabricService().getVersion(versionId).getProfile(name);
        p.setParents(stringsToProfiles(versionId, parents));
        return getProfile(versionId, p.getId());
    }

    @Override
    public String profileWebAppURL(String webAppId, String profileId, String versionId) {
        FabricServiceImpl service = getFabricService();
        if (versionId == null || versionId.length() == 0) {
            Version version = service.getDefaultVersion();
            if (version != null) {
                versionId = version.getId();
            }
        }
        List<String> ids = containerIdsForProfile(versionId, profileId);
        for (String id : ids) {
            String url = containerWebAppURL(webAppId, id);
            if (url != null && url.length() > 0) {
                return url;
            }
        }
        return null;
    }


    @Override
    public String containerWebAppURL(String webAppId, String name) {
        return getFabricService().containerWebAppURL(webAppId, name);
    }


    @Override
    public  Map<String, Object> createVersion(String parentVersionId, String toVersion) {
        Version version = getFabricService().createVersion(parentVersionId, toVersion);
        return BeanUtils.convertVersionToMap(getFabricService(), version, BeanUtils.getFields(Version.class));
    }

    @Override
    public  Map<String, Object> createVersion(String version) {
        return createVersion(getLatestVersion().getId(), version);
    }

    @Override
    public  Map<String, Object> createVersion() {
        return createVersion(getLatestVersion().getSequence().next().getName());
    }

    private Version getLatestVersion() {
        Version[] versions = getFabricService().getVersions();
        Version latest = null;
        int length = versions.length;
        if (length > 0) {
            latest = versions[length - 1];
        } else {
            throw new FabricException("No versions available");
        }
        return latest;
    }

    @Override
    public void deleteProfile(String versionId, String profileId) {
        deleteProfile(versionId, profileId, true);
    }

    @Override
    public void deleteProfile(String versionId, String profileId, boolean force) {
        Version v = getFabricService().getVersion(versionId);
        Profile p = v.getProfile(profileId);
        p.delete(force);
    }

    @Override
    public void deleteVersion(String version) {
        getFabricService().deleteVersion(version);
    }

    @Override
    public void destroyContainer(String containerId) {
        getFabricService().destroyContainer(containerId);
    }

    @Override
    public Map<String, Object> getContainer(String name) {
        return getContainer(name, BeanUtils.getFields(Container.class));
    }

    @Override
    public Map<String, Object> getContainer(String name, List<String> fields) {
        Container c = getFabricService().getContainer(name);
        return BeanUtils.convertContainerToMap(getFabricService(), c, fields);
    }

    @Override
    public List<String> getContainerProvisionList(String name) {
        Container container = getFabricService().getContainer(name);
        if (container != null) {
            return new ArrayList<String>();
        }
        throw new IllegalStateException(String.format("Container %s not found.",name));
    }

    @Override
    public void applyVersionToContainers(String version, List<String> containers) {
        Version v = getFabricService().getVersion(version);
        for(String container : containers) {
            getFabricService().getContainer(container).setVersion(v);
        }
    }

    @Override
    public void applyProfilesToContainers(String version, List<String> profiles, List<String> containers) {
        Profile[] p = stringsToProfiles(version, profiles);
        for (String container: containers) {
            getFabricService().getContainer(container).setProfiles(p);
        }
    }

    @Override
    public void addProfilesToContainer(String container, List<String> profiles) {
        Container cont = getFabricService().getContainer(container);
        cont.addProfiles(stringsToProfiles(cont.getVersion(), profiles));
    }

    @Override
    public void removeProfilesFromContainer(String container, List<String> profiles) {
        Container cont = getFabricService().getContainer(container);
        cont.removeProfiles(stringsToProfiles(cont.getVersion(), profiles));
    }


    @Override
    public List<Map<String, Object>> containers() {
        return containers(BeanUtils.getFields(Container.class));
    }

    @Override
    public List<Map<String, Object>> containers(List<String> fields) {
        List<Map<String, Object>> answer = new ArrayList<Map<String, Object>>();
        for (Container c : getFabricService().getContainers()) {
            answer.add(BeanUtils.convertContainerToMap(getFabricService(), c, fields));
        }
        return answer;
    }

    @Override
    public List<Map<String, Object>> containers(List<String> fields, List<String> profileFields) {
        List<Map<String, Object>> answer = new ArrayList<Map<String, Object>>();
        for (Container c : getFabricService().getContainers()) {
            Map<String, Object> map = BeanUtils.convertContainerToMap(getFabricService(), c, fields);
            List<Map<String, Object>> profiles = new ArrayList<Map<String, Object>>();
            for (Profile p : c.getProfiles()) {
                profiles.add(BeanUtils.convertProfileToMap(getFabricService(), p, profileFields));
            }
            map.put("profiles", profiles);
            answer.add(map);
        }
        return answer;
    }


    private CreateContainerMetadata<?> getContainerMetaData(String id) {
        Container container = getFabricService().getContainer(id);
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
    public Map<String,String> getProfileProperties(String versionId, String profileId, String pid) {
        Map<String, String> answer = null;
        Version version = getFabricService().getVersion(versionId);
        if (version != null) {
            Profile profile = version.getProfile(profileId);
            if (profile != null) {
                answer = profile.getConfiguration(pid);
            }
        }
        return answer;
    }

    @Override
    public boolean setProfileProperties(String versionId, String profileId, String pid, Map<String, String> properties) {
        boolean answer = false;
        Version version = getFabricService().getVersion(versionId);
        if (version != null) {
            Profile profile = version.getProfile(profileId);
            if (profile != null) {
                profile.setConfiguration(pid, properties);
                answer = true;
            }
        }
        return answer;
    }

    @Override
    public String getProfileProperty(String versionId, String profileId, String pid, String propertyName) {
        String answer = null;
        Map<String, String> properties = getProfileProperties(versionId, profileId, pid);
        if (properties != null){
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
        Version version = getFabricService().getVersion(versionId);
        Profile profile = version.getProfile(profileId);
        profile.setAttribute(attributeId, value);
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
        ((ObjectNode)optionsJson).put(field, valueJson);

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
        getFabricService().getDataStore().setContainerMetadata(metadata);
    }

    @Override
    public String[] containerIds() {
      List<String> answer = new ArrayList<String>();
      for (Container container : getFabricService().getContainers()) {
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
        Version version = getFabricService().getVersion(versionId);
        Profile profile = version != null ? version.getProfile(profileId) : null;
        List<Map<String, Object>> answer = new ArrayList<Map<String, Object>>();
        if (profile != null) {
            for (Container c : getFabricService().getContainers()) {
                for (Profile p : c.getProfiles()) {
                    if (p.equals(profile)) {
                        answer.add(BeanUtils.convertContainerToMap(getFabricService(), c, fields));
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
        Version version = getFabricService().getVersion(versionId);
        List<Map<String, Object>> answer = new ArrayList<Map<String, Object>>();
        if (version != null) {
            for (Container c : getFabricService().getContainers()) {
                if (c.getVersion().equals(version)) {
                    answer.add(BeanUtils.convertContainerToMap(getFabricService(), c, fields));
                }
            }
        }
        return answer;
    }

    @Override
    public void setContainerProperty(String containerId, String property, Object value) {
        Container container = getFabricService().getContainer(containerId);
        BeanUtils.setValue(container, property, value);
    }

    protected Profile[] stringsToProfiles(String version, List<String> names) {
        return stringsToProfiles(getFabricService().getVersion(version), names);
    }

    protected Profile[] stringsToProfiles(Version version, List<String> names) {
        Profile[] allProfiles = version.getProfiles();
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



/*
    @Override
    public ContainerTemplate getContainerTemplate(Container container, String jmxUser, String jmxPassword) {
        return getFabricService().getContainerTemplate(container, jmxUser, jmxPassword);
    }

*/

    @Override
    public Map<String, Object> currentContainer() {
        return BeanUtils.convertContainerToMap(getFabricService(), getFabricService().getCurrentContainer(), BeanUtils.getFields(Container.class));
    }


    @Override
    public String getCurrentContainerName() {
        return getFabricService().getCurrentContainerName();
    }


    @Override
    public String getDefaultJvmOptions() {
        return getFabricService().getDefaultJvmOptions();
    }

    @Override
    public String getDefaultRepo() {
        return getFabricService().getDefaultRepo();
    }


    @Override
    public Map<String, Object> defaultVersion() {
        return BeanUtils.convertVersionToMap(getFabricService(), getFabricService().getDefaultVersion(), BeanUtils.getFields(Version.class));
    }

    @Override
    public String getDefaultVersion() {
        return getFabricService().getDefaultVersion().getId();
    }

    @Override
    public FabricStatusDTO fabricStatus() {
        return new FabricStatusDTO(getFabricService().getFabricStatus());
    }


    @Override
    public String getMavenRepoUploadURI() {
        URI answer = getFabricService().getMavenRepoUploadURI();
        return (answer != null) ? answer.toString() : null;
    }


    @Override
    public String getMavenRepoURI() {
        URI answer = getFabricService().getMavenRepoURI();
        return (answer != null) ? answer.toString() : null;
    }

/*

    public PatchService patchService() {
        return getFabricService().getPatchService();
    }
*/

    @Override
    public Map<String, Object> getProfileFeatures(String versionId, String profileId) {
        Profile profile = getFabricService().getVersion(versionId).getProfile(profileId);
        Profile overlay = profile.getOverlay(true);

        Map<String, Boolean> isParentFeature = new HashMap<String, Boolean>();

        for (String feature : profile.getFeatures()) {
            isParentFeature.put(feature, Boolean.FALSE);
        }

        for (String feature : overlay.getFeatures()) {
            if (isParentFeature.get(feature) == null) {
                isParentFeature.put(feature, Boolean.TRUE);
            }
        }

        Map<String, Object> rc = new HashMap<String, Object>();

        List<Map<String, Object>> featureDefs = new ArrayList<Map<String, Object>>();

        for (String feature : isParentFeature.keySet()) {
            Map<String, Object> featureDef = new HashMap<String, Object>();
            featureDef.put("id", feature);
            featureDef.put("isParentFeature", isParentFeature.get(feature));
            featureDefs.add(featureDef);
        }

        rc.put("featureDefinitions", featureDefs);

        List<Map<String, Object>> repositoryDefs = new ArrayList<Map<String, Object>>();
        for (String repo : overlay.getRepositories()) {
            Map<String, Object> repoDef = new HashMap<String, Object>();

            repoDef.put("id", repo);
            InputStream is = null;
            try {
                URL url = new URL(repo);
                is = new BufferedInputStream(url.openStream());
                char[] buffer = new char[8192];
                StringBuilder data = new StringBuilder();

                Reader in = new InputStreamReader(is, "UTF-8");
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
                    if (is != null) {
                        is.close();
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
        return getProfile(versionId, profileId, BeanUtils.getFields(Profile.class));
    }

    public Map<String, Object> getProfile(String versionId, String profileId, List<String> fields) {
        Profile profile = getFabricService().getVersion(versionId).getProfile(profileId);
        return BeanUtils.convertProfileToMap(getFabricService(), profile, fields);
    }

    @Override
    public List<Map<String, Object>> getProfiles(String versionId) {
        return getProfiles(versionId, BeanUtils.getFields(Profile.class));
    }

    @Override
    public List<Map<String, Object>> getProfiles(String versionId, List<String> fields) {
        List<Map<String, Object>> answer = new ArrayList<Map<String, Object>>();

        for (Profile p : getFabricService().getVersion(versionId).getProfiles()) {
            answer.add(getProfile(versionId, p.getId(), fields));
        }

        return answer;
    }

    @Override
    @Deprecated
    public List<String> getProfileIds(String version) {
        return Ids.getIds(getFabricService().getVersion(version).getProfiles());
    }

    @Override
    public String getConfigurationFile(String versionId, String profileId, String fileName) {
        return Base64.encodeBase64String(getFabricService().getVersion(versionId).getProfile(profileId).getFileConfigurations().get(fileName));
    }

    @Override
    public List<String> getConfigurationFileNames(String versionId, String profileId) {
        Version version = getFabricService().getVersion(versionId);
        Profile profile = version.getProfile(profileId);
        if (profile != null) {
            return profile.getConfigurationFileNames();
        } else {
            return new ArrayList<String>();
        }
    }

    /**
     * Returns a map of all the current configuration files in the profiles of the current container with the file name as the key and the profile ID as the value
     */
    @Override
    public Map<String, String> currentContainerConfigurationFiles() {
        String containerName = getCurrentContainerName();
        FabricServiceImpl service = getFabricService();
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
        Map<String, Object> answer = new HashMap<String, Object>();
        Version version = getFabricService().getVersion(versionId);
        for (String profileId : profileIds) {
            Profile profile = version.getProfile(profileId);
            if (profile != null) {
                Map<String, String> files = new HashMap<String, String>();
                Map<String, byte[]> configs = profile.getFileConfigurations();

                for (String key : configs.keySet()) {
                    if (pattern.matcher(key).matches()) {
                        files.put(key, Base64.encodeBase64String(configs.get(key)));
                    }
                }
                answer.put(profileId, files);
            }
        }
        return answer;
    }

    @Override
    public void deleteConfigurationFile(String versionId, String profileId, String fileName) {
        Profile profile = getFabricService().getVersion(versionId).getProfile(profileId);
        Map<String, byte[]> configs = profile.getFileConfigurations();
        configs.remove(fileName);
        profile.setFileConfigurations(configs);
    }

    @Override
    public void setConfigurationFile(String versionId, String profileId, String fileName, String data) {
        Profile profile = getFabricService().getVersion(versionId).getProfile(profileId);
        Map<String, byte[]> configs = profile.getFileConfigurations();
        try {
            configs.put(fileName, Base64.decodeBase64(data));
            profile.setFileConfigurations(configs);
        } catch (Exception e) {
            throw new FabricException("Error setting config file: ", e);
        }
    }

    @Override
    public void setProfileBundles(String versionId, String profileId, List<String> bundles) {
        Version v = getFabricService().getVersion(versionId);
        Profile profile = v.getProfile(profileId);
        profile.setBundles(bundles);
    }

    @Override
    public void setProfileFeatures(String versionId, String profileId, List<String> features) {
        Version v = getFabricService().getVersion(versionId);
        Profile profile = v.getProfile(profileId);
        profile.setFeatures(features);
    }

    @Override
    public void setProfileRepositories(String versionId, String profileId, List<String> repositories) {
        Version v = getFabricService().getVersion(versionId);
        Profile profile = v.getProfile(profileId);
        profile.setRepositories(repositories);
    }

    @Override
    public void setProfileFabs(String versionId, String profileId, List<String> fabs) {
        Version v = getFabricService().getVersion(versionId);
        Profile profile = v.getProfile(profileId);
        profile.setFabs(fabs);
    }

    @Override
    public void setProfileOverrides(String versionId, String profileId, List<String> overrides) {
        Version v = getFabricService().getVersion(versionId);
        Profile profile = v.getProfile(profileId);
        profile.setOverrides(overrides);
    }


/*
    @Override
    public ContainerProvider getProvider(Container container) {
        return getFabricService().getProvider(container);
    }

    @Override
    public ContainerProvider getProvider(String scheme) {
        return getFabricService().getProvider(scheme);
    }

    @Override
    public Map<String, ContainerProvider> providers() {
        return getFabricService().getProviders();
    }
*/

    /**
     * Scales the given profile up or down in the number of instances required
     *
     *
     * @param profile the profile ID to change the requirements
     * @param numberOfInstances the number of instances to increase or decrease
     * @return true if the requiremetns changed
     */
    @Override
    public boolean scaleProfile(String profile, int numberOfInstances) throws IOException {
        return getFabricService().scaleProfile(profile, numberOfInstances);
    }

    @Override
    public FabricRequirements requirements() {
        return getFabricService().getRequirements();
    }

    @Override
    public Map<String, Object>  getVersion(String versionId) {
        return getVersion(versionId, BeanUtils.getFields(Version.class));
    }

    @Override
    public Map<String, Object> getVersion(String versionId, List<String> fields) {
        return BeanUtils.convertVersionToMap(getFabricService(), getFabricService().getVersion(versionId),
                fields);
    }

    @Override
    public List<Map<String, Object>> versions() {
        return versions(BeanUtils.getFields(Version.class));
    }

    @Override
    public List<Map<String, Object>> versions(List<String> fields) {
        List<Map<String, Object>> answer = new ArrayList<Map<String, Object>>();

        for (Version v : getFabricService().getVersions()) {
            answer.add(getVersion(v.getId(), fields));
        }

        return answer;

    }

    @Override
    public void copyProfile(String versionId, String sourceId, String targetId, boolean force) {
        Version v = getFabricService().getVersion(versionId);
        if (v != null) {
            v.copyProfile(sourceId, targetId, force);
        }
    }

    @Override
    public void renameProfile(String versionId, String profileId, String newId, boolean force) {
        Version v = getFabricService().getVersion(versionId);
        if (v != null) {
            v.renameProfile(profileId, newId, force);
        }
    }

    public void refreshProfile(String versionId, String profileId) {
        Version version = getFabricService().getVersion(versionId);
        if (version != null) {
            Profile profile = version.getProfile(profileId);
            if (profile != null) {
                profile.refresh();
            }
        }
    }


    @Override
    public String getZookeeperInfo(String name) {
        return getFabricService().getZookeeperInfo(name);
    }

/*
    public String getZookeeperPassword() {
        return getFabricService().getZookeeperPassword();
    }
*/

    @Override
    public String getZookeeperUrl() {
        return getFabricService().getZookeeperUrl();
    }

    @Override
    public void registerProvider(ContainerProvider provider, Map<String, Object> properties) {
        getFabricService().registerProvider(provider, properties);
    }

    @Override
    public void registerProvider(String scheme, ContainerProvider provider) {
        getFabricService().registerProvider(scheme, provider);
    }


    @Override
    public void setDefaultJvmOptions(String jvmOptions) {
        getFabricService().setDefaultJvmOptions(jvmOptions);
    }

    @Override
    public void setDefaultRepo(String defaultRepo) {
        getFabricService().setDefaultRepo(defaultRepo);
    }


    @Override
    public void setDefaultVersion(String versionId) {
        getFabricService().setDefaultVersion(versionId);
    }


    @Override
    public void requirements(FabricRequirements requirements) throws IOException {
        getFabricService().setRequirements(requirements);
    }

    @Override
    public void requirementsJson(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        Object value = mapper.reader(FabricRequirements.class).readValue(json);
        if (value instanceof FabricRequirements) {
            requirements((FabricRequirements) value);
        } else {
            throw new IOException("Failed to parse FabricRequirements from JSON. Got " + value + ". JSON: " + json);
        }
    }

    @Override
    public void startContainer(String containerId) {
        getFabricService().startContainer(containerId);
    }


    @Override
    public List<Map<String, Object>> startContainers(List<String> containerIds) {
        List<Map<String, Object>> rc = new ArrayList<Map<String, Object>>();
        for (String containerId : containerIds) {
            Map<String, Object> status = new HashMap<String, Object>();
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
        getFabricService().stopContainer(containerId);
    }

    @Override
    public List<Map<String, Object>> stopContainers(List<String> containerIds) {
        List<Map<String, Object>> rc = new ArrayList<Map<String, Object>>();
        for (String containerId : containerIds) {
            Map<String, Object> status = new HashMap<String, Object>();
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
        Map<String, ContainerProvider> providers = getFabricService().getProviders();

        Map<String, String> answer = new HashMap<String, String>();

        for (String name : providers.keySet()) {
            answer.put(name, providers.get(name).getOptionsType().getName());
        }
        return answer;
    }


    @Override
    public void unregisterProvider(ContainerProvider provider, Map<String, Object> properties) {
        getFabricService().unregisterProvider(provider, properties);
    }

    @Override
    public void unregisterProvider(String scheme) {
        getFabricService().unregisterProvider(scheme);
    }

    @Override
    public void applyPatches(List<String> files, String targetVersionId, String newVersionId, String proxyUser, String proxyPassword) {

        List<File> patchFiles = new ArrayList<File>();

        for (String fileName : files) {
            File file = new File(fileName);
            if (file.exists()) {
                patchFiles.add(file);
            } else {
                LOG.warn("Patch file does not exist, skipping: {}", fileName);
            }
        }

        if (patchFiles.size() == 0) {
            LOG.warn("No valid patches to apply");
            throw new FabricException("No valid patches to apply");
        }

        Version version = getFabricService().getVersion(targetVersionId);
        if (version == null) {
            throw new FabricException("Version " + targetVersionId + " not found");
        }

        if (newVersionId == null || newVersionId.equals("")) {
            newVersionId = getLatestVersion().getSequence().next().getName();
        }

        Version targetVersion = getFabricService().createVersion(version, newVersionId);

        File currentPatchFile = null;

        try {
            for (File file : patchFiles) {
                currentPatchFile = file;
                if (!file.isFile()) {
                    LOG.info("File is a directory, skipping: {}", file);
                    continue;
                }
                LOG.info("Applying patch file {}", file);
                getFabricService().getPatchService().applyPatch(targetVersion, file.toURI().toURL(), proxyUser, proxyPassword);
                LOG.info("Successfully applied {}", file);
            }
        } catch (Throwable t) {
            LOG.warn("Failed to apply patch file {}", currentPatchFile, t);
            targetVersion.delete();
            throw new FabricException("Failed to apply patch file " + currentPatchFile, t);
        }

        for (File file : patchFiles) {
            try {
                LOG.info("Deleting patch file {}", file);
                file.delete();
            } catch (Throwable t) {
                LOG.warn("Failed to delete patch file {} due to {}", file, t);
            }
        }

    }

    @Override
    public String getConfigurationValue(String versionId, String profileId, String pid, String key) {
        return getFabricService().getConfigurationValue(versionId, profileId, pid, key);
    }

    @Override
    public void setConfigurationValue(String versionId, String profileId, String pid, String key, String value) {
        getFabricService().setConfigurationValue(versionId, profileId, pid, key, value);
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
            }else {
                path = prefix + "/" + clusterPathSegment;
            }
        }
        Map<String,Object> answer = new HashMap<String, Object>();
        CuratorFramework curator = getFabricService().adapt(CuratorFramework.class);
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
                if (map.size() > 0) {
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
