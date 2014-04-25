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
package io.fabric8.commands.support;

import io.fabric8.api.Container;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;

import java.util.ArrayList;
import java.util.List;

public final class ContainerUpgradeSupport {

    /**
     * Gets the profiles for upgrade/rollback
     *
     * @param existingProfiles  the existing profiles
     * @param targetVersion     the target version
     * @return the new profiles to be used
     */
    public static Profile[] getProfilesForUpgradeOrRollback(Profile[] existingProfiles, Version targetVersion) {
        List<Profile> list = new ArrayList<Profile>(existingProfiles.length);
        for (Profile old : existingProfiles) {
            // get new profile
            Profile newProfile = targetVersion.getProfile(old.getId());
            if (newProfile != null) {
                list.add(newProfile);
            } else {
                // we expect a profile with the new version to exist
                throw new IllegalArgumentException("Profile " + old.getId() + " with version " + targetVersion + " does not exists");
            }
        }

        return list.toArray(new Profile[0]);
    }

    /**
     * Compare the version with the container
     *
     * @param version   the version to rollback to
     * @param container the container
     * @return <tt>-1</tt> if cannot rollback, <tt>0</tt> if same version, or <tt>1</tt> if can rollback
     */
    public static int canUpgrade(Version version, Container container) {
        Version current = container.getVersion();
        return version.compareTo(current);
    }

    /**
     * Compare the version with the container
     *
     * @param version   the version to rollback to
     * @param container the container
     * @return <tt>-1</tt> if cannot rollback, <tt>0</tt> if same version, or <tt>1</tt> if can rollback
     */
    public static int canRollback(Version version, Container container) {
        // reverse login than canUpgrade so * -1
        return canUpgrade(version, container) * -1;
    }

}
