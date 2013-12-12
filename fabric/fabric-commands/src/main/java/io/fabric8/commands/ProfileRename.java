package io.fabric8.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.api.Version;
import io.fabric8.boot.commands.support.FabricCommand;

import static io.fabric8.utils.FabricValidations.validateProfileName;

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
            }
        }

        ver.renameProfile(profileName, newName, force);
        return null;
    }

}
