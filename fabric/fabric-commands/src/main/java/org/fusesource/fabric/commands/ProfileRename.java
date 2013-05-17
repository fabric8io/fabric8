package org.fusesource.fabric.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.boot.commands.support.FabricCommand;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.fusesource.fabric.utils.FabricValidations.validateProfileName;

@Command(name = "profile-rename", scope = "fabric", description = "Rename the specified version of the source profile (where the version defaults to the current default version)")
public class ProfileRename extends FabricCommand {

    @Option(name = "--version", description = "The profile version to rename. Defaults to the current default version.")
    private String version;

    @Option(name = "-f", aliases = "--force", description = "Flag to allow replacing the target profile (if exists).")
    private boolean force;

    @Argument(index = 0, required = true, name = "profile", description = "Name of the profile.")
    @CompleterValues(index = 0)
    private String profileName;

    @Argument(index = 1, required = true, name = " profile", description = "New name of the profile.")
    @CompleterValues(index = 1)
    private String newName;

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        validateProfileName(profileName);
        validateProfileName(newName);
        Version ver = version != null ? fabricService.getVersion(version) : fabricService.getDefaultVersion();

        if (!ver.hasProfile(profileName)) {
            System.out.println("Profile " + newName + " not found.");
            return null;
        } else if (ver.hasProfile(newName)){
            if (!force) {
                System.out.println("New name " + newName + " already exists. Use --force if you want to overwrite.");
                return null;
            } else {

            }   ver.getProfile(newName).delete();
        }

        for (Profile profile : ver.getProfiles()) {
            if (profileName.equals(profile.getId())) {
                Profile targetProfile = ver.createProfile(newName);
                targetProfile.setParents(profile.getParents());
                targetProfile.setConfigurations(profile.getConfigurations());
                for (Map.Entry<String, String> entry : profile.getAttributes().entrySet()) {
                    targetProfile.setAttribute(entry.getKey(), entry.getValue());
                }

                for (Container container : profile.getAssociatedContainers()) {
                    Profile[] containerProfiles = container.getProfiles();
                    Set<Profile> profileSet = new HashSet<Profile>(Arrays.asList(containerProfiles));
                    profileSet.remove(profile);
                    profileSet.add(targetProfile);
                    container.setProfiles(profileSet.toArray(new Profile[profileSet.size()]));
                }
            }
        }
        return null;
    }

}