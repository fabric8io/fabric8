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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.common.util.Files;
import io.fabric8.common.util.Strings;
import io.fabric8.deployer.dto.DependencyDTO;
import io.fabric8.deployer.dto.DtoHelper;
import io.fabric8.deployer.dto.ProjectRequirements;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;

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

    /**
     * The projects in the reactor.
     */
    @Parameter(defaultValue = "${reactorProjects}")
    private List<MavenProject> reactorProjects;

    /**
     * Name of the directory used to create the profile zip files in each reactor project when creating an aggregated zip
     * for all the {@link #reactorProjects}
     */
    @Parameter(property = "fabric8.fullzip.reactorProjectOutputPath", defaultValue = "target/generated-profiles")
    private String reactorProjectOutputPath;

    /**
     * Whether or not we should upload the root readme file if no specific readme file exists in the {@link #profileConfigDir}
     */
    @Parameter(property = "fabric8.includeRootReadMe", defaultValue = "true")
    private boolean includeRootReadMe;

    /**
     * The Maven Session.
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    protected MavenSession session;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (isIgnore()) return;

            generateZip();

            if (reactorProjects != null) {
                List<MavenProject> pomZipProjects = new ArrayList<>();
                List<MavenProject> projectsWithZip = new ArrayList<>();
                for (MavenProject reactorProject : reactorProjects) {
                    List<Plugin> buildPlugins = reactorProject.getBuildPlugins();
                    for (Plugin buildPlugin : buildPlugins) {
                        String artifactId = buildPlugin.getArtifactId();
                        // TODO I guess we could try find if the "zip" goal is being invoked?
                        if ("fabric8-maven-plugin".equals(artifactId)) {
                            // TODO should we only consider reactorProjects which have a fabric8:zip goal?
                            Object goals = buildPlugin.getGoals();
                            boolean hasZipGoal = goals != null && goals.toString().contains("zip");
                            List<PluginExecution> executions = buildPlugin.getExecutions();
                            for (PluginExecution execution : executions) {
                                List<String> execGoals = execution.getGoals();
                                if (execGoals.contains("zip")) {
                                    hasZipGoal = true;
                                }
                            }
                            getLog().debug("project " + reactorProject.getArtifactId() + " has zip goal: " + hasZipGoal);

                            if ("pom".equals(reactorProject.getPackaging())) {
                                pomZipProjects.add(reactorProject);
                            }
                            projectsWithZip.add(reactorProject);
                        }
                    }
                }
                int projectsWithZipSize = projectsWithZip.size();
                if (projectsWithZipSize > 0) {
                    MavenProject lastProject = projectsWithZip.get(projectsWithZipSize - 1);
                    if (lastProject == project && projectsWithZipSize > 0) {
                        getLog().info("");
                        getLog().info("Creating aggregated profile zip");
                        getLog().info("built the last fabric8:zip project so generating a combined zip for all " + projectsWithZipSize + " projects with a fabric8:zip goal");

                        // lets pick the root project to build it in
                        MavenProject rootProject;
                        int pomZipProjectsSize = pomZipProjects.size();
                        if (pomZipProjectsSize > 0) {
                            //rootProject = pomZipProjects.get(pomZipProjectsSize - 1);
                            rootProject = pomZipProjects.get(0);
                            if (pomZipProjects.size() > 1) {
                                getLog().debug("pom packaged projects with fabric8:zip goals: " + pomZipProjects);
                            }
                        } else {
                            rootProject = reactorProjects.get(0);
                        }
                        getLog().info("Choosing root project " + rootProject.getArtifactId() + " for generation of aggregated zip");
                        generateAggregatedZip(rootProject, projectsWithZip);
                    }
                }
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Error executing", e);
        }
    }

    protected void generateAggregatedZip(MavenProject rootProject, List<MavenProject> reactorProjects) throws IOException {
        File projectBaseDir = rootProject.getBasedir();
        File projectOutputFile = new File(projectBaseDir, "target/profile.zip");
        getLog().info("Generating " + projectOutputFile.getAbsolutePath() + " from root project " + rootProject.getArtifactId());
        File projectBuildDir = new File(projectBaseDir, reactorProjectOutputPath);
        createAggregatedZip(reactorProjects, projectBaseDir, projectBuildDir, reactorProjectOutputPath, projectOutputFile);
        projectHelper.attachArtifact(rootProject, artifactType, artifactClassifier, projectOutputFile);
    }


    protected void generateZip() throws DependencyTreeBuilderException, MojoExecutionException, IOException {
        ProjectRequirements requirements = new ProjectRequirements();

        DependencyDTO rootDependency = null;
        if (!"pom".equals(project.getPackaging()) && isIncludeArtifact()) {
            rootDependency = loadRootDependency();
            requirements.setRootDependency(rootDependency);
        }
        configureRequirements(requirements);
        if (isIncludeArtifact()) {
            addProjectArtifactBundle(requirements);
        }

        File profileBuildDir = createProfileBuildDir(requirements.getProfileId());

        boolean hasConfigDir = profileConfigDir.isDirectory();
        if (hasConfigDir) {
            copyProfileConfigFiles(profileBuildDir, profileConfigDir);
        } else {
            getLog().info("The profile configuration files directory " + profileConfigDir + " doesn't exist, so not copying any additional project documentation or configuration files");
        }
//        if (includeRootReadMe) {
//            copyReadMe(profileBuildDir);
//        }

        // lets only generate a profile zip if we have a requirement (e.g. we're not a parent pom packaging project) and
        // we have defined some configuration files or dependencies
        // to avoid generating dummy profiles for parent poms
        if (hasConfigDir || rootDependency != null ||
                notEmpty(requirements.getBundles()) || notEmpty(requirements.getFeatures()) || notEmpty(requirements.getFeatureRepositories())) {

            if (isIncludeArtifact()) {
                writeProfileRequirements(requirements, profileBuildDir);
            }
            generateFabricAgentProperties(requirements, new File(profileBuildDir, "io.fabric8.agent.properties"));

            Zips.createZipFile(getLog(), buildDir, outputFile);

            projectHelper.attachArtifact(project, artifactType, artifactClassifier, outputFile);

            String relativePath = Files.getRelativePath(project.getBasedir(), outputFile);
            while (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            getLog().info("Created profile zip file: " + relativePath);
        }
    }

    protected void copyReadMe(File profileBuildDir) throws IOException {
        File[] files = project.getBasedir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase(Locale.ENGLISH).startsWith("readme.");
            }
        });
        if (files != null && files.length == 1) {
            File readme = files[0];
            File outFile = new File(profileBuildDir, readme.getName());
            Files.copy(readme, outFile);
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
                        copyProfileConfigFiles(outFile, file);
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

    protected void writeProfileRequirements(ProjectRequirements requirements, File profileBuildDir) throws IOException {
        ObjectMapper mapper = DtoHelper.getMapper();
        String name = DtoHelper.getRequirementsConfigFileName(requirements);
        File outFile = new File(profileBuildDir, name);
        outFile.getParentFile().mkdirs();
        mapper.writeValue(outFile, requirements);
        getLog().info("Writing " + outFile);
    }

    protected void generateFabricAgentProperties(ProjectRequirements requirements, File file) throws MojoExecutionException, IOException {
        file.getParentFile().mkdirs();

        PrintWriter writer = new PrintWriter(new FileWriter(file));
        try {
            String profileId = requirements.getProfileId();
            writer.println("# Profile: " +  profileId);
            writer.println("# generated by the fabric8 maven plugin at " + new Date());
            writer.println("# see: http://fabric8.io/gitbook/mavenPlugin.html");
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
