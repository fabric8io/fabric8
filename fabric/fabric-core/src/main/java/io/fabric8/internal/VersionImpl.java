/*
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

import io.fabric8.api.*;

import java.util.*;

public class VersionImpl implements Version {

    private final String id;
    private final FabricService service;
    private final VersionSequence sequence;

    public VersionImpl(String id, FabricService service) {
        this.id = id;
        this.service = service;
        this.sequence = new VersionSequence(id);
    }

    @Override
    public String getName() {
        return getId();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Map<String, String> getAttributes() {
        return service.getDataStore().getVersionAttributes(id);
    }

    @Override
    public void setAttribute(String key, String value) {
        service.getDataStore().setVersionAttribute(id, key, value);
    }

    @Override
    public VersionSequence getSequence() {
        return sequence;
    }

    @Override
    public int compareTo(Version that) {
        return this.sequence.compareTo(that.getSequence());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VersionImpl version = (VersionImpl) o;

        if (id != null ? !id.equals(version.id) : version.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public Version getDerivedFrom() {
        // TODO how to find the derived from???
        return null;
    }

    @Override
    public Profile[] getProfiles() {
        List<String> names = service.getDataStore().getProfiles(id);
        List<Profile> profiles = new ArrayList<Profile>();
        for (String name : names) {
            profiles.add(new ProfileImpl(name, id, service));
        }
        return profiles.toArray(new Profile[profiles.size()]);
    }

    @Override
    public Profile getProfile(String profileId) {
        if (service.getDataStore().hasProfile(id, profileId)) {
            return new ProfileImpl(profileId, id, service);
        }
        throw new FabricException("Profile '" + profileId + "' does not exist in version '" + id + "'.");
    }

    @Override
    public Profile createProfile(String profileId) {
        service.getDataStore().createProfile(id, profileId);
        return new ProfileImpl(profileId, id, service);
    }

    @Override
    public boolean hasProfile(String profileId) {
        return service.getDataStore().hasProfile(id, profileId);
    }

    @Override
    public void copyProfile(String sourceId, String targetId, boolean force) {

        maybeDeleteProfile(targetId, force);

        for (Profile profile : this.getProfiles()) {
            if (sourceId.equals(profile.getId())) {
                Profile targetProfile = this.createProfile(targetId);
                targetProfile.setParents(profile.getParents());
                targetProfile.setFileConfigurations(profile.getFileConfigurations());
                for (Map.Entry<String, String> entry : profile.getAttributes().entrySet()) {
                    targetProfile.setAttribute(entry.getKey(), entry.getValue());
                }
                return;
            }
        }
    }

    private void maybeDeleteProfile(String targetId, boolean force) {
        if (force && hasProfile(targetId)) {
            Profile p = this.getProfile(targetId);

            if (p != null) {
                p.delete(force);
            }
        }
    }

    @Override
    public void renameProfile(String profileId, String newId, boolean force) {

        maybeDeleteProfile(newId, force);

        for (Profile profile : this.getProfiles()) {
            if (profileId.equals(profile.getId())) {
                Profile targetProfile = this.createProfile(newId);
                targetProfile.setParents(profile.getParents());
                targetProfile.setConfigurations(profile.getConfigurations());
                for (Map.Entry<String, String> entry : profile.getAttributes().entrySet()) {
                    targetProfile.setAttribute(entry.getKey(), entry.getValue());
                }

                // TODO: what about child profiles ?

                for (Container container : profile.getAssociatedContainers()) {
                    Profile[] containerProfiles = container.getProfiles();
                    Set<Profile> profileSet = new HashSet<Profile>(Arrays.asList(containerProfiles));
                    profileSet.remove(profile);
                    profileSet.add(targetProfile);
                    container.setProfiles(profileSet.toArray(new Profile[profileSet.size()]));
                }
                maybeDeleteProfile(profileId, true);
                return;
            }
        }
    }


    @Override
    public void delete() {
        service.getDataStore().deleteVersion(id);
    }

    @Override
    public String toString() {
        // TODO: add attributes
        return id;
    }
}
