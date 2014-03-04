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
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricRequirements;
import io.fabric8.api.Profile;
import io.fabric8.api.FabricService;
import io.fabric8.api.Version;

import java.io.IOException;
import java.util.*;

public class ProfileImpl implements Profile {

    private final String id;
    private final String version;
    private final FabricService fabricService;

    public ProfileImpl(String id, String version, FabricService service) {
        this.id = id;
        this.version = version;
        this.fabricService = service;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public Map<String, String> getAttributes() {
        return fabricService.getDataStore().getProfileAttributes(version, id);
    }

    @Override
    public void setAttribute(String key, String value) {
        fabricService.getDataStore().setProfileAttribute(version, id, key, value);
    }

    //In some cases we need to sort profiles by Id.
    @Override
    public int compareTo(Profile profile) {
        return id.compareTo(profile.getId());
    }

    public enum ConfigListType {
        LIBRARIES("lib"),
        ENDORSED("endorsed"),
        EXTENSION("extension"),
        BUNDLES("bundle"),
        FABS("fab"),
        FEATURES("feature"),
        REPOSITORIES("repository"),
        OVERRIDES("override");

        private String value;

        private ConfigListType(String value) {
            this.value = value;
        }
        public String toString() {
            return value;
        }
    }

    @Override
    public List<String> getLibraries() {
        return getContainerConfigList(this, ConfigListType.LIBRARIES);
    }

    @Override
    public List<String> getEndorsedLibraries() {
        return getContainerConfigList(this, ConfigListType.ENDORSED);
    }

    @Override
    public List<String> getExtensionLibraries() {
        return getContainerConfigList(this, ConfigListType.EXTENSION);
    }

    public List<String> getBundles() {
        return getContainerConfigList(this, ConfigListType.BUNDLES);
    }

    public List<String> getFabs() {
        return getContainerConfigList(this, ConfigListType.FABS);
    }

    public List<String> getFeatures() {
        return getContainerConfigList(this, ConfigListType.FEATURES);
    }

    public List<String> getRepositories() {
        return getContainerConfigList(this, ConfigListType.REPOSITORIES);
    }

    @Override
    public List<String> getOverrides() {
        return getContainerConfigList(this, ConfigListType.OVERRIDES);
    }

    @Override
    public void setBundles(List<String> values) {
        setContainerConfigList(this, values, ConfigListType.BUNDLES);
    }

    @Override
    public void setFabs(List<String> values) {
        setContainerConfigList(this, values, ConfigListType.FABS);
    }

    @Override
    public void setFeatures(List<String> values) {
        setContainerConfigList(this, values, ConfigListType.FEATURES);
    }

    @Override
    public void setRepositories(List<String> values) {
        setContainerConfigList(this, values, ConfigListType.REPOSITORIES);
    }

    @Override
    public void setOverrides(List<String> values) {
        setContainerConfigList(this, values, ConfigListType.OVERRIDES);
    }

    public static List<String> getContainerConfigList(Profile p, ConfigListType type) {
        try {
            Map<String, String> containerProps = p.getContainerConfiguration();
            ArrayList<String> rc = new ArrayList<String>();
            String prefix = type + ".";
            for ( Map.Entry<String, String> e : containerProps.entrySet() ) {
                if ( (e.getKey()).startsWith(prefix) ) {
                    rc.add(e.getValue());
                }
            }
            return rc;

        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    public static void setContainerConfigList(Profile p, List<String> values, ConfigListType type) {
        Map<String,Map<String, String>> config = p.getConfigurations();
        String prefix = type + ".";
        Map<String, String> map = config.get(Constants.AGENT_PID);
        if (map == null) {
            map = new HashMap<String, String>();
            config.put(Constants.AGENT_PID, map);
        } else {
            List<String> keys = new ArrayList<String>(map.keySet());
            for (String key : keys) {
                if (key.startsWith(prefix)) {
                    map.remove(key);
                }
            }
        }
        for (String value : values) {
            map.put(prefix + value, value);
        }
        p.setConfigurations(config);
    }

    public Profile[] getParents() {
        try {
            String str = getAttributes().get(PARENTS);
            if (str == null || str.isEmpty()) {
                return new Profile[0];
            }
            str = str.trim();
            List<Profile> profiles = new ArrayList<Profile>();
            Version v = fabricService.getVersion(version);
            for (String p : str.split(" ")) {
                profiles.add(v.getProfile(p));
            }
            return profiles.toArray(new Profile[profiles.size()]);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    public void setParents(Profile[] parents) {
        if (parents == null) {
            setAttribute(PARENTS, null);
            return;
        } else assertNotLocked();

        try {
            StringBuilder sb = new StringBuilder();
            for (Profile parent : parents) {
                if (!version.equals(parent.getVersion())) {
                    throw new IllegalArgumentException("Version mismatch setting parent profile " + parent + " with version "
                            + parent.getVersion() + " expected version " + version);
                }
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(parent.getId());
            }
            setAttribute(PARENTS, sb.toString());
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    public Container[] getAssociatedContainers() {
        try {
            ArrayList<Container> rc = new ArrayList<Container>();
            Container[] containers = fabricService.getContainers();
            for (Container container : containers) {
                if (!container.getVersion().getId().equals(getVersion())) {
                    continue;
                }
                for (Profile p : container.getProfiles()) {
                    if (this.equals(p)) {
                        rc.add(container);
                        break;
                    }
                }
            }
            return rc.toArray(new Container[0]);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    public boolean isOverlay() {
        return false;
    }

    public Profile getOverlay() {
        return new ProfileOverlayImpl(this, fabricService.getEnvironment());
    }

    public Profile getOverlay(boolean substitute) {
        return new ProfileOverlayImpl(this, fabricService.getEnvironment(), substitute, fabricService);
    }

    @Override
    public Map<String, byte[]> getFileConfigurations() {
        return fabricService.getDataStore().getFileConfigurations(version, id);
    }

    @Override
    public List<String> getConfigurationFileNames() {
        return fabricService.getDataStore().getConfigurationFileNames(version, id);
    }

    @Override
    public byte[] getFileConfiguration(String fileName) {
        return fabricService.getDataStore().getFileConfiguration(version, id, fileName);
    }

    @Override
    public void setFileConfigurations(Map<String, byte[]> configurations) {
        assertNotLocked();
        fabricService.getDataStore().setFileConfigurations(version, id, configurations);
    }

    public Map<String, Map<String, String>> getConfigurations() {
        return fabricService.getDataStore().getConfigurations(version, id);
    }

    @Override
    public Map<String, String> getConfiguration(String pid) {
        return fabricService.getDataStore().getConfiguration(version, id, pid);
    }

    @Override
    public Map<String, String> getContainerConfiguration() {
        Map<String, String> map = getConfigurations().get(Constants.AGENT_PID);
        if (map == null) {
            map = new HashMap<String, String>();
        }
        return map;
    }

    public void setConfigurations(Map<String, Map<String, String>> configurations) {
        assertNotLocked();
        fabricService.getDataStore().setConfigurations(version, id, configurations);
    }

    @Override
    public void setConfiguration(String pid, Map<String, String> configuration) {
        assertNotLocked();
        fabricService.getDataStore().setConfiguration(version, id, pid, configuration);
    }

    public void refresh() {
        Map<String, Map<String, String>> configuration = this.getConfigurations();
        Map<String, String> agentConfiguration = configuration.get(Constants.AGENT_PID);
        if (agentConfiguration == null) {
            agentConfiguration = new HashMap<String, String>();
        }
        agentConfiguration.put("lastRefresh." + id, String.valueOf(System.currentTimeMillis()));
        this.setConfigurations(configuration);
    }

    public void delete() {
        delete(false);
    }

    public void delete(boolean force) {
        // TODO: what about child profiles ?
        Container[] containers = getAssociatedContainers();
        if (containers.length == 0) {
            fabricService.getDataStore().deleteProfile(version, id);
        } else if (force) {
            for (Container container : containers) {
                container.removeProfiles(this);
            }
            fabricService.getDataStore().deleteProfile(version, id);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Cannot delete profile:").append(id).append(".");
            sb.append("Profile has assigned ").append(containers.length).append(" container(s):");
            for (Container c :containers) {
                sb.append(" ").append(c.getId());
            }
            sb.append(". Use force option to also remove the profile from the containers.");
            throw new FabricException(sb.toString());
        }

        // lets remove any pending requirements on this profile
        FabricRequirements requirements = fabricService.getRequirements();
        if (requirements.removeProfileRequirements(id)) {
            try {
                fabricService.setRequirements(requirements);
            } catch (IOException e) {
                throw new FabricException("Failed to update requirements after deleting profile " + id + ". " + e, e);
            }
        }
    }

    public boolean configurationEquals(Profile other) {
         Profile[] parents = getParents();
         Profile[] otherParents = other.getParents();
         Arrays.sort(parents);
         Arrays.sort(otherParents);
         if (!getConfigurations().equals(other.getConfigurations())) {
             return false;
         }
         if (parents.length != otherParents.length) {
             return false;
         }

         for (int i = 0; i < parents.length; i++) {
             if (!parents[i].configurationEquals(otherParents[i])) {
                 return false;
             }
         }
         return true;
    }

    /**
     * Checks of the agent configuration of the current {@link Profile} matcher the other {@link Profile}.
     * @param other
     * @return
     */
    public boolean agentConfigurationEquals(Profile other) {
        ProfileOverlayImpl selfOverlay = new ProfileOverlayImpl(this, fabricService.getEnvironment());
        return selfOverlay.agentConfigurationEquals(other);
    }

    @Override
    public boolean exists() {
        return fabricService.getVersion(version).hasProfile(id);
    }

    @Override
    public String toString() {
        return "ProfileImpl[" +
                "id='" + id + '\'' +
                ", version='" + version + '\'' +
                ']';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProfileImpl profile = (ProfileImpl) o;
        if (!id.equals(profile.id)) return false;
        if (!version.equals(profile.version)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    @Override
    public boolean isAbstract() {
        return Boolean.parseBoolean(getAttributes().get(ABSTRACT));
    }

    @Override
    public boolean isLocked() {
        return Boolean.parseBoolean(getAttributes().get(LOCKED));
    }

    @Override
	public boolean isHidden() {
		return Boolean.parseBoolean(getAttributes().get(HIDDEN));
	}

    /**
     * Returns the time in milliseconds of the last modification of the profile.
     */
    @Override
    public String getProfileHash() {
        return fabricService.getDataStore().getLastModified(version, id);
    }

    protected void assertNotLocked() {
        if (isLocked()) {
            throw new UnsupportedOperationException("The profile " + id + " is locked and can not be modified");
        }
    }
}
