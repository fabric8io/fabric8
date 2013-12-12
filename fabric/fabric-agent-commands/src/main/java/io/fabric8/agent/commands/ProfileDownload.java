package io.fabric8.agent.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.agent.download.ProfileDownloader;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.boot.commands.support.FabricCommand;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Command(name = "profile-download", scope = "fabric", description = "Downloads all of the bundles, features and fabs from a version or profile to a directory to make an offline maven repository.")
public class ProfileDownload extends FabricCommand {

    @Option(name = "--version", description = "The profile version to download. Defaults to the current default version.")
    private String version;

    @Option(name = "--profile", description = "The profile to download. Defaults to all profiles in the selected version.")
    private String profile;

    @Option(name = "-f", aliases = "--force", description = "Flag to allow overwriting of files already in the target directory")
    private boolean force;

    @Option(name = "-t", aliases = "--threads", description = "The number of threads to use for the download manager. Defaults to 1")
    private int threadPoolSize;

    @Argument(index = 0, required = false, name = "target directory", description = "The directory to download files to. Defaults to the system folder")
    @CompleterValues(index = 0)
    private File target;

    private ExecutorService executorService;

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();

        Version ver = version != null ? fabricService.getVersion(version) : fabricService.getDefaultVersion();
        if (ver == null) {
            if (version != null) {
                System.out.println("version " + version + " does not exist!");
            } else {
                System.out.println("No default version available!");
            }
            return null;
        }

        if (target == null) {
            String karafBase = System.getProperty("karaf.base", ".");
            target = new File(karafBase + "/system");
        }
        target.mkdirs();
        if (!target.exists()) {
            System.out.println("Could not create the target directory " + target);
            return null;
        }
        if (!target.isDirectory()) {
            System.out.println("Target is not a directory " + target);
            return null;
        }

        if (executorService == null) {
            if (threadPoolSize > 1) {
                executorService = Executors.newFixedThreadPool(threadPoolSize);
            } else {
                executorService = Executors.newSingleThreadExecutor();
            }
        }

        ProfileDownloader downloader = new ProfileDownloader(getFabricService(), target, force, executorService);
        if (profile != null) {
            Profile profileObject = null;
            if (ver.hasProfile(profile)) {
                profileObject = ver.getProfile(profile);
            }
            if (profileObject == null) {
                System.out.println("Source profile " + profile + " not found.");
                return null;
            }
            downloader.downloadProfile(profileObject);
        } else {
            downloader.downloadVersion(ver);
        }
        List<String> failedProfileIDs = downloader.getFailedProfileIDs();
        System.out.println("Downloaded " + downloader.getProcessedFileCount() + " file(s) to " + target);
        if (failedProfileIDs.size() > 0) {
            System.out.println("Failed to download these profiles: " + failedProfileIDs + ". Check the logs for details");
        }
        return null;
    }
}
