/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.arquillian.kubernetes;

import io.fabric8.utils.Zips;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static io.fabric8.arquillian.kubernetes.Constants.DEFAULT_CONFIG_FILE_NAME;

public class DependencyResolver {

    public static final String DEFAULT_PATH_TO_POM = "pom.xml";

    private final String pathToPomFile;
    private final boolean rethrowExcpetions;

    public DependencyResolver() {
        this(DEFAULT_PATH_TO_POM, false);
    }

    //Mostly needed for testing
    public DependencyResolver(String pathToPomFile, boolean rethrowExcpetions) {
        this.pathToPomFile = pathToPomFile;
        this.rethrowExcpetions = rethrowExcpetions;
    }

    public List<String> resolve(Session session) throws IOException {
        List<String> dependencies = new ArrayList<>();
        try {
            File[] files = Maven.resolver().loadPomFromFile(pathToPomFile).importTestDependencies().resolve().withoutTransitivity().asFile();
            for (File f : files) {
                if (f.getName().endsWith("jar") && hasKubernetesJson(f)) {
                    Path dir = Files.createTempDirectory(session.getId());
                    try (FileInputStream fis = new FileInputStream(f); JarInputStream jis = new JarInputStream(fis)) {
                        Zips.unzip(new FileInputStream(f), dir.toFile());
                        File jsonPath = dir.resolve(DEFAULT_CONFIG_FILE_NAME).toFile();
                        if (jsonPath.exists()) {
                            dependencies.add(jsonPath.toURI().toString());
                        }
                    }
                } else if (f.getName().endsWith(".json")) {
                    dependencies.add(f.toURI().toString());
                }
            }
        } catch (Exception e) {
            if (rethrowExcpetions) {
                throw e;
            } else {
                session.getLogger().warn("Skipping maven project dependencies. Caused by:" + e.getMessage());
            }
        }
        return dependencies;
    }


    private boolean hasKubernetesJson(File f) throws IOException {
        try (FileInputStream fis = new FileInputStream(f); JarInputStream jis = new JarInputStream(fis)) {
            for (JarEntry entry = jis.getNextJarEntry(); entry != null; entry = jis.getNextJarEntry()) {
                if (entry.getName().equals(DEFAULT_CONFIG_FILE_NAME)) {
                    return true;
                }
            }
        }
        return false;
    }
}
