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

import io.fabric8.api.Container;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricRequirements;
import io.fabric8.api.FabricService;
import io.fabric8.api.OptionsProvider;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileRegistry;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Profiles;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.SystemProperties;
import io.fabric8.api.Version;
import io.fabric8.api.permit.PermitManager;
import io.fabric8.api.scr.AbstractProtectedComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.api.visibility.VisibleForExternal;
import io.fabric8.utils.DataStoreUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@References({ @Reference(referenceInterface = PermitManager.class) })
@Service(ProfileService.class)
public final class ProfileServiceImpl extends AbstractProtectedComponent<ProfileService> implements ProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileServiceImpl.class);

    @Reference(referenceInterface = ProfileRegistry.class)
    private final ValidatingReference<ProfileRegistry> profileRegistry = new ValidatingReference<>();
    @Reference(referenceInterface = RuntimeProperties.class)
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<>();

    static class OverlayAudit {
        Map<String, Profile> overlayProfiles = new HashMap<String, Profile>();
    }

    @Activate
    @VisibleForExternal
    public void activate() throws Exception {
        getOverlayAudit();
        activateComponent(PERMIT, this);
    }

    private OverlayAudit getOverlayAudit() {
        synchronized (runtimeProperties) {
            RuntimeProperties sysprops = runtimeProperties.get();
            OverlayAudit audit = sysprops.getRuntimeAttribute(OverlayAudit.class);
            if (audit == null) {
                sysprops.putRuntimeAttribute(OverlayAudit.class, audit = new OverlayAudit());
            }
            return audit;
        }
    }

    @Deactivate
    void deactivate() {
        deactivateComponent(PERMIT);
    }

    @Override
    public Version createVersion(Version version) {
        assertValid();
        LOGGER.info("createVersion: {}", version);
        String versionId = profileRegistry.get().createVersion(version);
        return getRequiredVersion(versionId);
    }

    @Override
    public Version createVersionFrom(String sourceId, String targetId, Map<String, String> attributes) {
        assertValid();
        LOGGER.info("createVersion: {} => {}", sourceId, targetId);
        profileRegistry.get().createVersion(sourceId, targetId, attributes);
        return getRequiredVersion(targetId);
    }

    @Override
    public List<String> getVersions() {
        assertValid();
        return profileRegistry.get().getVersionIds();
    }

    @Override
    public boolean hasVersion(String versionId) {
        assertValid();
        return profileRegistry.get().hasVersion(versionId);
    }

    @Override
    public Version getVersion(String versionId) {
        assertValid();
        return profileRegistry.get().getVersion(versionId);
    }

    @Override
    public Version getRequiredVersion(String versionId) {
        assertValid();
        return profileRegistry.get().getRequiredVersion(versionId);
    }

    @Override
    public Profile createProfile(Profile profile) {
        assertValid();
        LOGGER.info("createProfile: {}", profile);
        String profileId = profileRegistry.get().createProfile(profile);
        return getRequiredProfile(profile.getVersion(), profileId);
    }

    @Override
    public Profile updateProfile(Profile profile) {
        assertValid();
        LOGGER.info("updateProfile: {}", profile);
        String profileId = profileRegistry.get().updateProfile(profile);
        return getRequiredProfile(profile.getVersion(), profileId);
    }

    @Override
    public boolean hasProfile(String versionId, String profileId) {
        assertValid();
        return profileRegistry.get().hasProfile(versionId, profileId);
    }

    @Override
    public Profile getProfile(String versionId, String profileId) {
        assertValid();
        return profileRegistry.get().getProfile(versionId, profileId);
    }

    @Override
    public Profile getRequiredProfile(String versionId, String profileId) {
        assertValid();
        return profileRegistry.get().getRequiredProfile(versionId, profileId);
    }

    @Override
    public void deleteVersion(String versionId) {
        assertValid();
        LOGGER.info("deleteVersion: {}", versionId);
        profileRegistry.get().deleteVersion(versionId);
    }

    @Override
    public Profile getOverlayProfile(Profile profile) {
        assertValid();
        Profile overlayProfile;
        synchronized (this) {
            if (profile.isOverlay()) {
                LOGGER.debug("getOverlayProfile, given profile is already an overlay: " + profile);
                overlayProfile = profile;
            } else {
                String profileId = profile.getId();
                String environment = runtimeProperties.get().getProperty(SystemProperties.FABRIC_ENVIRONMENT);
                if (environment == null) {
                    // lets default to the environment from the current active
                    // set of profiles (e.g. docker or openshift)
                    environment = System.getProperty(SystemProperties.FABRIC_PROFILE_ENVIRONMENT);
                }
                Version version = getRequiredVersion(profile.getVersion());
                ProfileBuilder builder = ProfileBuilder.Factory.create(profile.getVersion(), profileId);
                builder.addOptions(new OverlayOptionsProvider(version, profile, environment));
                overlayProfile = builder.getProfile();

                // Log the overlay profile difference
                if (LOGGER.isDebugEnabled()) {
                    OverlayAudit audit = getOverlayAudit();
                    synchronized (audit) {
                        Profile lastOverlay = audit.overlayProfiles.get(profileId);
                        if (lastOverlay == null) {
                            LOGGER.debug("Overlay" + Profiles.getProfileInfo(overlayProfile));
                            audit.overlayProfiles.put(profileId, overlayProfile);
                        } else if (!lastOverlay.equals(overlayProfile)) {
                            LOGGER.debug("Overlay" + Profiles.getProfileDifference(lastOverlay, overlayProfile));
                            audit.overlayProfiles.put(profileId, overlayProfile);
                        }
                    }
                }
            }
        }
        return overlayProfile;
    }

    @Override
    public void deleteProfile(String versionId, String profileId, boolean force) {
        deleteProfile(null, versionId, profileId, force);
    }

    @Override
    public void deleteProfile(FabricService fabricService, String versionId, String profileId, boolean force) {
        assertValid();

        Profile profile = getRequiredProfile(versionId, profileId);
        LOGGER.info("deleteProfile: {}", profile);

        // TODO: what about child profiles ?
        Container[] containers = fabricService != null ? fabricService.getAssociatedContainers(versionId, profileId) : new Container[0];
        if (containers.length == 0) {
            profileRegistry.get().deleteProfile(versionId, profileId);
        } else if (force) {
            for (Container container : containers) {
                container.removeProfiles(profileId);
            }
            profileRegistry.get().deleteProfile(versionId, profileId);
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
        FabricRequirements requirements = fabricService != null ? fabricService.getRequirements() : null;
        if (requirements != null && requirements.removeProfileRequirements(profileId)) {
            try {
                fabricService.setRequirements(requirements);
            } catch (IOException e) {
                throw new FabricException("Failed to update requirements after deleting profile " + profileId + ". " + e, e);
            }
        }
    }

    static class OverlayOptionsProvider implements OptionsProvider<ProfileBuilder> {

        private final Version version;
        private final Profile self;
        private final String environment;

        private static class SupplementControl {
            byte[] data;
            Properties props;
        }

        OverlayOptionsProvider(Version version, Profile self, String environment) {
            this.version = version;
            this.self = self;
            this.environment = environment;
        }

        @Override
        public ProfileBuilder addOptions(ProfileBuilder builder) {
            builder.setAttributes(self.getAttributes());
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
            List<Profile> profiles = new ArrayList<>();
            fillParentProfiles(self, profiles);
            return profiles;
        }

        private void fillParentProfiles(Profile profile, List<Profile> profiles) {
            if (!profiles.contains(profile)) {
                for (String parentId : profile.getParentIds()) {
                    Profile parent = version.getRequiredProfile(parentId);
                    fillParentProfiles(parent, profiles);
                }
                profiles.add(profile);
            }
        }

        private void supplement(Profile profile, Map<String, SupplementControl> aggregate) throws Exception {

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
            for (String parentId : self.getParentIds()) {
                Profile parent = version.getRequiredProfile(parentId);
                sb.append("-").append(parent.getProfileHash());
            }
            return sb.toString();
        }
    }

    @VisibleForExternal
    public void bindProfileRegistry(ProfileRegistry service) {
        this.profileRegistry.bind(service);
    }

    void unbindProfileRegistry(ProfileRegistry service) {
        this.profileRegistry.unbind(service);
    }

    @VisibleForExternal
    public void bindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.bind(service);
    }

    void unbindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.unbind(service);
    }
}
