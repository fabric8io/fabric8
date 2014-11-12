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
package io.fabric8.internal;

import io.fabric8.api.Constants;
import io.fabric8.api.Container;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.DataStore;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricService;
import io.fabric8.api.ModuleStatus;
import io.fabric8.api.OptionsProvider;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Profiles;
import io.fabric8.api.Version;
import io.fabric8.api.ZkDefs;
import io.fabric8.api.data.BundleInfo;
import io.fabric8.api.data.ServiceInfo;
import io.fabric8.common.util.Strings;
import io.fabric8.service.ContainerTemplate;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import io.fabric8.api.gravia.IllegalArgumentAssertion;
import org.osgi.jmx.framework.BundleStateMBean;
import org.osgi.jmx.framework.ServiceStateMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContainerImpl implements Container {

    private static final String ENSEMBLE_PROFILE_PATTERN = "fabric-ensemble-[0-9]*-[0-9]*";

    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    private final Container parent;
    private final String id;
    private final FabricService fabricService;
    private final DataStore dataStore;

    private CreateContainerMetadata<?> metadata;

    public ContainerImpl(Container parent, String id, FabricService fabricService) {
        this.parent = parent;
        this.id = id;
        this.fabricService = fabricService;
        this.dataStore = fabricService.adapt(DataStore.class);
    }

    public FabricService getFabricService() {
		return fabricService;
	}

	public Container getParent() {
        return parent;
    }

    public String getId() {
        return id;
    }

    public boolean isAlive() {
        return dataStore.isContainerAlive(id);
    }

    public void setAlive(boolean flag) {
       dataStore.setContainerAlive(id, flag);
    }

    public boolean isRoot() {
        return parent == null;
    }

    @Override
    public boolean isEnsembleServer() {
        try {
            List<String> containers = dataStore.getEnsembleContainers();
            for (String container : containers) {
                if (id.equals(container)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public boolean isProvisioningComplete() {
        String result = getProvisionResult();
        return PROVISION_SUCCESS.equals(result) ||  PROVISION_STOPPED.equals(result) || PROVISION_ERROR.equals(result);
    }

    @Override
    public boolean isProvisioningPending() {
        return isManaged() && !isProvisioningComplete();
    }

    @Override
    public String getProvisionStatus() {
        String provisioned = getProvisionResult();
        String provisionException = getProvisionException();
        String result = "not provisioned";

        if (provisioned != null) {
            result = provisioned;
            if (result.equals(PROVISION_ERROR) && provisionException != null) {
                result += " - " + provisionException.split(System.getProperty("line.separator"))[0];
            }
        }
        return result;
    }

    public String getSshUrl() {
        return getMandatorySubstitutedAttribute(DataStore.ContainerAttribute.SshUrl);
    }

    public String getJmxUrl() {
        return getMandatorySubstitutedAttribute(DataStore.ContainerAttribute.JmxUrl);
    }

    public String getJolokiaUrl() {
        return getMandatorySubstitutedAttribute(DataStore.ContainerAttribute.JolokiaUrl);
    }

    public void setJolokiaUrl(String location) {
        setAttribute(DataStore.ContainerAttribute.JolokiaUrl, location);
    }

    @Override
    public String getDebugPort() {
        return getOptionalAttribute(DataStore.ContainerAttribute.DebugPort, null);
    }

    @Override
    public String getHttpUrl() {
        return getMandatorySubstitutedAttribute(DataStore.ContainerAttribute.HttpUrl);
    }

    @Override
    public void setHttpUrl(String location) {
        setAttribute(DataStore.ContainerAttribute.HttpUrl, location);
    }

    @Override
    public boolean isManaged() {
        Map<String, String> agentConfig = getOverlayProfile().getConfiguration(Constants.AGENT_PID);
        if (agentConfig != null) {
            return !Boolean.parseBoolean(agentConfig.get("disabled"));
        }
        //if for any reason the profiles are not available yet, then assume the container is managed.
        return true;
    }

    @Override
    public String getVersionId() {
        return dataStore.getContainerVersion(id);
    }

    @Override
    public void setVersionId(String versionId) {
        String currentId = getVersionId();
        if (versionId.compareTo(currentId) != 0) {
            ProfileService profileService = fabricService.adapt(ProfileService.class);
            Version version = profileService.getRequiredVersion(versionId);
            setVersion(version);
        }
    }

    @Override
    public Version getVersion() {
        String versionId = dataStore.getContainerVersion(id);
        ProfileService profileService = fabricService.adapt(ProfileService.class);
        return versionId != null ? profileService.getVersion(versionId) : null;
    }

    @Override
    public void setVersion(Version version) {
        String currentId = getVersionId();
        int compareResult = version.getId().compareTo(currentId);
        if (compareResult != 0) {
            if (requiresUpgrade(version) && isManaged()) {
                String status = compareResult > 0 ? "upgrading" : "downgrading";
                dataStore.setContainerAttribute(id, DataStore.ContainerAttribute.ProvisionStatus, status);
            }
            dataStore.setContainerVersion(id, version.getId());
        }
    }

    @Override
    public Long getProcessId() {
        String pid = dataStore.getContainerAttribute(id, DataStore.ContainerAttribute.ProcessId, null, false, false);
        if( pid == null )
            return null;
        return Long.valueOf(pid);
    }

    
    @Override
    public List<String> getProfileIds() {
        return dataStore.getContainerProfiles(id);
    }

    @Override
    public Profile[] getProfiles() {
        Version version = getVersion();
        List<String> profileIds = dataStore.getContainerProfiles(id);
        List<Profile> profiles = new ArrayList<Profile>();
        for (String profileId : profileIds) {
            profiles.add(version.getRequiredProfile(profileId));
        }
        if (profiles.isEmpty() && version != null) {
            Profile defaultProfile = version.getProfile(ZkDefs.DEFAULT_PROFILE);
            if (defaultProfile != null) {
                profiles.add(defaultProfile);
            }
        }
        return profiles.toArray(new Profile[profiles.size()]);
    }

    public void setProfiles(Profile[] profiles) {
        String versionId = dataStore.getContainerVersion(id);
        List<String> currentProfileIds = dataStore.getContainerProfiles(id);
        List<String> profileIds = new ArrayList<String>();
        if (profiles != null) {
            for (Profile profile : profiles) {
                IllegalArgumentAssertion.assertTrue(versionId.equals(profile.getVersion()), "Version mismatch setting profile " + profile + ", expected version " + versionId);
                IllegalArgumentAssertion.assertFalse(profile.isAbstract(), "The profile " + profile + " is abstract and can not be associated to containers");
                IllegalArgumentAssertion.assertFalse(profile.getId().matches(ENSEMBLE_PROFILE_PATTERN) && !currentProfileIds.contains(profile.getId()), "The profile " + profile + " is not assignable.");
                profileIds.add(profile.getId());
            }
        }
        if (profileIds.isEmpty()) {
            profileIds.add(ZkDefs.DEFAULT_PROFILE);
        }
        dataStore.setContainerProfiles(id, profileIds);
    }

    @Override
    public void addProfiles(Profile... profiles) {
        List<Profile> addedProfiles = Arrays.asList(profiles);
        List<Profile> updatedProfileList = new LinkedList<Profile>();
        for (Profile p : getProfiles()) {
            updatedProfileList.add(p);
        }

        ProfileService profileService = fabricService.adapt(ProfileService.class);
        for (Profile addedProfile : addedProfiles) {
        	String versionId = addedProfile.getVersion();
            String profileId = addedProfile.getId();
            if (!profileService.hasProfile(versionId, profileId)) {
				throw new IllegalArgumentException("Profile " + profileId + " doesn't exist.");
            } else if (!updatedProfileList.contains(addedProfile)) {
                updatedProfileList.add(addedProfile);
            }
        }
        setProfiles(updatedProfileList.toArray(new Profile[updatedProfileList.size()]));
    }

    @Override
    public void removeProfiles(String... profileIds) {
        List<String> removedProfiles = Arrays.asList(profileIds);
        List<Profile> updatedProfileList = new LinkedList<>();
        for (String profileId : dataStore.getContainerProfiles(id)) {
            if (!removedProfiles.contains(profileId)) {
                Profile profile = getVersion().getProfile(profileId);
                if (profile != null) {
                    updatedProfileList.add(profile);
                }
            }
        }
        setProfiles(updatedProfileList.toArray(new Profile[updatedProfileList.size()]));
    }

    public Profile getOverlayProfile() {
		ProfileService profileService = fabricService.adapt(ProfileService.class);
		return profileService.getOverlayProfile(getContainerProfile());
    }

    public String getLocation() {
        return getOptionalAttribute(DataStore.ContainerAttribute.Location, "");
    }

    public void setLocation(String location) {
        setAttribute(DataStore.ContainerAttribute.Location, location);
    }

    public String getGeoLocation() {
        return getOptionalAttribute(DataStore.ContainerAttribute.GeoLocation, "");
    }

    public void setGeoLocation(String location) {
        setAttribute(DataStore.ContainerAttribute.GeoLocation, location);
    }

    /**
     * Returns the resolver of the {@link io.fabric8.api.Container}.
     * The resolver identifies which of the {@link io.fabric8.api.Container} address should be used for address resolution.
     *
     * @return One of the: localip, localhostname, publicip, publichostname, manualip.
     */
    @Override
    public String getResolver() {
        return getMandatorySubstitutedAttribute(DataStore.ContainerAttribute.Resolver);
    }

    /**
     * Sets the resolver value of the {@link io.fabric8.api.Container}.
     *
     * @param resolver the new resolver for this container
     */
    @Override
    public void setResolver(String resolver) {
        List<String> validResolverList = Arrays.asList(ZkDefs.VALID_RESOLVERS);
        if (!validResolverList.contains(resolver)) {
            throw new FabricException("Resolver " + resolver + " is not valid.");
        }
        setAttribute(DataStore.ContainerAttribute.Resolver, resolver);
    }

    /**
     * Returns the resolved address of the {@link io.fabric8.api.Container}.
     *
     * @return the resolved address for this container
     */
    @Override
    public String getIp() {
        return getMandatorySubstitutedAttribute(DataStore.ContainerAttribute.Ip);
    }

    @Override
    public String getLocalIp() {
        return getNullableSubstitutedAttribute(DataStore.ContainerAttribute.LocalIp);
    }

    @Override
    public void setLocalIp(String localIp) {
        setAttribute(DataStore.ContainerAttribute.LocalIp, localIp);
    }

    @Override
    public String getLocalHostname() {
        return getNullableSubstitutedAttribute(DataStore.ContainerAttribute.LocalHostName);
    }

    @Override
    public void setLocalHostname(String localHostname) {
        setAttribute(DataStore.ContainerAttribute.LocalHostName, localHostname);
    }

    @Override
    public String getPublicIp() {
        return getNullableSubstitutedAttribute(DataStore.ContainerAttribute.PublicIp);
    }

    @Override
    public void setPublicIp(String publicIp) {
        setAttribute(DataStore.ContainerAttribute.PublicIp, publicIp);
    }

    @Override
    public String getPublicHostname() {
        return getNullableSubstitutedAttribute(DataStore.ContainerAttribute.PublicHostName);
    }

    @Override
    public void setPublicHostname(String publicHostname) {
        setAttribute(DataStore.ContainerAttribute.PublicHostName, publicHostname);
    }

    @Override
    public String getManualIp() {
        return getNullableSubstitutedAttribute(DataStore.ContainerAttribute.ManualIp);
    }

    @Override
    public void setManualIp(String manualIp) {
        setAttribute(DataStore.ContainerAttribute.ManualIp, manualIp);
    }

    @Override
    public int getMinimumPort() {
        int minimumPort = 0;
        try {
            minimumPort = Integer.parseInt(getOptionalAttribute(DataStore.ContainerAttribute.PortMin, "0"));
        } catch (NumberFormatException e) {
            //ignore and fallback to 0
        }
        return minimumPort;
    }

    @Override
    public void setMinimumPort(int port) {
        setAttribute(DataStore.ContainerAttribute.PortMin, String.valueOf(port));
    }

    @Override
    public int getMaximumPort() {
        int maximumPort = 0;
        try {
            maximumPort = Integer.parseInt(getOptionalAttribute(DataStore.ContainerAttribute.PortMax, "0"));
        } catch (NumberFormatException e) {
            // Ignore and fallback to 0
        }
        return maximumPort;
    }

    @Override
    public void setMaximumPort(int port) {
        setAttribute(DataStore.ContainerAttribute.PortMax, String.valueOf(port));
    }

    public BundleInfo[] getBundles(ContainerTemplate containerTemplate) {
        try {
            return containerTemplate.execute(new ContainerTemplate.BundleStateCallback<BundleInfo[]>() {
                public BundleInfo[] doWithBundleState(BundleStateMBean bundleState) throws Exception {
                    TabularData bundles = bundleState.listBundles();
                    BundleInfo[] info = new BundleInfo[bundles.size()];

                    int i = 0;
                    for (Object data : bundles.values().toArray()) {
                        info[i++] = new JmxBundleInfo((CompositeData) data);
                    }

                    // sort bundles using bundle id to preserve same order like in framework
                    Arrays.sort(info, new BundleInfoComparator());
                    return info;
                }
            });
        } catch (Exception e) {
            logger.warn("Error while retrieving bundles. This exception will be ignored.", e);
            return new BundleInfo[0];
        }
    }

    public ServiceInfo[] getServices(ContainerTemplate containerTemplate) {
        try {
            return containerTemplate.execute(new ContainerTemplate.ServiceStateCallback<ServiceInfo[]>() {
                public ServiceInfo[] doWithServiceState(ServiceStateMBean serviceState) throws Exception {
                    TabularData services = serviceState.listServices();
                    ServiceInfo[] info = new ServiceInfo[services.size()];

                    int i = 0;
                    for (Object data : services.values().toArray()) {
                        CompositeData svc = (CompositeData) data;
                        info[i++] = new JmxServiceInfo(svc, serviceState.getProperties((Long) svc.get(ServiceStateMBean.IDENTIFIER)));
                    }

                    // sort services using service id to preserve same order like in framework
                    Arrays.sort(info, new ServiceInfoComparator());
                    return info;
                }
            });
        } catch (Exception e) {
            logger.warn("Error while retrieving services. This exception will be ignored.", e);
            return new ServiceInfo[0];
        }
    }

    public List<String> getJmxDomains() {
        String str = getOptionalAttribute(DataStore.ContainerAttribute.Domains, null);
        return str != null ? Arrays.asList(str.split("\n")) : Collections.<String>emptyList();
    }

    @Override
    public void setJmxDomains(List<String> jmxDomains) {
        String text = Strings.join(jmxDomains, "\n");
        setAttribute(DataStore.ContainerAttribute.Domains, text);
    }

    @Override
    public void start() {
        start(false);
    }

    @Override
    public void start(boolean force) {
        fabricService.startContainer(this, force);
    }

    @Override
    public void stop() {
        stop(false);
    }

    @Override
    public void stop(boolean force) {
        fabricService.stopContainer(this, force);
    }

    @Override
    public void destroy() {
        destroy(false);
    }

    @Override
    public void destroy(boolean force) {
        if (!hasAliveChildren()) {
            fabricService.destroyContainer(this, force);
        } else {
            throw new IllegalStateException("Container " + id + " has one or more child containers alive and cannot be destroyed.");
        }
    }

    public Container[] getChildren() {
        List<Container> children = new ArrayList<Container>();
        for (Container container : fabricService.getContainers()) {
            if (container.getParent() != null && getId().equals(container.getParent().getId())) {
                children.add(container);
            }
        }
        return children.toArray(new Container[children.size()]);
    }

    public String getType() {
        String answer = null;
        CreateContainerMetadata<?> containerMetadata = getMetadata();
        if (containerMetadata != null) {
            answer = containerMetadata.getContainerType();
        }
        if (Strings.isNullOrBlank(answer)) {
            answer = "karaf";
        }
        return answer;
    }

    @Override
    public void setType(String type) {
        if (metadata != null) {
            metadata.setContainerType(type);
        }
    }

    @Override
    public String getProvisionResult() {
        String status = getOptionalAttribute(DataStore.ContainerAttribute.ProvisionStatus, "");
        if (status.equals(PROVISION_SUCCESS)) {
            return getExtenderStatus();
        } else {
            return status;
        }
    }

    @Override
    public void setProvisionResult(String result) {
        setAttribute(DataStore.ContainerAttribute.ProvisionStatus, result);
    }

    @Override
    public String getProvisionException() {
        return getOptionalAttribute(DataStore.ContainerAttribute.ProvisionException, null);
    }

    @Override
    public void setProvisionException(String exception) {
        setAttribute(DataStore.ContainerAttribute.ProvisionException, exception);
    }

    @Override
    public List<String> getProvisionList() {
        String str = getOptionalAttribute(DataStore.ContainerAttribute.ProvisionList, null);
        return str != null ? Arrays.asList(str.split("\n")) : null;
    }

    @Override
    public void setProvisionList(List<String> bundles) {
        StringBuilder str = new StringBuilder();
        for (String b : bundles) {
            if (str.length() > 0) {
                str.append("\n");
            }
            str.append(b);
        }
        setAttribute(DataStore.ContainerAttribute.ProvisionList, str.toString());
    }

    @Override
    public Properties getProvisionChecksums() {
        String str = getOptionalAttribute(DataStore.ContainerAttribute.ProvisionChecksums, null);
        Properties answer = new Properties();
        if (str != null) {
            try {
                answer.load(new StringReader(str));
            } catch (IOException e) {
                logger.warn("Failed to convert provisionChecksums: " + str + " to a Properties object. " + e, e);
            }
        }
        return answer;
    }

    @Override
    public void setProvisionChecksums(Properties checksums) {
        // TODO we could merge the ProvisionChecksums and ProvisionList together to reduce writes into ZK?
        StringWriter writer = new StringWriter();
        try {
            checksums.store(writer, "provision checksums");
            setAttribute(DataStore.ContainerAttribute.ProvisionChecksums, writer.toString());
        } catch (IOException e) {
            logger.warn("Failed to convert provisionChecksums: " + checksums + " to a string. " + e, e);
        }
    }

    @Override
    public CreateContainerMetadata<?> getMetadata() {
        if (metadata == null) {
            metadata = getMetadata(getClass().getClassLoader());
            if (metadata == null) {
                for (Class<?> type : fabricService.getSupportedCreateContainerMetadataTypes()) {
                    metadata = getMetadata(type.getClassLoader());
                    if (metadata != null) {
                        break;
                    }
                }
            }
        }
        return metadata;
    }

    private CreateContainerMetadata<?> getMetadata(ClassLoader classLoader) {
        try {
            return dataStore.getContainerMetadata(id, classLoader);
        } catch (Exception e) {
            logger.debug("Error while retrieving metadata. This exception will be ignored.", e);
            return null;
        }
    }

    /**
     * Checks if container requires upgrade/rollback operation.
     */
    private boolean requiresUpgrade(Version version) {
        boolean requiresUpgrade = false;
        if (version.getId().compareTo(getVersionId()) == 0) {
            return false;
        }
        for (Profile oldProfile : getProfiles()) {
            Profile newProfile = version.getProfile(oldProfile.getId());
            if (newProfile != null && !Profiles.agentConfigurationEquals(fabricService, oldProfile, newProfile)) {
                requiresUpgrade = true;
                break;
            }
        }
        return requiresUpgrade;
    }

    /**
     * Checks if Container is root and has alive children.
     */
    public boolean hasAliveChildren() {
        for (Container child : getChildren()) {
            if (child.isAlive()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContainerImpl container = (ContainerImpl) o;

        if (id != null ? !id.equals(container.id) : container.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Container[" +
                "id=" + id +
                (parent != null ? ", parent=" + parent.getId() : "") +
                ']';
    }

    public boolean isAliveAndOK() {
        String status = getProvisionStatus();
        return isAlive() && !Strings.isNotBlank(getProvisionException()) && (status == null || status.length() == 0 || status.toLowerCase().startsWith(PROVISION_SUCCESS));
    }

    public Map<String, String> getProvisionStatusMap() {
        Map<String, String> answer = new HashMap<String, String>();
        answer.put(DataStore.ContainerAttribute.ProvisionStatus.name(), getProvisionStatus());
        answer.put(DataStore.ContainerAttribute.BlueprintStatus.name(), getBlueprintStatus().name());
        answer.put(DataStore.ContainerAttribute.SpringStatus.name(), getSpringStatus().name());
        return answer;
    }

    private String getOptionalAttribute(DataStore.ContainerAttribute attribute, String def) {
        return dataStore.getContainerAttribute(id, attribute, def, false, false);
    }

    private String getNullableSubstitutedAttribute(DataStore.ContainerAttribute attribute) {
        return dataStore.getContainerAttribute(id, attribute, null, false, true);
    }

    private String getMandatorySubstitutedAttribute(DataStore.ContainerAttribute attribute) {
        return dataStore.getContainerAttribute(id, attribute, null, true, true);
    }

    private void setAttribute(DataStore.ContainerAttribute attribute, String value) {
        dataStore.setContainerAttribute(id, attribute, value);
    }

    private String getExtenderStatus() {
        ModuleStatus blueprintStatus = getBlueprintStatus();
        ModuleStatus springStatus = getSpringStatus();
        if (blueprintStatus != ModuleStatus.STARTED) {
            return blueprintStatus.name().toLowerCase();
        } else if (springStatus != ModuleStatus.STARTED) {
            return springStatus.name().toLowerCase();
        } else {
            return PROVISION_SUCCESS;
        }
    }

    private ModuleStatus getSpringStatus() {
        return Enum.valueOf(ModuleStatus.class, getOptionalAttribute(DataStore.ContainerAttribute.SpringStatus, ModuleStatus.STARTED.name()));
    }

    private ModuleStatus getBlueprintStatus() {
        return Enum.valueOf(ModuleStatus.class, getOptionalAttribute(DataStore.ContainerAttribute.BlueprintStatus, ModuleStatus.STARTED.name()));
    }

	private Profile getContainerProfile() {
		Version version = getVersion();
		String profileId = "#container-" + getId();
		ProfileBuilder builder = ProfileBuilder.Factory.create(profileId).version(version.getId());
		ContainerProfileOptions optionsProvider = new ContainerProfileOptions(getId(), version, dataStore);
		return builder.addOptions(optionsProvider).getProfile();
	}

	static class ContainerProfileOptions implements OptionsProvider<ProfileBuilder> {

	    private static Logger LOGGER = LoggerFactory.getLogger(ContainerImpl.class);
	    
		private final String cntId;
		private final DataStore dataStore;
	    private final Version version;

	    ContainerProfileOptions(String cntId, Version version, DataStore dataStore) {
	    	this.dataStore = dataStore;
	    	this.version = version;
	    	this.cntId = cntId;
	    }

	    @Override
		public ProfileBuilder addOptions(ProfileBuilder builder) {
	        List<String> missingProfiles = new ArrayList<>();
			List<String> profileIds = dataStore.getContainerProfiles(cntId);
			LOGGER.debug("Building container overlay for {} with profile: {}", cntId, profileIds);
            for (String profileId : profileIds) {
                if (version.hasProfile(profileId)) {
                    builder.addParent(profileId);
                } else {
                    missingProfiles.add(profileId);
                }
	        }
			if (!missingProfiles.isEmpty()) {
	            LOGGER.warn("Container overlay has missing profiles: {}", missingProfiles);
                builder.addAttribute("missing.profiles", missingProfiles.toString());
                // builder.addConfiguration(Constants.AGENT_PID, "disabled", "true");
			}
			return builder;
	    }
	}
}
