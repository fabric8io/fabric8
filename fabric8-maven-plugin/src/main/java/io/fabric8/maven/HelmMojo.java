/*
 * Copyright 2005-2015 Red Hat, Inc.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.maven.helm.Chart;
import io.fabric8.utils.Files;
import io.fabric8.utils.Strings;
import org.apache.maven.model.Developer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates a Helm chart for the kubernetes.yml file
 */
@Mojo(name = "helm", defaultPhase = LifecyclePhase.PACKAGE)
public class HelmMojo extends AbstractFabric8Mojo {

    // TODO when Helm supports this we should probably switch to ".yml"
    public static final String HELM_YAML_EXTENSION = ".yaml";
    public static final String PROPERTY_HELM_GIT_URL = "fabric8.helm.gitUrl";
    public static final String PROPERTY_HELM_CHART_NAME = "fabric8.helm.chartName";

    @Component
    private MavenProjectHelper projectHelper;

    /**
     * The artifact type for attaching the generated kubernetes json file to the project
     */
    @Parameter(property = PROPERTY_HELM_GIT_URL, defaultValue = "json")
    private String helmGitUrl;

    /**
     * The kubernetes YAML file
     */
    @Parameter(property = "fabric8.yaml.target", defaultValue = "${basedir}/target/classes/kubernetes.yml")
    private File kubernetesYaml;

    /**
     * The kubernetes YAML file
     */
    @Parameter(property = "fabric8.helm.cloneDir")
    private File helmCloneDir;

    /**
     * The kubernetes YAML file
     */
    @Parameter(property = PROPERTY_HELM_CHART_NAME, defaultValue = "${project.artifactId}")
    private String chartName;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File yaml = getKubernetesYaml();
        if (Files.isFile(yaml)) {
            getLog().info("Creating Helm Chart for kubernetes yaml file: " + yaml);

            File outputDir = getOutputDir();
            if (outputDir != null) {
                File manifestsDir = new File(outputDir, "manifests");
                manifestsDir.mkdirs();
                File outputYamlFile = new File(manifestsDir, chartName + HELM_YAML_EXTENSION);
                try {
                    Files.copy(yaml, outputYamlFile);
                } catch (IOException e) {
                    throw new MojoExecutionException("Failed to copy file " + yaml + " to chart manifest file: " + outputYamlFile + ". Reason: " + e, e);
                }

                File outputChartFile = new File(outputDir, "Chart" + HELM_YAML_EXTENSION);
                Chart chart = createChart();
                try {
                    KubernetesHelper.saveYaml(chart, outputChartFile);
                } catch (IOException e) {
                    throw new MojoExecutionException("Failed to save chart " + outputChartFile + ". Reason: " + e, e);
                }
                getLog().info("Generated Helm Chart " + chartName + " at " + outputDir);
            }
        }
    }

    protected Chart createChart() {
        Chart answer = new Chart();
        answer.setName(chartName);
        MavenProject project = getProject();
        if (project != null) {
            answer.setVersion(project.getVersion());
            answer.setDescription(project.getDescription());
            answer.setHome(project.getUrl());
            List<Developer> developers = project.getDevelopers();
            if (developers != null) {
                List<String> maintainers = new ArrayList<>();
                for (Developer developer : developers) {
                    String email = developer.getEmail();
                    String name = developer.getName();
                    String text = Strings.defaultIfEmpty(name, "");
                    if (Strings.isNotBlank(email)) {
                        if (Strings.isNotBlank(text)) {
                            text = text + " <" + email + ">";
                        } else {
                            text = email;
                        }
                    }
                    if (Strings.isNotBlank(text)) {
                        maintainers.add(text);
                    }
                }
                answer.setMaintainers(maintainers);
            }
        }
        return answer;
    }

    protected File getOutputDir() throws MojoExecutionException {
        if (helmCloneDir == null) {
            File rootProjectFolder = getRootProjectFolder();
            if (rootProjectFolder != null) {
                helmCloneDir = new File(rootProjectFolder, "target/helm-repo");
            }
        }

        if (helmCloneDir == null) {
            return null;
        }
        if (Strings.isNullOrBlank(helmGitUrl)) {
            getLog().warn("No git url so cannot clone a Helm repository. Please specify the `" + PROPERTY_HELM_GIT_URL + "` property");
        } else {
            // TODO clone the git repo
        }
        if (Strings.isNullOrBlank(chartName)) {
            throw new MojoExecutionException("No Chart name defined! Please specify the `" + PROPERTY_HELM_CHART_NAME + "` property");
        }
        return new File(helmCloneDir, chartName);
    }


    public File getKubernetesYaml() {
        return kubernetesYaml;
    }
}
