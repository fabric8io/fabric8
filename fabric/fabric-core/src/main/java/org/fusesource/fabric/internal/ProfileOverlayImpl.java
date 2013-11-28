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
import org.fusesource.fabric.api.Constants;
import org.fusesource.fabric.api.DataStore;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Profiles;
import org.fusesource.fabric.utils.DataStoreUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.fusesource.fabric.internal.ProfileImpl.ConfigListType;
import static org.fusesource.fabric.internal.ProfileImpl.getContainerConfigList;

public class ProfileOverlayImpl implements Profile {

    private final Profile self;
    private final boolean substitute;
    private final DataStore dataStore;
    private final String environment;

    public ProfileOverlayImpl(Profile self, String environment) {
        this(self, false, null, environment);
    }

    public ProfileOverlayImpl(Profile self, boolean substitute, String environment) {
        this(self, substitute, null, environment);
    }

    public ProfileOverlayImpl(Profile self, boolean substitute, DataStore dataStore, String environment) {
        this.self = self;
        this.substitute = substitute;
        this.dataStore = dataStore;
        this.environment = environment;
    }

    @Override
    public String getId() {
        return self.getId();
    }

    @Override
    public String getVersion() {
        return self.getVersion();
    }

    @Override
    public Map<String, String> getAttributes() {
        return self.getAttributes();
    }

    @Override
    public void setAttribute(String key, String value) {
        throw new UnsupportedOperationException("Overlay profiles are read-only.");
    }

    @Override
    public Profile[] getParents() {
        return self.getParents();
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
    public Container[] getAssociatedContainers() {
        return self.getAssociatedContainers();
    }

    @Override
    public Map<String, String> getContainerConfiguration() {
        Map<String, String> map = getConfigurations().get(Constants.AGENT_PID);
        if (map == null) {
            map = new HashMap<String, String>();
        }
        return map;
    }

    @Override
    public void setFileConfigurations(Map<String, byte[]> configurations) {
        throw new UnsupportedOperationException("Overlay profiles are read-only.");
    }

    @Override
    public void setParents(Profile[] parents) {
        throw new UnsupportedOperationException("Overlay profiles are read-only.");
    }

    @Override
    public void setConfigurations(Map<String, Map<String, String>> configurations) {
        throw new UnsupportedOperationException("Overlay profiles are read-only.");
    }

    @Override
    public void setConfiguration(String pid, Map<String, String> configuration) {
        Map<String, Map<String, String>> configurations = getConfigurations();
        configurations.put(pid, configuration);
        setConfigurations(configurations);
    }

    @Override
    public void setBundles(List<String> values) {
        throw new UnsupportedOperationException("Overlay profiles are read-only.");
    }

    @Override
    public void setFabs(List<String> values) {
        throw new UnsupportedOperationException("Overlay profiles are read-only.");
    }

    @Override
    public void setFeatures(List<String> values) {
        throw new UnsupportedOperationException("Overlay profiles are read-only.");
    }

    @Override
    public void setRepositories(List<String> values) {
        throw new UnsupportedOperationException("Overlay profiles are read-only.");
    }

    @Override
    public void setOverrides(List<String> values) {
        throw new UnsupportedOperationException("Overlay profiles are read-only.");
    }

    @Override
    public boolean configurationEquals(Profile other) {
        return self.configurationEquals(other);
    }

    /**
     * Checks if the two Profiles share the same agent configuration.
     *
     * @param other
     * @return
     */
    @Override
    public boolean agentConfigurationEquals(Profile other) {
        ProfileOverlayImpl otherOverlay = new ProfileOverlayImpl(other, environment);
        if (!getConfigurations().containsKey(Constants.AGENT_PID) && !otherOverlay.getConfigurations().containsKey(Constants.AGENT_PID)) {
            return true;
        } else if (getConfigurations().containsKey(Constants.AGENT_PID) != otherOverlay.getConfigurations().containsKey(Constants.AGENT_PID)) {
            return false;
        } else if (getConfigurations().containsKey(Constants.AGENT_PID) && !getConfigurations().get(Constants.AGENT_PID).equals(otherOverlay.getConfigurations().get(Constants.AGENT_PID))) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean exists() {
        return false;
    }

    public void delete() {
        throw new UnsupportedOperationException("Can not delete an overlay profile");
    }

    @Override
    public void delete(boolean force) {
        throw new UnsupportedOperationException("Can not delete an overlay profile");
    }

    @Override
    public Profile getOverlay() {
        return this;
    }

    @Override
    public Profile getOverlay(boolean substitute) {
        return new ProfileOverlayImpl(this.self, substitute, environment);
    }

    @Override
    public boolean isOverlay() {
        return true;
    }

    @Override
    public int compareTo(Profile profile) {
        return self.compareTo(profile);
    }

    private static class SupplementControl {
        byte[] data;
        Properties props;
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
        if (profile instanceof ProfileOverlayImpl) {
            if (((ProfileOverlayImpl) profile).self.equals(self)) {
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
                    if (childMap.remove(DELETED) != null) {
                        ctrl.props.clear();
                    }

                    // Update the entries...
                    for (Map.Entry<Object, Object> p : childMap.entrySet()) {
                        if (DELETED.equals(p.getValue())) {
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

    @Override
    public List<String> getConfigurationFileNames() {
        return Profiles.getConfigurationFileNames(getInheritedProfiles());
    }


    @Override
    public Map<String, byte[]> getFileConfigurations() {
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

    @Override
    public Map<String, Map<String, String>> getConfigurations() {
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
            if (substitute && dataStore != null) {
                dataStore.substituteConfigurations(rc);
            }
            return rc;
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public Map<String, String> getConfiguration(String pid) {
        return getConfigurations().get(pid);
    }

    @Override
    public boolean isAbstract() {
        return self.isAbstract();
    }

    @Override
    public boolean isLocked() {
        return self.isLocked();
    }

    @Override
    public boolean isHidden() {
        return self.isHidden();
    }

    /**
     * Returns the time in milliseconds of the last modification of the profile.
     */
    @Override
    public String getProfileHash() {
        StringBuffer sb = new StringBuffer();
        sb.append(self.getProfileHash());
        for (Profile parent : getParents()) {
            Profile parentOverlay = parent.getOverlay();
            sb.append("-").append(parentOverlay.getProfileHash());
        }
        return sb.toString();
    }
}
