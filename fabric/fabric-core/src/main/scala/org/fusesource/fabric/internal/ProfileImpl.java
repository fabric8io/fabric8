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
package org.fusesource.fabric.internal;

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.FabricService;

import java.io.IOException;
import java.util.*;

public class ProfileImpl implements Profile {

    public static final String AGENT_PID = "org.fusesource.fabric.agent";

    private final String id;
    private final String version;
    private final FabricService service;

    public ProfileImpl(String id, String version, FabricService service) {
        this.id = id;
        this.version = version;
        this.service = service;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public Map<String, String> getAttributes() {
        return service.getDataStore().getProfileAttributes(version, id);
    }

    @Override
    public void setAttribute(String key, String value) {
        service.getDataStore().setProfileAttribute(version, id, key, value);
    }

    public FabricService getService() {
        return service;
    }

    //In some cases we need to sort profiles by Id.
    @Override
    public int compareTo(Profile profile) {
        return id.compareTo(profile.getId());
    }

    public enum ConfigListType {

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
            Properties containerProps = getContainerProperties(p);
            ArrayList<String> rc = new ArrayList<String>();
            for ( Map.Entry<Object, Object> e : containerProps.entrySet() ) {
                if ( ((String)e.getKey()).startsWith(type + ".") ) {
                    rc.add((String)e.getValue());
                }
            }
            return rc;

        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public static void setContainerConfigList(Profile p, List<String> values, ConfigListType type) {
        Map<String,Map<String, String>> config = p.getConfigurations();
        String prefix = type + ".";
        Map<String, String> map = config.get(AGENT_PID);
        if (map == null) {
            map = new HashMap<String, String>();
            config.put(AGENT_PID, map);
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

    public static Properties getContainerProperties(Profile p) throws IOException {
        byte[] b = p.getFileConfigurations().get(AGENT_PID + ".properties");
        if (b != null) {
            return DataStoreHelpers.toProperties(b);
        } else {
            return new Properties();
        }
    }

    public Profile[] getParents() {
        try {
            String str = getAttributes().get(PARENTS);
            if (str == null || str.isEmpty()) {
                return new Profile[0];
            }
            str = str.trim();
            List<Profile> profiles = new ArrayList<Profile>();
            for (String p : str.split(" ")) {
                profiles.add(new ProfileImpl(p, version, service));
            }
            return profiles.toArray(new Profile[profiles.size()]);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public void setParents(Profile[] parents) {
        if (parents == null) {
            setAttribute(PARENTS, null);
            return;
        } else if (isLocked()) {
            throw new UnsupportedOperationException("The profile " + id + " is locked and can not be modified");
        }

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
            throw new FabricException(e);
        }
    }

    public Container[] getAssociatedContainers() {
        try {
            ArrayList<Container> rc = new ArrayList<Container>();
            Container[] containers = service.getContainers();
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
            throw new FabricException(e);
        }
    }

    public boolean isOverlay() {
        return false;
    }

    public Profile getOverlay() {
        return new ProfileOverlayImpl(this);
    }

    @Override
    public Map<String, byte[]> getFileConfigurations() {
        return getService().getDataStore().getFileConfigurations(version, id);
    }

    @Override
    public void setFileConfigurations(Map<String, byte[]> configurations) {
        if (isLocked()) {
            throw new UnsupportedOperationException("The profile " + id + " is locked and can not be modified");
        }
        getService().getDataStore().setFileConfigurations(version, id, configurations);
    }

    public Map<String, Map<String, String>> getConfigurations() {
        return getService().getDataStore().getConfigurations(version, id);
    }

    @Override
    public Map<String, String> getContainerConfiguration() {
        Map<String, String> map = getConfigurations().get(AGENT_PID);
        if (map == null) {
            map = new HashMap<String, String>();
        }
        return map;
    }

    public void setConfigurations(Map<String, Map<String, String>> configurations) {
        if (isLocked()) {
            throw new UnsupportedOperationException("The profile " + id + " is locked and can not be modified");
        }
        getService().getDataStore().setConfigurations(version, id, configurations);
    }

    public void delete() {
        service.deleteProfile(this);
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
        ProfileOverlayImpl selfOverlay = new ProfileOverlayImpl(this);
        return selfOverlay.agentConfigurationEquals(other);
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
    public long getLastModified() {
        return getService().getDataStore().getLastModified(version, id);
    }

}
