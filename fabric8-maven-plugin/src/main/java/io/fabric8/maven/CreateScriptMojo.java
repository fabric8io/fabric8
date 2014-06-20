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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

import io.fabric8.common.util.Strings;
import io.fabric8.deployer.dto.DependencyDTO;
import io.fabric8.deployer.dto.ProjectRequirements;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;

/**
 * Generates a karaf shell script to create the profile
 */
@Mojo(name = "script", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.PACKAGE)
public class CreateScriptMojo extends AbstractProfileMojo {

    /**
     * Name of the created karaf shell script file
     */
    @Parameter(property = "fabric8.script.outFile", defaultValue = "${project.build.directory}/profile.karaf")
    private File outputFile;

    @Component
    private MavenProjectHelper projectHelper;


    /**
     * The artifact type for attaching the generated script to the project
     */
    @Parameter(property = "fabric8.script.artifactType", defaultValue = "karaf")
    private String artifactType = "karaf";

    /**
     * The artifact classifier for attaching the generated script to the project
     */
    @Parameter(property = "fabric8.script.artifactClassifier", defaultValue = "profile")
    private String artifactClassifier = "profile";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            DependencyDTO rootDependency = loadRootDependency();

            ProjectRequirements requirements = new ProjectRequirements();
            requirements.setRootDependency(rootDependency);
            configureRequirements(requirements);
            addProjectArtifactBundle(requirements);

            generateScript(requirements, outputFile);

            projectHelper.attachArtifact(project, artifactType, artifactClassifier, outputFile);
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Error executing", e);
        }
    }

    protected void generateScript(ProjectRequirements requirements, File file) throws MojoExecutionException, IOException {
        file.getParentFile().mkdirs();

        PrintWriter writer = new PrintWriter(new FileWriter(file));
        try {
            String profileId = requirements.getProfileId();
                writer.write("profile-create");
                List<String> parentProfiles = notNullList(requirements.getParentProfiles());
                    for (String parentProfile : parentProfiles) {
                        writer.write(" --parents ");
                        writer.write(parentProfile);
                    }
                writer.write(" ");
                writer.println(profileId);
            List<String> bundles = notNullList(requirements.getBundles());
            List<String> features = notNullList(requirements.getFeatures());
            List<String> repos = notNullList(requirements.getFeatureRepositories());
            for (String bundle : bundles) {
                if (Strings.isNotBlank(bundle)) {
                    writer.println("profile-edit --bundles " + bundle);
                }
            }
            for (String feature : features) {
                if (Strings.isNotBlank(feature)) {
                    writer.println("profile-edit --features " + feature);
                }
            }
            for (String repo : repos) {
                if (Strings.isNotBlank(repo)) {
                    writer.println("profile-edit --repositories " + repo);
                }
            }
        } finally {
            try {
                writer.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    protected static List<String> notNullList(List<String> list) {
        if (list == null) {
            return Collections.EMPTY_LIST;
        } else {
            return list;
        }
    }

}
