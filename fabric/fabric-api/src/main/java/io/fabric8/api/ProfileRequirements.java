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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Represents the requirements to successfully provision a profile such as the minimum instances required
 * and which other profiles should be profiled before hand.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileRequirements implements Comparable<ProfileRequirements> {
    private String profile;
    private Integer minimumInstances;
    private Integer maximumInstances;
    private List<String> dependentProfiles;
    private ChildScalingRequirements childScalingRequirements;
    private SshScalingRequirements sshScalingRequirements;
    private DockerScalingRequirements dockerScalingRequirements;
    private OpenShiftScalingRequirements openShiftScalingRequirements;
    private Integer maximumInstancesPerHost;

    public ProfileRequirements() {
    }

    public ProfileRequirements(String profile) {
        this.profile = profile;
    }

    public ProfileRequirements(String profile, Integer minimumInstances) {
        this(profile);
        this.minimumInstances = minimumInstances;
    }

    public ProfileRequirements(String profile, Integer minimumInstances, Integer maximumInstances) {
        this(profile, minimumInstances);
        this.maximumInstances = maximumInstances;
    }

    public ProfileRequirements(String profile, Integer minimumInstances, Integer maximumInstances, List<String> dependentProfiles) {
        this(profile, minimumInstances, maximumInstances);
        this.dependentProfiles = dependentProfiles;
    }

    public ProfileRequirements(String profile, Integer minimumInstances, Integer maximumInstances, String... dependentProfiles) {
        this(profile, minimumInstances, maximumInstances);
        this.dependentProfiles = new ArrayList<String>(Arrays.asList(dependentProfiles));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProfileRequirements that = (ProfileRequirements) o;

        if (!profile.equals(that.profile)) return false;

        return true;
    }

    @Override
    public int compareTo(ProfileRequirements o) {
        return this.profile.compareTo(o.profile);
    }

    @Override
    public int hashCode() {
        return profile.hashCode();
    }

    @Override
    public String toString() {
        return "ProfileRequirements[" + profile + " " + getOrBlank(minimumInstances) + ".." + getOrBlank(maximumInstances) + "]";

    }

    private static String getOrBlank(Object value) {
        return value != null ? value.toString() : "";
    }


    /**
     * Checks that the configuation of these requirements are valid
     */
    public void validate() {
        if (maximumInstances != null) {
            if (maximumInstances < 0) {
                throw new IllegalArgumentException("Maximum instances should be >= 0");
            }
            if (minimumInstances != null) {
                if (minimumInstances > maximumInstances) {
                    throw new IllegalArgumentException("Minimum instances must not be greater than the maximum instances");
                }
            }
        }
        if (minimumInstances != null) {
            if (minimumInstances < 0) {
                throw new IllegalArgumentException("Minimum instances should be >= 0");
            }
        }
    }

    // Builder DSL
    //-------------------------------------------------------------------------
    public ProfileRequirements dependentProfiles(List<String> profiles) {
        setDependentProfiles(profiles);
        return this;
    }

    public ProfileRequirements dependentProfiles(String... profiles) {
        return dependentProfiles(Arrays.asList(profiles));
    }

    public ProfileRequirements minimumInstances(Integer value) {
        setMinimumInstances(value);
        return this;
    }

    public ProfileRequirements maximumInstances(Integer value) {
        setMaximumInstances(value);
        return this;
    }

    /**
     * Lazily creates the scaling requirements for the child container provider
     */
    public ChildScalingRequirements childScaling() {
        if (childScalingRequirements == null) {
            childScalingRequirements = new ChildScalingRequirements();
        }
        return getChildScalingRequirements();
    }

    /**
     * Lazily creates the scaling requirements for the ssh container provider
     */
    public SshScalingRequirements sshScaling() {
        if (sshScalingRequirements == null) {
            sshScalingRequirements = new SshScalingRequirements();
        }
        return getSshScalingRequirements();
    }

    /**
     * Lazily creates the scaling requirements for the docker container provider
     */
    public DockerScalingRequirements dockerScaling() {
        if (dockerScalingRequirements == null) {
            dockerScalingRequirements = new DockerScalingRequirements();
        }
        return getDockerScalingRequirements();
    }

    /**
     * Lazily creates the scaling requirements for the OpenShift container provider
     */
    public OpenShiftScalingRequirements openShiftScaling() {
        if (openShiftScalingRequirements == null) {
            openShiftScalingRequirements = new OpenShiftScalingRequirements();
        }
        return getOpenShiftScalingRequirements();
    }

    /**
     * Specifies the maximum number of instances of this profile per host. e.g. set to 1 to ensure that only 1 instance of a profile is provisioned per host
     */
    public ProfileRequirements maximumInstancesPerHost(final Integer maximumInstancesPerHost) {
        this.maximumInstancesPerHost = maximumInstancesPerHost;
        return this;
    }


    // Properties
    //-------------------------------------------------------------------------

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public List<String> getDependentProfiles() {
        return dependentProfiles;
    }

    public void setDependentProfiles(List<String> dependentProfiles) {
        this.dependentProfiles = dependentProfiles;
    }

    public Integer getMaximumInstances() {
        return maximumInstances;
    }

    public void setMaximumInstances(Integer maximumInstances) {
        this.maximumInstances = maximumInstances;
    }

    public Integer getMinimumInstances() {
        return minimumInstances;
    }

    public void setMinimumInstances(Integer minimumInstances) {
        this.minimumInstances = minimumInstances;
    }

    public ChildScalingRequirements getChildScalingRequirements() {
        return childScalingRequirements;
    }

    public void setChildScalingRequirements(ChildScalingRequirements childScalingRequirements) {
        this.childScalingRequirements = childScalingRequirements;
    }

    public SshScalingRequirements getSshScalingRequirements() {
        return sshScalingRequirements;
    }

    public void setSshScalingRequirements(SshScalingRequirements sshScalingRequirements) {
        this.sshScalingRequirements = sshScalingRequirements;
    }

    public DockerScalingRequirements getDockerScalingRequirements() {
        return dockerScalingRequirements;
    }

    public void setDockerScalingRequirements(DockerScalingRequirements dockerScalingRequirements) {
        this.dockerScalingRequirements = dockerScalingRequirements;
    }

    public OpenShiftScalingRequirements getOpenShiftScalingRequirements() {
        return openShiftScalingRequirements;
    }

    public void setOpenShiftScalingRequirements(OpenShiftScalingRequirements openShiftScalingRequirements) {
        this.openShiftScalingRequirements = openShiftScalingRequirements;
    }


    public Integer getMaximumInstancesPerHost() {
        return maximumInstancesPerHost;
    }

    public void setMaximumInstancesPerHost(Integer maximumInstancesPerHost) {
        this.maximumInstancesPerHost = maximumInstancesPerHost;
    }

    /**
     * Returns the health as a percentage for tools based on the current number of instances and the requirements
     */
    public double getHealth(int instances) {
        if (instances <= 0) {
            return 0.0;
        }
        if (minimumInstances != null) {
            int min = minimumInstances.intValue();
            if (min <= 0) {
                return 1.0;
            } else {
                return 1.0 * instances / min;
            }
        }
        // if no minimum assume fine?
        return 1.0;
    }

    /**
     * Returns true if these requirements are empty (and so do not need to be persisted)
     */
    //@JsonIgnore
    // name this differently so it's not picked up as a property
    public boolean checkIsEmpty() {
        // we allow 0 maximum instances as being non-empty so we can keep the requirements around to
        // stop things
        return isEmpty(minimumInstances) && isEmpty(dependentProfiles) && maximumInstances == null;
    }

    protected static boolean isEmpty(Integer number) {
        return number == null || number == 0;
    }

    protected static boolean isEmpty(Collection<?> coll) {
        return coll == null || coll.isEmpty();
    }

    /**
     * Returns true if this profile requirements has at least 1 {@link #getMinimumInstances()}
     */
    public boolean hasMinimumInstances() {
        return minimumInstances != null && minimumInstances.intValue() > 0;
    }

}
