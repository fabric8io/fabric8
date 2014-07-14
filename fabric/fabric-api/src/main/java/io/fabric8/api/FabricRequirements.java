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
package io.fabric8.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Allows the requirements of a profile to be defined so that we can do automatic provisioning,
 * can ensure that required services stay running and can provide health checks
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FabricRequirements {
    private List<ProfileRequirements> profileRequirements = new ArrayList<ProfileRequirements>();
    private String version;
    private SshHostsConfiguration sshConfiguration;

    public FabricRequirements() {
    }

    public FabricRequirements(List<ProfileRequirements> profileRequirements) {
        this();
        this.profileRequirements = profileRequirements;
        sortProfilesRequirements();
    }


    @Override
    public String toString() {
        return "FabricRequirements" + profileRequirements;
    }

    public List<ProfileRequirements> getProfileRequirements() {
        return profileRequirements;
    }

    public void setProfileRequirements(List<ProfileRequirements> profileRequirements) {
        this.profileRequirements = profileRequirements;
        sortProfilesRequirements();
    }

    /**
     * Returns the current version for the fabric which the requirements apply to (usually the latest version,
     * as scaling requirements typically are independent of rolling upgrades and versioning).
     */
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public SshHostsConfiguration getSshConfiguration() {
        return sshConfiguration;
    }

    public void setSshConfiguration(SshHostsConfiguration sshConfiguration) {
        this.sshConfiguration = sshConfiguration;
    }

    @JsonIgnore
    public Map<String, SshHostConfiguration> getSshHostsMap() {
        if (sshConfiguration != null) {
            return sshConfiguration.getHosts();
        } else {
            return null;
        }
    }

    /**
     * Returns or creates a new {@link ProfileRequirements} for the given profile id
     */
    public ProfileRequirements getOrCreateProfileRequirement(String profile) {
        ProfileRequirements requirements = findProfileRequirements(profile);
        if (requirements == null) {
            requirements = new ProfileRequirements(profile);
            profileRequirements.add(requirements);
        }
        return requirements;
    }

    public ProfileRequirements findProfileRequirements(String profile) {
        for (ProfileRequirements profileRequirement : profileRequirements) {
            if (profile.equals(profileRequirement.getProfile())) {
                return profileRequirement;
            }
        }
        return null;
    }

    public boolean removeProfileRequirements(String profile) {
        ProfileRequirements requirements = findProfileRequirements(profile);
        if (requirements != null) {
            profileRequirements.remove(requirements);
            return true;
        }
        return false;
    }

    public void addOrUpdateProfileRequirements(ProfileRequirements requirement) {
        removeProfileRequirements(requirement.getProfile());
        profileRequirements.add(requirement);
        sortProfilesRequirements();
    }

    protected void sortProfilesRequirements() {
        Collections.sort(profileRequirements);
    }

    /**
     * Removes all the empty requirements; usually used just before saving to JSON
     */
    public void removeEmptyRequirements() {
        Iterator<ProfileRequirements> iterator = profileRequirements.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().checkIsEmpty()) {
                iterator.remove();
            }
        }
    }

    /**
     * Returns true if there are any requirements for the given profile ID and it has at least 1 minimum instances defined
     */
    public boolean hasMinimumInstances(String profileId) {
        ProfileRequirements profileRequirement = findProfileRequirements(profileId);
        if (profileRequirement != null) {
            return profileRequirement.hasMinimumInstances();
        }
        return false;
    }



    // Fluid API to make constructing requirements easier
    //-------------------------------------------------------------------------

    /**
     * Looks up and lazily creates if required a SSH host configuration for the given host alias.
     * The host name will be defaulted to the same hostAlias value for cases when the alias is the same as the actual host name
     */
    public SshHostConfiguration sshHost(String hostAlias) {
        SshHostsConfiguration config = getSshConfiguration();
        if (config == null) {
            config = new SshHostsConfiguration();
            setSshConfiguration(config);
        }
        Map<String, SshHostConfiguration> hosts = config.getHosts();
        if (hosts == null) {
            hosts = new HashMap<String, SshHostConfiguration>();
            config.setHosts(hosts);
        }
        SshHostConfiguration answer = hosts.get(hostAlias);
        if (answer == null) {
            answer = new SshHostConfiguration(hostAlias);
            hosts.put(hostAlias, answer);
        }
        return answer;
    }

    /**
     * Returns the requirements for the given profile; lazily creating requirements if none exist yet.
     */
    public ProfileRequirements profile(String profileId) {
        return getOrCreateProfileRequirement(profileId);
    }

    /**
     * Returns the ssh configuration; lazily creating one if it does not exist yet
     */
    public SshHostsConfiguration sshConfiguration() {
        SshHostsConfiguration answer = getSshConfiguration();
        if (answer == null) {
            answer = new SshHostsConfiguration();
            setSshConfiguration(answer);
        }
        return answer;
    }
}
