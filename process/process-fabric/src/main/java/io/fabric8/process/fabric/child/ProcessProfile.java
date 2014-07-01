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
package io.fabric8.process.fabric.child;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.internal.ProfileImpl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ProcessProfile extends ProfileImpl {

    private final Container container;
    private final ProcessRequirements requirements;
    private final boolean includeContainerProfile;

    public ProcessProfile(Container container, ProcessRequirements requirements, FabricService fabricService,
                          boolean includeContainerProfile) {
        super(requirements.getId(), container.getVersion().getId(), fabricService);
        this.container = container;
        this.requirements = requirements;
        this.includeContainerProfile = includeContainerProfile;
    }

    @Override
    public String getVersion() {
        return container.getVersion().getId();
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
    public Profile[] getParents() {
        List<String> parents = requirements.getProfiles();
        List<Profile> profiles = new LinkedList<Profile>();
        if (includeContainerProfile) {
            profiles.add(container.getOverlayProfile());
        }
        for (String parent : parents) {
            Profile p = container.getVersion().getProfile(parent);
            profiles.add(p);
        }
        return profiles.toArray(new Profile[profiles.size()]);
    }

    @Override
    public void setParents(Profile[] parents) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setConfigurations(Map<String, Map<String, String>> configurations) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOverlay() {
        return false;
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBundles(List<String> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFabs(List<String> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFeatures(List<String> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRepositories(List<String> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOverrides(List<String> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOptionals(List<String> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean configurationEquals(Profile other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean agentConfigurationEquals(Profile other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAbstract() {
        return true;
    }

    @Override
    public boolean isLocked() {
        return true;
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    @Override
    public String getId() {
        return "process-profile-" + requirements.getId();
    }
}
