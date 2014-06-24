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
package io.fabric8.agent.commands;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.fabric8.agent.download.ProfileDownloader;
import io.fabric8.agent.download.ProfileDownloaderListener;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = ProfileDownloadArtifacts.FUNCTION_VALUE, scope = ProfileDownloadArtifacts.SCOPE_VALUE, description = ProfileDownloadArtifacts.DESCRIPTION)
public class ProfileDownloadArtifactsAction extends AbstractAction {

    @Option(name = "--version", description = "The profile version to download. Defaults to the current default version.")
    private String version;

    @Option(name = "--profile", description = "The profile to download. Defaults to all profiles in the selected version.")
    private String profile;

    @Option(name = "-f", aliases = "--force", description = "Flag to allow overwriting of files already in the target directory")
    private boolean force;

    @Option(name = "-v", aliases = "--verbose", description = "Flag to turn off verbose mode which prints more details during the download process")
    private boolean verbose = true;

    @Option(name = "-s", aliases = "--stop", description = "Flag to either stop on first failure or continue downloading")
    private boolean stopOnFailure;

    @Option(name = "-t", aliases = "--threads", description = "The number of threads to use for the download manager. Defaults to 1.")
    private int threadPoolSize;

    @Argument(index = 0, required = false, name = "target directory", description = "The directory to download files to. Defaults to the system folder.")
    @CompleterValues(index = 0)
    private File target;

    private final FabricService fabricService;
    private ExecutorService executorService;

    ProfileDownloadArtifactsAction(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    @Override
    protected Object doExecute() throws Exception {
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

        ProfileDownloader downloader = new ProfileDownloader(fabricService, target, force, executorService);
        downloader.setStopOnFailure(stopOnFailure);
        // we do not want to download the files from within the profile itself, only the dependencies
        downloader.setDownloadFilesFromProfile(false);
        if (verbose) {
            downloader.setListener(new ProgressIndicator());
        }
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

    private final class ProgressIndicator implements ProfileDownloaderListener {

        @Override
        public void beforeDownloadProfiles(Profile[] profiles) {
            if (profiles != null) {
                System.out.println(profiles.length + " profiles to download");
            }
        }

        @Override
        public void afterDownloadProfiles(Profile[] profiles) {
            // force a println when we are done
            System.out.println(""); System.out.flush();
        }

        @Override
        public void beforeDownloadProfile(Profile profile) {
            System.out.print('.'); System.out.flush();
        }

        @Override
        public void afterDownloadProfile(Profile profile) {
            // noop
        }

        @Override
        public void onCopyDone(Profile profile, File file) {
            // noop
        }

        @Override
        public void onError(Profile profile, Exception e) {
            // noop
        }
    }
}
