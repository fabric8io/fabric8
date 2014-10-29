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

import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.utils.Files;
import io.fabric8.deployer.JavaContainers;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Adds files from the profiles to the docker image in a flat folder structure; such as a shared lib or deploy directory
 */
public class FlatArtifactFilesAdder implements ArtifactFilesAdder {
    private FabricService fabric;
    private List<Profile> profileList;
    private ExecutorService downloadExecutor;
    private File uploadLibDir;
    private File uploadDeployDir;
    private int libFileCount;
    private int deployFileCount;
    private Set<String> artifactKeys;

    public FlatArtifactFilesAdder(FabricService fabric, List<Profile> profileList, ExecutorService downloadExecutor, File uploadLibDir, File uploadDeployDir) {
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
        Map<String, File> artifacts = JavaContainers.getJavaContainerArtifactsFiles(fabric, profileList, downloadExecutor);
        libFileCount = 0;
        deployFileCount = 0;
        Set<Map.Entry<String, File>> entries = artifacts.entrySet();
        for (Map.Entry<String, File> entry : entries) {
            File file = entry.getValue();
            String fileName = file.getName();
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
            Files.copy(file, new File(outputDir, fileName));
        }
        artifactKeys = artifacts.keySet();
        return this;
    }
}
