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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.fabric8.common.util.Files;
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
 * Generates a ZIP file of the profile configuration
 */
@Mojo(name = "zip", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.PACKAGE)
public class CreateProfileZipMojo extends AbstractProfileMojo {

    /**
     * Name of the directory used to create the profile configuration zip
     */
    @Parameter(property = "fabric8.zip.buildDir", defaultValue = "${project.build.directory}/generated-profiles")
    private File buildDir;

    /**
     * Name of the created profile zip file
     */
    @Parameter(property = "fabric8.zip.outFile", defaultValue = "${project.build.directory}/profile.zip")
    private File outputFile;

    @Component
    private MavenProjectHelper projectHelper;

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
     * Files to be excluded
     */
    @Parameter(property = "fabric8.excludedFiles", defaultValue = "io.fabric8.agent.properties")
    private String[] filesToBeExcluded;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            ProjectRequirements requirements = new ProjectRequirements();

            DependencyDTO rootDependency = null;
            if (!"pom".equals(project.getPackaging())) {
                rootDependency = loadRootDependency();
                requirements.setRootDependency(rootDependency);
            }
            configureRequirements(requirements);
            addProjectArtifactBundle(requirements);

            File profileBuildDir = createProfileBuildDir(requirements.getProfileId());

            boolean hasConfigDir = profileConfigDir.isDirectory();
            if (hasConfigDir) {
                copyProfileConfigFiles(profileBuildDir, profileConfigDir);
            } else {
                getLog().info("The profile configuration files directory " + profileConfigDir + " doesn't exist, so not copying any additional project documentation or configuration files");
            }

            // lets only generate a profile zip if  we have a requirement (e.g. we're not a parent pom packaging project) and
            // we have defined some configuration files or dependencies
            // to avoid generating dummy profiles for parent poms
            if (hasConfigDir || rootDependency != null ||
                    notEmpty(requirements.getBundles()) || notEmpty(requirements.getFeatures()) || notEmpty(requirements.getFeatureRepositories())) {
                generateFabricAgentProperties(requirements, new File(profileBuildDir, "io.fabric8.agent.properties"));

                Zips.createZipFile(getLog(), buildDir, outputFile);

                projectHelper.attachArtifact(project, artifactType, artifactClassifier, outputFile);

                String relativePath = Files.getRelativePath(project.getBasedir(), outputFile);
                while (relativePath.startsWith("/")) {
                    relativePath = relativePath.substring(1);
                }
                getLog().info("Created profile zip file: " + relativePath);
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Error executing", e);
        }
    }

    public static boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }


    /**
     * Copies any local configuration files into the profile directory
     */
    protected void copyProfileConfigFiles(File profileBuildDir, File profileConfigDir) throws IOException {

        File[] files = profileConfigDir.listFiles();

        if (files != null) {
            profileBuildDir.mkdirs();
            for (File file : files) {
                if (!toBeExclude(file.getName())) {
                    File outFile = new File(profileBuildDir, file.getName());
                    if (file.isDirectory()) {
                        copyProfileConfigFiles(file, outFile);
                    } else {
                        Files.copy(file, outFile);
                    }
                }
            }
        }

    }

    protected ArrayList<String> removePath(List<String> filesToBeExcluded) {
        ArrayList<String> fileName = new ArrayList<String>();
        for (String name : filesToBeExcluded) {
            int pos = name.lastIndexOf("/");
            if (pos > 0) {
                String fname = name.substring(0, pos);
                fileName.add(fname);
            }
        }
        return fileName;
    }

    protected boolean toBeExclude(String fileName) {
        List excludedFilesList = Arrays.asList(filesToBeExcluded);
        Boolean result = excludedFilesList.contains(fileName);
        return result;
    }

    /**
     * Returns the directory within the {@link #buildDir} to generate data for the profile
     */
    protected File createProfileBuildDir(String profileId) {
        String profilePath = profileId.replace('-', '/') + ".profile";
        return new File(buildDir, profilePath);
    }

    protected void generateFabricAgentProperties(ProjectRequirements requirements, File file) throws MojoExecutionException, IOException {
        file.getParentFile().mkdirs();

        PrintWriter writer = new PrintWriter(new FileWriter(file));
        try {
            String profileId = requirements.getProfileId();
            writer.println("# Profile: " +  profileId);
            writer.println("# generated by the fabric8 maven plugin at " + new Date());
            writer.println("# see: http://fabric8.io/#/site/book/doc/index.md?chapter=mavenPlugin_md");
            writer.println();
            List<String> parentProfiles = Zips.notNullList(requirements.getParentProfiles());
            if (!parentProfiles.isEmpty()) {
                writer.write("attribute.parents =");
                for (String parentProfile : parentProfiles) {
                    writer.write(" ");
                    writer.write(parentProfile);
                }
                writer.println();
                writer.println();
            }
            List<String> bundles = Zips.notNullList(requirements.getBundles());
            List<String> features = Zips.notNullList(requirements.getFeatures());
            List<String> repos = Zips.notNullList(requirements.getFeatureRepositories());
            for (String bundle : bundles) {
                if (Strings.isNotBlank(bundle)) {
                    writer.println("bundle." + escapeAgentPropertiesKey(bundle) + " = " + escapeAgentPropertiesValue(bundle));
                }
            }
            if (!bundles.isEmpty()) {
                writer.println();
            }
            for (String feature : features) {
                if (Strings.isNotBlank(feature)) {
                    writer.println("feature." + escapeAgentPropertiesKey(feature) + " = " + escapeAgentPropertiesValue(feature));
                }
            }
            if (!features.isEmpty()) {
                writer.println();
            }
            for (String repo : repos) {
                if (Strings.isNotBlank(repo)) {
                    writer.println("repository." + escapeAgentPropertiesKey(repo) + " = " + escapeAgentPropertiesValue(repo));
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

    protected String escapeAgentPropertiesKey(String text) {
        return text.replaceAll("\\:", "\\\\:");
    }

    protected String escapeAgentPropertiesValue(String text) {
        return escapeAgentPropertiesKey(text);
    }

}
