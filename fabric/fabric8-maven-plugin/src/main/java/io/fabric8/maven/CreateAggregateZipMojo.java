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
package io.fabric8.maven;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;


/**
 * Generates an aggregated ZIP file of all the profiles across all projects in a multi-maven project.
 */
@Mojo(name = "aggregate-zip", aggregator = true)
@Execute(goal = "zip")
public class CreateAggregateZipMojo extends AbstractProfileMojo {

    @Component()
    private MavenProjectHelper projectHelper;

    /**
     * Name of the directory used to create the profile zip files in each reactor project
     */
    @Parameter(property = "fabric8.fullzip.reactorProjectOutputPath", defaultValue = "target/generated-profiles")
    private String reactorProjectOutputPath;

    /**
     * Name of the directory used to create the full profiles configuration zip
     */
    @Parameter(property = "fabric8.zip.buildDir", defaultValue = "${project.build.directory}/generated-profiles")
    private File buildDir;

    /**
     * Name of the created profile zip file
     */
    @Parameter(property = "fabric8.fullzip.outFile", defaultValue = "${project.build.directory}/profile.zip")
    private File outputFile;

    /**
     * The artifact type for attaching the generated profile zip file to the project
     */
    @Parameter(property = "fabric8.zip.artifactType", defaultValue = "zip")
    private String artifactType = "zip";

    /**
     * The artifact classifier for attaching the generated profile zip file to the project
     */
    @Parameter(property = "fabric8.zip.artifactClassifier", defaultValue = "profile")
    private String artifactClassifier = "profile";

    /**
     * The projects in the reactor.
     */
    @Parameter(defaultValue = "${reactorProjects}")
    private List<MavenProject> reactorProjects;

    /**
     * Whether we should execute the fabric8:zip goal on each reactor project if there is not a
     * profile zip directory already
     */
    @Parameter(property = "fabric8.zip.invokeZipOnEachProject", defaultValue = "true")
    private boolean invokeZipOnEachProject;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (isIgnore()) return;

            List<MavenProject> reactorProjectList = reactorProjects;
            File projectBuildDir = buildDir;
            if (!project.isExecutionRoot()) {
                getLog().info("Not the execution root so ignoring this project");
                return;
            } else {
                getLog().info("Execution root directory so about to aggregate the reactor " + reactorProjectList.size() + " project(s) into " + projectBuildDir);
            }
            File projectOutputFile = outputFile;
            File projectBaseDir = project.getBasedir();
            String reactorProjectOutputPath1 = reactorProjectOutputPath;

            createAggregatedZip(reactorProjectList, projectBaseDir, projectBuildDir, reactorProjectOutputPath1, projectOutputFile);

            projectHelper.attachArtifact(project, artifactType, artifactClassifier, projectOutputFile);
        } catch (Exception e) {
            getLog().error(e);
            throw new MojoExecutionException("Error executing", e);
        }
    }


}
