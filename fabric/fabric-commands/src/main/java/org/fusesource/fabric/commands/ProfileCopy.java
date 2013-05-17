package org.fusesource.fabric.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.boot.commands.support.FabricCommand;

import java.util.Map;

import static org.fusesource.fabric.utils.FabricValidations.validateProfileName;

@Command(name = "profile-copy", scope = "fabric", description = "Copies the specified version of the source profile (where the version defaults to the current default version)")
public class ProfileCopy extends FabricCommand {

    @Option(name = "--version", description = "The profile version to copy. Defaults to the current default version.")
    private String version;

    @Option(name = "-f", aliases = "--force", description = "Flag to allow overwriting the target profile (if exists).")
    private boolean force;


    @Argument(index = 0, required = true, name = "source profile", description = "Name of the source profile.")
    @CompleterValues(index = 0)
    private String source;

    @Argument(index = 1, required = true, name = "target profile", description = "Name of the target profile.")
    @CompleterValues(index = 1)
    private String target;

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        validateProfileName(source);
        validateProfileName(target);
        Version ver = version != null ? fabricService.getVersion(version) : fabricService.getDefaultVersion();

        if (!ver.hasProfile(source)) {
            System.out.println("Source profile " + target + " not found.");
            return null;
        } else if (ver.hasProfile(target)){
            if (!force) {
                System.out.println("Target profile " + target + " already exists. Use --force if you want to overwrite.");
                return null;
            } else {

            }   ver.getProfile(target).delete();
        }

        for (Profile profile : ver.getProfiles()) {
            if (source.equals(profile.getId())) {
                Profile targetProfile = ver.createProfile(target);
                targetProfile.setParents(profile.getParents());
                targetProfile.setConfigurations(profile.getConfigurations());
                for (Map.Entry<String, String> entry : profile.getAttributes().entrySet()) {
                    targetProfile.setAttribute(entry.getKey(), entry.getValue());
                }
            }
        }
        return null;
    }

}