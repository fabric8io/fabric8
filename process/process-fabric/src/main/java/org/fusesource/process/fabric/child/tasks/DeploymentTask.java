package org.fusesource.process.fabric.child.tasks;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.codehaus.plexus.util.FileUtils;
import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.utils.AgentUtils;
import io.fabric8.api.Profile;
import io.fabric8.utils.features.FeatureUtils;
import org.fusesource.process.manager.InstallTask;
import org.fusesource.process.manager.config.ProcessConfig;
import org.fusesource.process.manager.support.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeploymentTask implements InstallTask {
    private static final transient Logger LOG = LoggerFactory.getLogger(DeploymentTask.class);

    private static String PAX_URL_MVN_PID = "org.ops4j.pax.url.mvn";

    private final DownloadManager downloadManager;
    private final Profile profile;


    public DeploymentTask(DownloadManager downloadManager, Profile profile) {
        this.downloadManager = downloadManager;
        this.profile = profile;
    }

    @Override

    public void install(ProcessConfig config, int id, File installDir) throws Exception {
        File baseDir = ProcessUtils.findInstallDir(installDir);
        Set<String> bundles = new LinkedHashSet<String>(profile.getBundles());
        Set<Feature> features = getFeatures(profile);
        LOG.info("Deploying into external container features " + features + " and bundles " + bundles);
        Map<String, File> files = AgentUtils.downloadBundles(downloadManager, features, bundles,
                Collections.<String>emptySet());
        Set<Map.Entry<String, File>> entries = files.entrySet();
        for (Map.Entry<String, File> entry : entries) {
            String name = entry.getKey();
            File file = entry.getValue();
            String destPath;
            String fileName = file.getName();
            if (name.startsWith("war:") || name.contains("/war/") || fileName.toLowerCase()
                    .endsWith(".war")) {
                destPath = config.getDeployPath();
            } else {
                destPath = config.getSharedLibraryPath();
            }

            File destDir = new File(baseDir, destPath);
            File destFile = new File(destDir, fileName);
            LOG.debug("Copying file " + fileName + " to :  " + destFile.getCanonicalPath());
            FileUtils.copyFile(file, destFile);
        }
    }

    public Set<Feature> getFeatures(Profile p) throws Exception {
        Set<Feature> features = new LinkedHashSet<Feature>();
        AgentUtils.addFeatures(features, downloadManager, p);
        return features;
    }
}
