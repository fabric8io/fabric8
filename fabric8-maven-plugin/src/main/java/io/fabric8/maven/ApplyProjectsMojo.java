/**
 * Copyright 2005-2016 Red Hat, Inc.
 * <p/>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.maven;

import io.fabric8.devops.ProjectConfig;
import io.fabric8.devops.ProjectRepositories;
import io.fabric8.devops.ProjectRepository;
import io.fabric8.devops.connector.DevOpsConnector;
import io.fabric8.devops.connector.DevOpsConnectors;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Ensures that the projects defined in the <code>projects.yaml</code> are populated into a kubernetes namespace
 *
 */
@Mojo(name = "apply-projects", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.INSTALL)
public class ApplyProjectsMojo extends AbstractNamespacedMojo {

    @Parameter(property = "fabric8.projectsFile", defaultValue = "${basedir}/src/main/fabric8/projects.yml")
    protected File projectsFile;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        KubernetesClient kubernetes = getKubernetes();

        List<ProjectRepository> projects;
        try {
            projects = ProjectRepositories.loadProjectRepositories(projectsFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to load projects YAML file: " + projectsFile + ". " + e, e);
        }

        for (ProjectRepository project : projects) {
            DevOpsConnector connector = DevOpsConnectors.createDevOpsConnector(project);
            String namespace = getNamespace();
            connector.setNamespace(namespace);

            ProjectConfig config = connector.getProjectConfig();
            String flow = config != null ? config.getPipeline() : null;

            getLog().info("Updating project " + project.getUser() + "/" + project.getRepoName() + " at " + project.getGitUrl() + " and flow " + flow + " in namespace: " + namespace);

            getLog().debug("Using connector: " + connector);

            try {
                connector.execute();
            } catch (Exception e) {
                getLog().error("Failed to update DevOps resources: " + e, e);
                throw new MojoExecutionException("Failed to update DevOps resources: " + e, e);
            }
        }
    }
}
