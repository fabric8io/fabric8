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

import io.fabric8.api.DataStore;
import io.fabric8.api.Container;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricRequirements;
import io.fabric8.api.FabricService;
import io.fabric8.api.OptionsProvider;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileService;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.Version;
import io.fabric8.api.VersionBuilder;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.utils.DataStoreUtils;
import io.fabric8.utils.SystemProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(ProfileService.class)
public final class ProfileServiceImpl extends AbstractComponent implements ProfileService {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileServiceImpl.class);
    
    @Reference(referenceInterface = DataStore.class)
    private final ValidatingReference<DataStore> dataStore = new ValidatingReference<>();
    @Reference(referenceInterface = RuntimeProperties.class)
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();
    
    @Activate
    void activate() throws Exception {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }
    
	@Override
	public List<String> getVersions() {
        assertValid();
        return dataStore.get().getVersions();
	}

	@Override
    public boolean hasVersion(String versionId) {
        assertValid();
        return dataStore.get().hasVersion(versionId);
    }

    @Override
	public Version getVersion(String versionId) {
        assertValid();
        
    	// [TODO] This is check-then-act -- make atomic using locks
    	if (!dataStore.get().hasVersion(versionId))
    		return null;
    	
		VersionBuilder builder = VersionBuilder.Factory.create(versionId);
		builder.setAttributes(dataStore.get().getVersionAttributes(versionId));
		
        HashMap<String, Profile> profiles = new HashMap<String, Profile>();
        for (String profileId : dataStore.get().getProfiles(versionId)) {
        	builder.addProfile(getProfileInternal(versionId, profileId, profiles));
        }
        
        return builder.getVersion();
	}

	@Override
	public Version getRequiredVersion(String versionId) {
        assertValid();
        Version version = getVersion(versionId);
		IllegalStateAssertion.assertNotNull(version, "Version '" + versionId + "' does not exist");
        return version;
	}

	@Override
	public Version createVersion(Version version) {
        assertValid();
        
        LOGGER.info("createVersion: {}", version);
        
        // [TODO] make atomic using locks
        String versionId = version.getId();
        if (version.getParentId() != null) {
    		dataStore.get().createVersion(version.getParentId(), versionId);
        } else {
    		dataStore.get().createVersion(versionId);
            for (Entry<String, String> entry : version.getAttributes().entrySet()) {
                dataStore.get().setVersionAttribute(versionId, entry.getKey(), entry.getValue());
            }
            for (Profile profile : version.getProfiles()) {
            	createOrUpdateProfile(profile, true);
            }
        }
        return getRequiredVersion(versionId);
	}

    @Override
    public Profile createProfile(Profile profile) {
        assertValid();
        createOrUpdateProfile(profile, true);
        return getProfileInternal(profile.getVersion(), profile.getId());
    }

    @Override
    public Profile updateProfile(Profile profile) {
        assertValid();
        createOrUpdateProfile(profile, false);
        return getProfileInternal(profile.getVersion(), profile.getId());
    }

    private void createOrUpdateProfile(Profile profile, boolean create) {
        String versionId = profile.getVersion();
        String profileId = profile.getId();
        
        if (create) {
            LOGGER.info("createProfile: {}", profile);
            dataStore.get().createProfile(versionId, profileId);
        } else {
            LOGGER.info("updateProfile: {}", profile);
        }
        
        // Attributes
        for (Entry<String, String> entry : profile.getAttributes().entrySet()) {
            dataStore.get().setProfileAttribute(versionId, profileId, entry.getKey(), entry.getValue());
        }

        // Parent Profiles
        List<Profile> parents = profile.getParents();
        if (parents.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (Profile parent : parents) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(parent.getId());
            }
            dataStore.get().setProfileAttribute(versionId, profileId, Profile.PARENTS, sb.toString());
        }
        
        // FileConfigurations
        Map<String, byte[]> fileConfigurations = profile.getFileConfigurations();
        if (!fileConfigurations.isEmpty()) {
            dataStore.get().setFileConfigurations(versionId, profileId, fileConfigurations);
        }
        
        // Configurations
        Map<String, Map<String, String>> configurations = profile.getConfigurations();
        if (!configurations.isEmpty()) {
            dataStore.get().setConfigurations(versionId, profileId, configurations);
        }
    }

    @Override
	public boolean hasProfile(String versionId, String profileId) {
    	assertValid();
        return dataStore.get().hasProfile(versionId, profileId);
	}

    @Override
	public Profile getRequiredProfile(String versionId, String profileId) {
    	assertValid();
    	return getProfileInternal(versionId, profileId);
	}
    
    private Profile getProfileInternal(String versionId, String profileId) {
    	return getProfileInternal(versionId, profileId, new HashMap<String, Profile>());
    }

    private Profile getProfileInternal(String versionId, String profileId, Map<String, Profile> profiles) {
        Profile profile = profiles.get(profileId);
        if (profile == null) {
            // [TODO] This is check-then-act -- make atomic using locks
            boolean hasProfile = dataStore.get().hasProfile(versionId, profileId);
            IllegalStateAssertion.assertTrue(hasProfile, "Profile '" + profileId + "' does not exist in version: " + versionId);
            
            Map<String, String> attributes = dataStore.get().getProfileAttributes(versionId, profileId);
            ProfileBuilder builder = ProfileBuilder.Factory.create(profileId).version(versionId).setAttributes(attributes);
            
            String parentsAttr = attributes.get(Profile.PARENTS);
            if (parentsAttr != null && !parentsAttr.isEmpty()) {
                for (String parentId : parentsAttr.trim().split(" ")) {
                    Profile parent = profiles.get(parentId);
                    if (parent == null) {
                        parent = getProfileInternal(versionId, parentId, profiles);
                    }
                    builder.addParent(parent);
                }
            }

            builder.setFileConfigurations(dataStore.get().getFileConfigurations(versionId, profileId));
            builder.setConfigurations(dataStore.get().getConfigurations(versionId, profileId));
            builder.setLastModified(dataStore.get().getLastModified(versionId, profileId));
            
            profile = builder.getProfile();
            profiles.put(profile.getId(), profile);
        }
        return profile;
	}

    @Override
    public void deleteVersion(String versionId) {
    	assertValid();
        LOGGER.info("deleteVersion: {}", versionId);
        dataStore.get().deleteVersion(versionId);
    }

	@Override
	public Profile getOverlayProfile(Profile profile) {
		assertValid();
        return getOverlayInternal(profile);
	}

	@Override
	public void deleteProfile(FabricService fabricService, String versionId, String profileId, boolean force) {
    	assertValid();
    	
        Profile profile = getRequiredProfile(versionId, profileId);
        LOGGER.info("deleteProfile: {}", profile);
        
        // TODO: what about child profiles ?
        Container[] containers = fabricService.getAssociatedContainers(versionId, profileId);
        if (containers.length == 0) {
            dataStore.get().deleteProfile(versionId, profileId);
        } else if (force) {
            for (Container container : containers) {
                container.removeProfiles(profileId);
            }
            dataStore.get().deleteProfile(versionId, profileId);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Cannot delete profile:").append(profileId).append(".");
            sb.append("Profile has assigned ").append(containers.length).append(" container(s):");
            for (Container c : containers) {
                sb.append(" ").append(c.getId());
            }
            sb.append(". Use force option to also remove the profile from the containers.");
            throw new FabricException(sb.toString());
        }

        // lets remove any pending requirements on this profile
        FabricRequirements requirements = fabricService.getRequirements();
        if (requirements.removeProfileRequirements(profileId)) {
            try {
                fabricService.setRequirements(requirements);
            } catch (IOException e) {
                throw new FabricException("Failed to update requirements after deleting profile " + profileId + ". " + e, e);
            }
        }
	}

    private Profile getOverlayInternal(Profile profile) {
        Profile overlayProfile;
        if (profile.isOverlay()) {
            overlayProfile = profile;
        } else {
            String environment = runtimeProperties.get().getProperty(SystemProperties.FABRIC_ENVIRONMENT);
            ProfileBuilder builder = ProfileBuilder.Factory.create(profile.getVersion(), profile.getId());
            builder.addOptions(new OverlayOptionsProvider(profile, environment));
            overlayProfile = builder.getProfile();
        }
        return overlayProfile;
    }
    
    void bindDataStore(DataStore service) {
        this.dataStore.bind(service);
    }
    void unbindDataStore(DataStore service) {
        this.dataStore.unbind(service);
    }
    
    void bindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.bind(service);
    }
    void unbindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.unbind(service);
    }
    
    static class OverlayOptionsProvider implements OptionsProvider<ProfileBuilder> {

        private final Profile self;
        private final String environment;

        private static class SupplementControl {
            byte[] data;
            Properties props;
        }
        
        OverlayOptionsProvider(Profile self, String environment) {
            this.self = self;
            this.environment = environment;
        }

        @Override
        public ProfileBuilder addOptions(ProfileBuilder builder) {
            builder.setAttributes(self.getAttributes());
            builder.setParents(self.getParents());
            builder.setFileConfigurations(getFileConfigurations());
            builder.setConfigurations(getConfigurations());
            builder.setLastModified(getLastModified());
            builder.setOverlay(true);
            return builder;
        }

        private Map<String, byte[]> getFileConfigurations() {
            try {
                Map<String, SupplementControl> aggregate = new HashMap<String, SupplementControl>();
                for (Profile profile : getInheritedProfiles()) {
                    supplement(profile, aggregate);
                }

                Map<String, byte[]> rc = new HashMap<String, byte[]>();
                for (Map.Entry<String, SupplementControl> entry : aggregate.entrySet()) {
                    SupplementControl ctrl = entry.getValue();
                    if (ctrl.props != null) {
                        ctrl.data = DataStoreUtils.toBytes(ctrl.props);
                    }
                    rc.put(entry.getKey(), ctrl.data);
                }
                return rc;
            } catch (Exception e) {
                throw FabricException.launderThrowable(e);
            }
        }
        
        private Map<String, Map<String, String>> getConfigurations() {
            try {
                Map<String, SupplementControl> aggregate = new HashMap<String, SupplementControl>();
                for (Profile profile : getInheritedProfiles()) {
                    supplement(profile, aggregate);
                }

                Map<String, Map<String, String>> rc = new HashMap<String, Map<String, String>>();
                for (Map.Entry<String, SupplementControl> entry : aggregate.entrySet()) {
                    SupplementControl ctrl = entry.getValue();
                    if (ctrl.props != null) {
                        rc.put(DataStoreUtils.stripSuffix(entry.getKey(), ".properties"), DataStoreUtils.toMap(ctrl.props));
                    }
                }
                return rc;
            } catch (Exception e) {
                throw FabricException.launderThrowable(e);
            }
        }
        
        private List<Profile> getInheritedProfiles() {
            List<Profile> profiles = new ArrayList<Profile>();
            fillParentProfiles(self, profiles);
            return profiles;
        }

        private void fillParentProfiles(Profile profile, List<Profile> profiles) {
            for (Profile p : profile.getParents()) {
                fillParentProfiles(p, profiles);
            }
            if (!profiles.contains(profile)) {
                profiles.add(profile);
            }
        }

        private void supplement(Profile profile, Map<String, SupplementControl> aggregate) throws Exception {
            // TODO fix this, should this every happen???
            if (profile instanceof OverlayOptionsProvider) {
                if (((OverlayOptionsProvider) profile).self.equals(self)) {
                    return;
                }
            }

            Map<String, byte[]> configs = profile.getFileConfigurations();
            for (String key : configs.keySet()) {
                // Ignore environment specific configs
                if (key.contains("#")) {
                    continue;
                }
                byte[] value = configs.get(key);
                if (environment != null && configs.containsKey(key + "#" + environment)) {
                    value = configs.get(key + "#" + environment);
                }
                // we can use fine grained inheritance based updating if it's
                // a properties file.
                String fileName = key;
                if (fileName.endsWith(".properties")) {
                    SupplementControl ctrl = aggregate.get(fileName);
                    if (ctrl != null) {
                        // we can update the file..
                        Properties childMap = DataStoreUtils.toProperties(value);
                        if (childMap.remove(Profile.DELETED) != null) {
                            ctrl.props.clear();
                        }

                        // Update the entries...
                        for (Map.Entry<Object, Object> p : childMap.entrySet()) {
                            if (Profile.DELETED.equals(p.getValue())) {
                                ctrl.props.remove(p.getKey());
                            } else {
                                ctrl.props.put(p.getKey(), p.getValue());
                            }
                        }

                    } else {
                        // new file..
                        ctrl = new SupplementControl();
                        ctrl.props = DataStoreUtils.toProperties(value);
                        aggregate.put(fileName, ctrl);
                    }
                } else {
                    // not a properties file? we can only overwrite.
                    SupplementControl ctrl = new SupplementControl();
                    ctrl.data = value;
                    aggregate.put(fileName, ctrl);
                }
            }
        }

        private String getLastModified() {
            StringBuilder sb = new StringBuilder();
            sb.append(self.getProfileHash());
            for (Profile parent : self.getParents()) {
                sb.append("-").append(parent.getProfileHash());
            }
            return sb.toString();
        }
    }
}
