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
package io.fabric8.process.fabric.child.tasks;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import io.fabric8.api.FabricService;
import org.apache.karaf.features.Feature;
import org.codehaus.plexus.util.FileUtils;
import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.utils.AgentUtils;
import io.fabric8.api.Profile;
import io.fabric8.process.manager.InstallTask;
import io.fabric8.process.manager.config.ProcessConfig;
import io.fabric8.process.manager.support.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeploymentTask implements InstallTask {
    private static final transient Logger LOG = LoggerFactory.getLogger(DeploymentTask.class);

    private static String PAX_URL_MVN_PID = "org.ops4j.pax.url.mvn";

    private final DownloadManager downloadManager;
    private final Profile profile;
    private final FabricService fabricService;


    public DeploymentTask(DownloadManager downloadManager, Profile profile, FabricService fabricService) {
        this.downloadManager = downloadManager;
        this.profile = profile;
        this.fabricService = fabricService;
    }

    @Override
    public void install(ProcessConfig config, String id, File installDir) throws Exception {
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
        AgentUtils.addFeatures(features, fabricService, downloadManager, p);
        return features;
    }
}
