package io.fabric8.commands.support;

import java.util.ArrayList;
import java.util.List;

import io.fabric8.api.Container;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.boot.commands.support.FabricCommand;

public abstract class ContainerUpgradeSupport extends FabricCommand {

    /**
     * Gets the profiles for upgrade/rollback
     *
     * @param existingProfiles  the existing profiles
     * @param targetVersion     the target version
     * @return the new profiles to be used
     */
    protected Profile[] getProfilesForUpgradeOrRollback(Profile[] existingProfiles, Version targetVersion) {
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
    protected int canUpgrade(Version version, Container container) {
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
    protected int canRollback(Version version, Container container) {
        // reverse login than canUpgrade so * -1
        return canUpgrade(version, container) * -1;
    }

}