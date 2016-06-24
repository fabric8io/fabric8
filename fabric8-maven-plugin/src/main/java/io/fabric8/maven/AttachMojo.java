/*
 * Copyright 2005-2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.maven;

import java.io.File;
import java.io.IOException;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.utils.Files;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;

/**
 * Generates or copies the Kubernetes JSON file and attaches it to the build so its
 * installed and released to maven repositories like other build artifacts.
 */
@Mojo(name = "attach", defaultPhase = LifecyclePhase.PACKAGE)
public class AttachMojo extends AbstractFabric8Mojo {

    @Component
    private MavenProjectHelper projectHelper;

    /**
     * The artifact type for attaching the generated kubernetes json file to the project
     */
    @Parameter(property = "fabric8.kubernetes.artifactType", defaultValue = "json")
    private String artifactType = "json";

    /**
     * The artifact type for attaching the generated kubernetes YAML file to the project
     */
    @Parameter(property = "fabric8.kubernetes.yaml.artifactType", defaultValue = "yml")
    private String yamlArtifactType = "yml";

    /**
     * The artifact classifier for attaching the generated kubernetes json file to the project
     */
    @Parameter(property = "fabric8.kubernetes.artifactClassifier", defaultValue = "kubernetes")
    private String artifactClassifier = "kubernetes";

    /**
     * Should we generate YAML too?
     */
    @Parameter(property = "fabric8.generateYaml", defaultValue = "true")
    private boolean generateYaml = true;

    /**
     * The generated kubernetes YAML file
     */
    @Parameter(property = "fabric8.yaml.target", defaultValue = "${basedir}/target/classes/kubernetes.yml")
    private File kubernetesYaml;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File json = getKubernetesJson();
        if (Files.isFile(json)) {
            printSummary(json);

            getLog().info("Attaching kubernetes json file: " + json + " to the build");
            projectHelper.attachArtifact(getProject(), artifactType, artifactClassifier, json);

            if (generateYaml) {
                Object savedObjects = null;
                try {
                    savedObjects = KubernetesHelper.loadJson(json);
                } catch (IOException e) {
                    throw new MojoExecutionException("Failed to load saved json file " + json + ". Reason: " + e, e);
                }
                if (savedObjects != null) {
                    try {
                        KubernetesHelper.saveYaml(savedObjects, kubernetesYaml);
                    } catch (IOException e) {
                        throw new MojoExecutionException("Failed to save YAML file " + kubernetesYaml + ". Reason: " + e, e);
                    }
                }
                projectHelper.attachArtifact(getProject(), yamlArtifactType, artifactClassifier, kubernetesYaml);
            }
        }
    }

    protected void printSummary(File json) throws MojoExecutionException {
        try {
            Object savedObjects = KubernetesHelper.loadJson(json);
            getLog().info("Generated Kubernetes JSON resources:");
            printSummary(savedObjects);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to load saved json file " + json + ". Reason: " + e, e);
        }
    }

}
