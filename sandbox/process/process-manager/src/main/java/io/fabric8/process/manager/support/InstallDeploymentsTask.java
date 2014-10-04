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

import io.fabric8.common.util.ChecksumUtils;
import io.fabric8.common.util.FileChangeInfo;
import io.fabric8.process.manager.InstallContext;
import io.fabric8.process.manager.InstallTask;
import io.fabric8.process.manager.config.ProcessConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashSet;
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
    private final Map<String, String> locationToContextPathMap;

    public InstallDeploymentsTask(Map<String, File> javaArtifacts, Map<String, String> locationToContextPathMap) {
        this.javaArtifacts = javaArtifacts;
        this.locationToContextPathMap = locationToContextPathMap;
    }

    @Override
    public void install(InstallContext installContext, ProcessConfig config, String id, File installDir) throws Exception {
        File baseDir = ProcessUtils.findInstallDir(installDir);
        String sharedLibraryPath = config.getSharedLibraryPath();
        String deployPath = config.getDeployPath();

        File libraryDir = new File(baseDir, sharedLibraryPath);
        File deployDir = new File(baseDir, deployPath);
        libraryDir.mkdirs();
        deployDir.mkdirs();

        Map<File, Long> deployChecksums = ChecksumUtils.loadInstalledChecksumCache(deployDir);
        Map<File, Long> libraryChecksums = ChecksumUtils.loadInstalledChecksumCache(libraryDir);

        SortedSet<String> sharedLibraries = new TreeSet<String>();
        SortedSet<String> deployments = new TreeSet<String>();

        Set<File> installedFiles = new HashSet<File>();
        Set<Map.Entry<String, File>> entries = javaArtifacts.entrySet();

        Set<File> filesToDelete = new HashSet<File>();
        filesToDelete.addAll(deployChecksums.keySet());
        filesToDelete.addAll(libraryChecksums.keySet());

        // lets delete any old files and update the cached checksums first before we install any files
        // so we can properly clean down any old files installed if we fail at any point
        for (int i = 0; i < 2; i++) {
            boolean deletePass = i == 0;
            for (Map.Entry<String, File> entry : entries) {
                String location = entry.getKey();
                File file = entry.getValue();
                String fileName = file.getName();
                File destDir;
                Map<File, Long> checksums;
                boolean isSharedLibrary = fileName.endsWith(".jar");
                if (isSharedLibrary) {
                    checksums = libraryChecksums;
                    destDir = libraryDir;
                } else {
                    destDir = deployDir;
                    checksums = deployChecksums;
                }
                String destFileName = fileName;
                if (destFileName.endsWith(".war")) {
                    String value = locationToContextPathMap.get(location);
                    if (value != null) {
                        while (value.startsWith("/")) {
                            value = value.substring(1);
                        }
                        if (value.length() == 0) {
                            value = "ROOT";
                        }
                        destFileName = value + ".war";
                    }
                }
                File destFile = new File(destDir, destFileName);
                Long checksum = checksums.get(destFile);
                if (deletePass) {
                    // on the delete pass we just keep track of all checksums
                    // and files we are installing so we can delete and update the checksum files
                    filesToDelete.remove(destFile);
                    if (checksum == null) {
                        // lets use the source file for the checksum so we can update the cached file
                        // before we perform any copy operations
                        checksum = ChecksumUtils.checksumFile(file);
                        checksums.put(destFile, checksum);
                    }
                } else {
                    // we can't use the 'checksum' value as its using the source file not the destFile
                    FileChangeInfo changeInfo = installContext.createChangeInfo(destFile);
                    LOG.debug("Copying file " + fileName + " to :  " + destFile.getCanonicalPath());
                    org.codehaus.plexus.util.FileUtils.copyFile(file, destFile);
                    installContext.onDeploymentFileWrite(location, destFile, changeInfo, isSharedLibrary);
                }
            }
            if (deletePass) {
                // lets delete all the files that were in the cache file
                // that are not being installed
                for (File fileToDelete : filesToDelete) {
                    LOG.info("Removing: " + fileToDelete);
                    deployChecksums.remove(fileToDelete);
                    libraryChecksums.remove(fileToDelete);
                    installContext.addRestartReason(fileToDelete);
                    fileToDelete.delete();
                }

                // now lets update the checksums on disk before we start writing any new files
                // so that if we fail after this point we can properly clean up any new files we've added
                ChecksumUtils.saveInstalledChecksumCache(deployDir, deployChecksums);
                ChecksumUtils.saveInstalledChecksumCache(libraryDir, libraryChecksums);
            }
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
