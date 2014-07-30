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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;

/**
 * Represents the configuration of a Profile dependency that can be met through either profile wildcards or tags to define it.
 *
 * @author Ken Finnigan
 */
@Component(name = "io.fabric8.profile.dependency", label = "Fabric8 Profile Dependency Configuration", immediate = true, metatype = true)
public class ProfileDependencyConfig {

    /**
     * The PID for the profile dependency configuration properties.
     */
    public static final String PROFILE_DEPENDENCY_CONFIG_PID = "io.fabric8.profile.dependency";

    @Property(label = "Dependency Type", options = {
                @PropertyOption(name = "Zookeeper Service", value = "ZOOKEEPER_SERVICE")},
            description = "Type of profile dependency this represents.")
    private ProfileDependencyKind kind;

    @Property(label = "ZooKeeper Path", description = "Path in ZooKeeper under which an entry for this profile should be searched.")
    private String zookeeperPath;

    @Property(label = "Dependency Summary", description = "Describes what this dependency requires to be running for the Profile.")
    private String summary;

    @Property(label = "Profile Wildcards", cardinality = Integer.MAX_VALUE,
            description = "Partial or full Profile names that specify which Profiles would meet this dependency requirement.")
    private String[] profileWildcards;

    @Property(label = "Profile Tags", cardinality = Integer.MAX_VALUE,
            description = "Tags to define the type of Profile that would meet this dependency requirement.")
    private String[] profileTags;

    public ProfileDependencyKind getKind() {
        return kind;
    }

    public void setKind(ProfileDependencyKind kind) {
        this.kind = kind;
    }

    public String getZookeeperPath() {
        return zookeeperPath;
    }

    public void setZookeeperPath(String zookeeperPath) {
        this.zookeeperPath = zookeeperPath;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String[] getProfileWildcards() {
        return profileWildcards;
    }

    public void setProfileWildcards(String[] profileWildcards) {
        this.profileWildcards = profileWildcards;
    }

    public String[] getProfileTags() {
        return profileTags;
    }

    public void setProfileTags(String[] profileTags) {
        this.profileTags = profileTags;
    }

}
