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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import io.fabric8.common.util.Files;
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

            if (!project.isExecutionRoot()) {
                getLog().info("Not the execution root so ignoring this project");
                return;
            } else {
                getLog().info("Execution root directory so about to aggregate the reactor " + reactorProjects.size() + " project(s) into " + buildDir);
            }
            buildDir.mkdirs();

            for (MavenProject reactorProject : reactorProjects) {
                // ignore the execution root which just aggregates stuff
                if (!reactorProject.isExecutionRoot()) {
                    combineProfilesTo(reactorProject, buildDir);
                }
            }
            Zips.createZipFile(getLog(), buildDir, outputFile);

            projectHelper.attachArtifact(project, artifactType, artifactClassifier, outputFile);

            String relativePath = Files.getRelativePath(project.getBasedir(), outputFile);
            while (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            getLog().info("Created profile zip file: " + relativePath);
        } catch (MojoExecutionException e) {
            getLog().error(e);
            throw e;
        } catch (Exception e) {
            getLog().error(e);
            throw new MojoExecutionException("Error executing", e);
        }
    }

    protected void combineProfilesTo(MavenProject reactorProject, File buildDir) throws IOException, MojoExecutionException {
        File basedir = reactorProject.getBasedir();
        if (!basedir.exists()) {
            getLog().warn("No basedir " + basedir.getAbsolutePath() + " for project + " + reactorProject);
            return;
        }
        File outDir = new File(basedir, reactorProjectOutputPath);
        if (!outDir.exists()) {
            getLog().warn("No profile output dir at: " + outDir.getAbsolutePath() + " for project + " + reactorProject + " so ignoring this project.");
            return;
        }
        getLog().info("Copying profiles from " + outDir.getAbsolutePath() + " into the output directory: " + buildDir);
        appendProfileConfigFiles(outDir, buildDir);
    }


    /**
     * Combines any files from the profileSourceDir into the output directory
     */
    public static void appendProfileConfigFiles(File profileSourceDir, File outputDir) throws IOException {
        if (profileSourceDir.exists() && profileSourceDir.isDirectory()) {
            File[] files = profileSourceDir.listFiles();
            if (files != null) {
                outputDir.mkdirs();
                for (File file : files) {
                    File outFile = new File(outputDir, file.getName());
                    if (file.isDirectory()) {
                        appendProfileConfigFiles(file, outFile);
                    } else {
                        if (outFile.exists() && file.getName().endsWith(".properties")) {
                            System.out.println("Combining properties: file " + file.getAbsolutePath());
                            combinePropertiesFiles(file, outFile);
                        } else {
                            System.out.println("Copying file " + file.getAbsolutePath());
                            Files.copy(file, outFile);
                        }
                    }
                }
            }
        }
    }

    /**
     * For 2 properties files the source and dest file, lets combine the values so that all the values of the sourceFile are in the dest file
     */
    public static void combinePropertiesFiles(File sourceFile, File destFile) throws IOException {
        Properties source = loadProperties(sourceFile);
        Properties dest = loadProperties(destFile);
        Set<Map.Entry<Object,Object>> entries = source.entrySet();
        for (Map.Entry<Object, Object> entry : entries) {
            Object key = entry.getKey();
            Object value = entry.getKey();
            if (key != null && value != null) {
                String keyText = key.toString();
                String valueText = value.toString();
                String oldValue = dest.getProperty(keyText);
                if (oldValue == null || oldValue.trim().length() == 0) {
                    dest.setProperty(keyText, valueText);
                } else {
                    if (oldValue.contains(valueText)) {
                        // we've already added it so ignore!
                    } else {
                        String newValue = oldValue + " " + valueText;
                        dest.setProperty(keyText, newValue);
                    }
                }
            }
        }
        dest.store(new FileWriter(destFile), "Generated by fabric8:full-zip plugin at " + new Date());
    }

    private static Properties loadProperties(File file) throws IOException {
        Properties answer = new Properties();
        answer.load(new FileReader(file));
        return answer;
    }
}
