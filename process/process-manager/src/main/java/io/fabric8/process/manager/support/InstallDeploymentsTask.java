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
package io.fabric8.process.manager.support;

import io.fabric8.common.util.Files;
import io.fabric8.process.manager.InstallTask;
import io.fabric8.process.manager.config.ProcessConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Copies the set of deployments into the process
 */
public class InstallDeploymentsTask implements InstallTask {
    private static final transient Logger LOG = LoggerFactory.getLogger(InstallDeploymentsTask.class);

    private final Map<String, File> javaArtifacts;

    public InstallDeploymentsTask(Map<String, File> javaArtifacts) {
        this.javaArtifacts = javaArtifacts;
    }

    @Override
    public void install(ProcessConfig config, String id, File installDir) throws Exception {
        File baseDir = ProcessUtils.findInstallDir(installDir);
        String sharedLibraryPath = config.getSharedLibraryPath();
        String deployPath = config.getDeployPath();

        File sharedLibDir = new File(baseDir, sharedLibraryPath);
        File deployDir = new File(baseDir, deployPath);

        sharedLibDir.mkdirs();
        deployDir.mkdirs();

        SortedSet<String> sharedLibraries = new TreeSet<String>();
        SortedSet<String> deployments = new TreeSet<String>();

        Set<Map.Entry<String, File>> entries = javaArtifacts.entrySet();
        for (Map.Entry<String, File> entry : entries) {
            String uri = entry.getKey();
            File file = entry.getValue();

            String fileName = file.getName();
            File destDir;
            if (fileName.endsWith(".jar")) {
                destDir = sharedLibDir;
                sharedLibraries.add(fileName);
            }
            else {
                destDir = deployDir;
                deployments.add(fileName);
            }
            File destFile = new File(destDir, fileName);
            Files.copy(file, destFile);
        }

        LOG.info("Deployed " + deployments.size() + " deployment(s)");
        for (String deployment : deployments) {
            LOG.info("   deployed: " + deployment);
        }

        LOG.info("Installed " + sharedLibraries.size() + " shared jar(s)");
        for (String sharedLib : sharedLibraries) {
            LOG.info("   jar: " + sharedLib);
        }
    }
}
