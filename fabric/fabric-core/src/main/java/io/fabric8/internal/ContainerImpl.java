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
package io.fabric8.internal;

import io.fabric8.api.Constants;
import io.fabric8.api.Container;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.DataStore;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricService;
import io.fabric8.api.ModuleStatus;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.api.data.BundleInfo;
import io.fabric8.api.data.ServiceInfo;
import io.fabric8.service.ContainerTemplate;
import io.fabric8.zookeeper.ZkDefs;
import org.osgi.jmx.framework.BundleStateMBean;
import org.osgi.jmx.framework.ServiceStateMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

public class ContainerImpl implements Container {

    private static final String ENSEMBLE_PROFILE_PATTERN = "fabric-ensemble-[0-9]*-[0-9]*";

    /**
     * Logger.
     */
    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    private final Container parent;
    private final String id;
    private final FabricService service;
    private CreateContainerMetadata<?> metadata;
    private long processId;

    public ContainerImpl(Container parent, String id, FabricService service) {
        this.parent = parent;
        this.id = id;
        this.service = service;
    }

    public FabricService getFabricService() {
		return service;
	}

	public Container getParent() {
        return parent;
    }

    public String getId() {
        return id;
    }

    public boolean isAlive() {
        return service.getDataStore().isContainerAlive(id);
    }

    public boolean isRoot() {
        return parent == null;
    }

    @Override
    public boolean isEnsembleServer() {
        try {
            List<String> containers = service.getDataStore().getEnsembleContainers();
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
        return PROVISION_SUCCESS.equals(result) || PROVISION_ERROR.equals(result);
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
    public String getHttpUrl() {
        return getMandatorySubstitutedAttribute(DataStore.ContainerAttribute.HttpUrl);
    }

    @Override
    public boolean isManaged() {
        Map<String, String> agentConfig = getOverlayProfile().getConfiguration(Constants.AGENT_PID);
        if (agentConfig != null) {
            String disabled = getOverlayProfile().getConfiguration(Constants.AGENT_PID).get("disabled");
            return !"true".equals(disabled);
        }
        return false;
    }

    @Override
    public Version getVersion() {
        String versionId = service.getDataStore().getContainerVersion(id);
        if (versionId == null) {
            return null;
        }
        return service.getVersion(versionId);
    }

    @Override
    public void setVersion(Version version) {
        if (version.compareTo(getVersion()) != 0) {
            if (requiresUpgrade(version) && isManaged()) {
                String status = version.compareTo(getVersion()) > 0 ? "upgrading" : "downgrading";
                service.getDataStore().setContainerAttribute(id, DataStore.ContainerAttribute.ProvisionStatus, status);
            }
            service.getDataStore().setContainerVersion(id, version.getId());
        }
    }

    @Override
    public Long getProcessId() {
        String pid = service.getDataStore().getContainerAttribute(id, DataStore.ContainerAttribute.ProcessId, null, false, false);
        if( pid == null )
            return null;
        return new Long(pid);
    }

    public Profile[] getProfiles() {
        Version version = getVersion();
        List<String> profileIds = service.getDataStore().getContainerProfiles(id);
        List<Profile> profiles = new ArrayList<Profile>();
        for (String profileId : profileIds) {
            profiles.add(version.getProfile(profileId));
        }
        if (profiles.isEmpty()) {
            profiles.add(version.getProfile(ZkDefs.DEFAULT_PROFILE));
        }
        return profiles.toArray(new Profile[profiles.size()]);
    }

    public void setProfiles(Profile[] profiles) {
        String versionId = service.getDataStore().getContainerVersion(id);
        List<String> currentProfileIds = service.getDataStore().getContainerProfiles(id);
        List<String> profileIds = new ArrayList<String>();
        if (profiles != null) {
            for (Profile profile : profiles) {
                if (!versionId.equals(profile.getVersion())) {
                    throw new IllegalArgumentException("Version mismatch setting profile " + profile.getId() + " with version "
                            + profile.getVersion() + " expected version " + versionId);
                } else if (profile.isAbstract()) {
                    throw new IllegalArgumentException("The profile " + profile.getId() + " is abstract and can not "
                            + "be associated to containers");
                } else if (profile.getId().matches(ENSEMBLE_PROFILE_PATTERN) && !currentProfileIds.contains(profile.getId())) {
                    throw new IllegalArgumentException("The profile " + profile.getId() + " is not assignable.");
                }
                profileIds.add(profile.getId());
            }
        }
        if (profileIds.isEmpty()) {
            profileIds.add(ZkDefs.DEFAULT_PROFILE);
        }
        service.getDataStore().setContainerProfiles(id, profileIds);
    }

    @Override
    public void addProfiles(Profile... profiles) {
        List<Profile> addedProfiles = Arrays.asList(profiles);
        List<Profile> updatedProfileList = new LinkedList<Profile>();
        for (Profile p : getProfiles()) {
            updatedProfileList.add(p);
        }

        for (Profile addedProfile : addedProfiles) {
            if (!addedProfile.exists()) {
                throw new IllegalArgumentException("Profile "+addedProfile.getId()+" doesn't exist.");
            } else if (!updatedProfileList.contains(addedProfile)) {
                updatedProfileList.add(addedProfile);
            }
        }
        setProfiles(updatedProfileList.toArray(new Profile[updatedProfileList.size()]));
    }

    @Override
    public void removeProfiles(Profile... profiles) {
        List<Profile> removedProfiles = Arrays.asList(profiles);
        List<Profile> updatedProfileList = new LinkedList<Profile>();
        for (String p : service.getDataStore().getContainerProfiles(id)) {
            Profile profile = getVersion().hasProfile(p) ? getVersion().getProfile(p) : new ProfileImpl(p, getVersion().getId(), service);
            if (!removedProfiles.contains(profile))
                updatedProfileList.add(profile);
        }
        setProfiles(updatedProfileList.toArray(new Profile[updatedProfileList.size()]));
    }


    public Profile getOverlayProfile() {
        return new ProfileOverlayImpl(new ContainerProfile(), true, service.getDataStore(), service.getEnvironment());
    }

    private class ContainerProfile extends ProfileImpl {
        private ContainerProfile() {
            super("#container-" + ContainerImpl.this.id,
                    ContainerImpl.this.getVersion().getId(),
                    ContainerImpl.this.service);
        }

        @Override
        public Profile[] getParents() {
            Version v = service.getVersion(getVersion());
            List<Profile> parents = new ArrayList<Profile>();
            for (String p :service.getDataStore().getContainerProfiles(id)) {
                try {
                    parents.add(v.getProfile(p));
                } catch (Exception e) {
                    //We ignore profiles that threw an error (e.g. they don't exist).
                }
            }
            return parents.toArray(new Profile[parents.size()]);
        }

        @Override
        public Map<String, String> getAttributes() {
            return Collections.emptyMap();
        }

        @Override
        public void setAttribute(String key, String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Container[] getAssociatedContainers() {
            return new Container[]{ContainerImpl.this};
        }

        @Override
        public Map<String, byte[]> getFileConfigurations() {
            return Collections.emptyMap();
        }

        @Override
        public void setFileConfigurations(Map<String, byte[]> configurations) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, Map<String, String>> getConfigurations() {
            return Collections.emptyMap();
        }

        @Override
        public void setConfigurations(Map<String, Map<String, String>> configurations) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete() {
            throw new UnsupportedOperationException();
        }

        /**
         * Returns the time in milliseconds of the last modification of the profile.
         */
        @Override
        public String getProfileHash() {
            StringBuilder sb = new StringBuilder();
            Version v = service.getVersion(getVersion());
            boolean first = true;
            for (String p :service.getDataStore().getContainerProfiles(id)) {
                try {
                    if (!first) {
                        sb.append("-");
                    } else {
                        first = false;
                    }
                    sb.append(v.getProfile(p));
                } catch (Exception e) {
                    //We ignore profiles that threw an error (e.g. they don't exist).
                }
            }
            return sb.toString();
        }
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
    public void start() {
        start(false);
    }

    @Override
    public void start(boolean force) {
        service.startContainer(this, force);
    }

    @Override
    public void stop() {
        stop(false);
    }

    @Override
    public void stop(boolean force) {
        service.stopContainer(this, force);
    }

    @Override
    public void destroy() {
        destroy(false);
    }

    @Override
    public void destroy(boolean force) {
        if (!hasAliveChildren()) {
            service.destroyContainer(this, force);
        } else {
            throw new IllegalStateException("Container " + id + " has one or more child containers alive and cannot be destroyed.");
        }
    }

    public Container[] getChildren() {
        List<Container> children = new ArrayList<Container>();
        for (Container container : service.getContainers()) {
            if (container.getParent() != null && getId().equals(container.getParent().getId())) {
                children.add(container);
            }
        }
        return children.toArray(new Container[children.size()]);
    }

    public String getType() {
        return "karaf";
    }

    @Override
    public String getProvisionResult() {
        String status = getOptionalAttribute(DataStore.ContainerAttribute.ProvisionStatus, "");
        if (status.equals("success")) {
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
        try {
            if (metadata == getMetadata(getClass().getClassLoader())) {
                for (Class type : service.getSupportedCreateContainerMetadataTypes()) {
                    metadata = getMetadata(type.getClassLoader());
                    if (metadata != null) {
                        return metadata;
                    }
                }
            }
            return metadata;
        } catch (Exception e) {
            logger.warn("Error while retrieving metadata. This exception will be ignored.", e);
            return null;
        }
    }

    private CreateContainerMetadata<?> getMetadata(ClassLoader classLoader) {
        try {
            if (metadata == null) {
                metadata = service.getDataStore().getContainerMetadata(id, classLoader);
            }
            return metadata;
        } catch (Exception e) {
            logger.debug("Error while retrieving metadata. This exception will be ignored.", e);
            return null;
        }
    }

    public void setMetadata(CreateContainerMetadata<?> metadata) {
        this.metadata = metadata;
    }

    /**
     * Checks if container requires upgrade/rollback operation.
     */
    private boolean requiresUpgrade(Version version) {
        boolean requiresUpgrade = false;
        if (version.compareTo(getVersion()) == 0) {
            return false;
        }
        for (Profile oldProfile : getProfiles()) {
            // get new profile
            Profile newProfile = version.getProfile(oldProfile.getId());
            if (newProfile != null && !oldProfile.agentConfigurationEquals(newProfile)) {
                requiresUpgrade = true;
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
        return isAlive() && (status == null || status.length() == 0 || status.toLowerCase().startsWith("success"));
    }

    public Map<String, String> getProvisionStatusMap() {
        HashMap<String, String> answer = new HashMap<String, String>();
        answer.put(DataStore.ContainerAttribute.ProvisionStatus.name(), getProvisionStatus());
        answer.put(DataStore.ContainerAttribute.BlueprintStatus.name(), getBlueprintStatus().name());
        answer.put(DataStore.ContainerAttribute.SpringStatus.name(), getSpringStatus().name());
        return answer;
    }

    private String getOptionalAttribute(DataStore.ContainerAttribute attribute, String def) {
        return service.getDataStore().getContainerAttribute(id, attribute, def, false, false);
    }

    private String getNullableSubstitutedAttribute(DataStore.ContainerAttribute attribute) {
        return service.getDataStore().getContainerAttribute(id, attribute, null, false, true);
    }

    private String getMandatorySubstitutedAttribute(DataStore.ContainerAttribute attribute) {
        return service.getDataStore().getContainerAttribute(id, attribute, null, true, true);
    }

    private void setAttribute(DataStore.ContainerAttribute attribute, String value) {
        service.getDataStore().setContainerAttribute(id, attribute, value);
    }

    private String getExtenderStatus() {
        ModuleStatus blueprintStatus = getBlueprintStatus();
        ModuleStatus springStatus = getSpringStatus();
        if (blueprintStatus != ModuleStatus.STARTED) {
            return blueprintStatus.name().toLowerCase();
        } else if (springStatus != ModuleStatus.STARTED) {
            return springStatus.name().toLowerCase();
        } else {
            return "success";
        }
    }

    private ModuleStatus getSpringStatus() {
        return Enum.valueOf(ModuleStatus.class, getOptionalAttribute(DataStore.ContainerAttribute.SpringStatus, ModuleStatus.STARTED.name()));
    }

    private ModuleStatus getBlueprintStatus() {
        return Enum.valueOf(ModuleStatus.class, getOptionalAttribute(DataStore.ContainerAttribute.BlueprintStatus, ModuleStatus.STARTED.name()));
    }
}
