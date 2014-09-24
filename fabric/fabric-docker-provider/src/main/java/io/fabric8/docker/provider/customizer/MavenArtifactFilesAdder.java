/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.docker.provider.customizer;

import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.download.DownloadManagers;
import io.fabric8.maven.util.Parser;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Profiles;
import io.fabric8.common.util.Files;
import io.fabric8.deployer.JavaContainers;
import io.fabric8.service.VersionPropertyPointerResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Adds files from the profiles to the docker image in a flat folder structure; such as a shared lib or deploy directory
 */
public class MavenArtifactFilesAdder implements ArtifactFilesAdder {
    private static final transient Logger LOG = LoggerFactory.getLogger(MavenArtifactFilesAdder.class);

    private FabricService fabric;
    private List<Profile> profileList;
    private ExecutorService downloadExecutor;
    private File uploadLibDir;
    private File uploadDeployDir;
    private int libFileCount;
    private int deployFileCount;
    private Set<String> artifactKeys;

    public MavenArtifactFilesAdder(FabricService fabric, List<Profile> profileList, ExecutorService downloadExecutor, File uploadLibDir, File uploadDeployDir) {
        this.fabric = fabric;
        this.profileList = profileList;
        this.downloadExecutor = downloadExecutor;
        this.uploadLibDir = uploadLibDir;
        this.uploadDeployDir = uploadDeployDir;
    }

    @Override
    public int getLibFileCount() {
        return libFileCount;
    }

    @Override
    public int getDeployFileCount() {
        return deployFileCount;
    }

    @Override
    public Set<String> getArtifactKeys() {
        return artifactKeys;
    }

    @Override
    public ArtifactFilesAdder invoke() throws Exception {
        DownloadManager downloadManager = DownloadManagers.createDownloadManager(fabric, downloadExecutor);
        Map<String, Parser> artifacts = JavaContainers.getJavaContainerArtifacts(fabric, profileList, downloadManager);
        libFileCount = 0;
        deployFileCount = 0;
        Set<Map.Entry<String, Parser>> entries = artifacts.entrySet();
        Set<String> rawUrls = artifacts.keySet();
        Map<String, File> files = new HashMap<>();
        JavaContainers.downloadArtifactUrls(downloadManager, rawUrls, files);
        for (Map.Entry<String, Parser> entry : entries) {
            String uri = entry.getKey();
            if (uri.startsWith("fab:")) {
                uri = uri.substring(4);
            }
            Parser parser = entry.getValue();
            File file = files.get(uri);
            if (file == null) {
                LOG.warn("Could not find file for " + uri + " in files map when has parser: " + parser);
                continue;
            }
            String fileName = parser.getArtifactPath();
            File outputDir;
            if (fileName.toLowerCase().endsWith(".jar")) {
                outputDir = uploadLibDir;
                libFileCount++;
            }
            else {
                outputDir = uploadDeployDir;
                deployFileCount++;
            }
            outputDir.mkdirs();
            File outFile = new File(outputDir, fileName);
            outFile.getParentFile().mkdirs();
            Files.copy(file, outFile);
        }

        // lets make sure there's all the feature files too
        Set<String> processedFeatureXmls = new HashSet<>();
        for (Profile profile : profileList) {
            List<String> repositories = profile.getRepositories();
            for (String repository : repositories) {
                if (processedFeatureXmls.add(repository)) {
                    if (repository.contains("$")) {
                        Map<String, Map<String, String>> configurations = Profiles.getOverlayConfigurations(fabric, profileList);
                        repository = VersionPropertyPointerResolver.replaceVersions(fabric, configurations, repository);
                    }
                    Parser parser = null;
                    URL url = null;
                    try {
                        parser = Parser.parsePathWithSchemePrefix(repository);
                    } catch (MalformedURLException e) {
                        LOG.warn("Could not parse maven coords in features repository: " + repository + ". " + e, e);
                    }
                    try {
                        url = new URL(repository);
                    } catch (MalformedURLException e) {
                        LOG.warn("Could not parse URL for feature repository: " + repository + ". " + e, e);
                    }
                    if (parser != null && url != null) {
                        InputStream inputStream = null;
                        try {
                            inputStream = url.openStream();
                            if (inputStream == null) {
                                LOG.warn("Could not load URL for feature repository: " + repository);
                                continue;
                            }
                        } catch (IOException e) {
                            LOG.warn("Could not load URL for feature repository: " + repository + ". " + e, e);
                            continue;
                        }
                        String fileName = parser.getArtifactPath();
                        uploadLibDir.mkdirs();
                        File outFile = new File(uploadLibDir, fileName);
                        outFile.getParentFile().mkdirs();
                        try {
                            Files.copy(inputStream, new FileOutputStream(outFile));
                        } catch (FileNotFoundException e) {
                            LOG.warn("Failed to copy stream to " + outFile);
                        }
                    }
                }
            }
        }
        artifactKeys = artifacts.keySet();
        return this;
    }
}
