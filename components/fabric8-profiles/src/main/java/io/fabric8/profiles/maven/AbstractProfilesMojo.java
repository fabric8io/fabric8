/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.profiles.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import io.fabric8.profiles.ProfilesHelpers;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Base class for Profiles mojos.
 */
public abstract class AbstractProfilesMojo extends AbstractMojo {

    protected static final String FABRIC8_PROFILES_CFG = "fabric8-profiles.cfg";
    protected static final String LAST_BUILD_COMMIT_ID_PROPERTY = "lastBuildCommitId";
    protected final Log log = getLog();

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * Profile and container repository directory, defaults to ${project.basedir}.
     */
    @Parameter(defaultValue = "${project.basedir}", readonly = false, required = true)
    protected File sourceDirectory;

    /**
     * Build directory, defaults to ${project.build.outputDir}.
     */
    @Parameter(defaultValue = "${project.build.outputDir}", readonly = false, required = true)
    protected File targetDirectory;

    /**
     * Build properties, overrides fabric8-profies.cfg under {@literal sourceDirectory}.
     */
    @Parameter(readonly = false, required = true)
    protected Properties profilesProperties;

    protected Path configs;
    protected Path profiles;
    protected String lastCommitId;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // read generator properties
        final Path sourcePath = Paths.get(sourceDirectory.getAbsolutePath());
        try {
            final Properties propertiesFile = ProfilesHelpers.readPropertiesFile(sourcePath.resolve(FABRIC8_PROFILES_CFG));
            if (profilesProperties == null) {
                profilesProperties = propertiesFile;
            } else {
                final Properties userOverrides = profilesProperties;
                profilesProperties = new Properties(propertiesFile);
                profilesProperties.putAll(userOverrides);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error reading " + FABRIC8_PROFILES_CFG + ": " + e.getMessage(), e);
        }

        // last build id
        lastCommitId = profilesProperties.getProperty(LAST_BUILD_COMMIT_ID_PROPERTY);

        // repository paths
        final Path repository = Paths.get(sourceDirectory.getAbsolutePath());

        configs = repository.resolve("configs");
        if (!Files.isDirectory(configs)) {
            throw new MojoExecutionException("Missing container directory " + configs);
        }
        profiles = repository.resolve("profiles");
        if (!Files.isDirectory(profiles)) {
            throw new MojoExecutionException("Missing profiles directory " + configs);
        }
    }
}
