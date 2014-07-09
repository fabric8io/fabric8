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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.common.util.Files;
import io.fabric8.common.util.Strings;
import io.fabric8.deployer.dto.DependencyDTO;
import io.fabric8.deployer.dto.DtoHelper;
import io.fabric8.deployer.dto.ProjectRequirements;
import io.fabric8.service.child.ChildConstants;
import org.apache.maven.artifact.Artifact;
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
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

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
            if (isIgnoreProject()) return;

            generateZip();

            if (reactorProjects != null) {
                List<MavenProject> pomZipProjects = new ArrayList<>();
                List<MavenProject> fabricZipGoalProjects = new ArrayList<>();
                List<MavenProject> fabricHasParentZipGoalProject = new ArrayList<MavenProject>();
                for (MavenProject reactorProject : reactorProjects) {

                    if ("pom".equals(reactorProject.getPackaging())) {
                        pomZipProjects.add(reactorProject);
                    }

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

                            fabricZipGoalProjects.add(reactorProject);
                        }
                    }
                }

                // we want a list of projects which has a parent that has a zip goal too
                // as that helps us detect the 'last' project when we do a full build from the entire project
                for (MavenProject project : fabricZipGoalProjects) {
                    if (fabricZipGoalProjects.contains(project.getParent())) {
                        fabricHasParentZipGoalProject.add(project);
                    }
                }

                // are we the last project?
                boolean last = reactorProjects.size() > 1 && project == reactorProjects.get(reactorProjects.size() - 1);
                if (!last) {
                    // are we the last project with the zip goal, part of a group as they have a parent?
                    // TODO: there can be multiple groups, so when we switch to a new group we should aggregate
                    last = fabricHasParentZipGoalProject.size() > 1 && project == fabricHasParentZipGoalProject.get(fabricHasParentZipGoalProject.size() - 1);
                }
                if (!last) {
                    // are we the last project with the zip goal?
                    last = fabricZipGoalProjects.size() > 1 && project == fabricZipGoalProjects.get(fabricZipGoalProjects.size() - 1);
                }

                // we need to generate the aggregated zip last, so we have all the generated profiles in the other modules
                // which we can aggregate
                if (last) {
                    getLog().info("");
                    getLog().info("Creating aggregated profile zip");
                    getLog().info("built the last fabric8:zip project so generating a combined zip for all " + fabricZipGoalProjects.size() + " projects with a fabric8:zip goal");

                    // favor root project as the 1st project with fabric8:zip goal
                    MavenProject rootProject = fabricZipGoalProjects.size() > 0 ? fabricZipGoalProjects.get(0) : reactorProjects.get(0);

                    // we got the root project, now filter out pom projects which has the rootProject as one of their parents
                    List<MavenProject> ourPomZipProjects = new ArrayList<MavenProject>();
                    // include the root project if its a zip as well
                    if (pomZipProjects.contains(rootProject)) {
                        ourPomZipProjects.add(rootProject);
                    }
                    ourPomZipProjects.add(rootProject);
                    for (MavenProject zip : pomZipProjects) {
                        if (hasParent(zip, rootProject, true)) {
                            ourPomZipProjects.add(zip);
                        }
                    }

                    getLog().info("Choosing root project " + rootProject.getArtifactId() + " for generation of aggregated zip");
                    generateAggregatedZip(rootProject, fabricZipGoalProjects, ourPomZipProjects);
                }
            }

        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Error executing", e);
        }
    }

    protected boolean hasParent(MavenProject me, MavenProject parent, boolean recusive) {
        if (me == null) {
            return false;
        } else if (me.getParent() == parent) {
            return true;
        } else if (recusive) {
            return hasParent(me.getParent(), parent, recusive);
        } else {
            return false;
        }
    }

    protected void generateAggregatedZip(MavenProject rootProject, List<MavenProject> reactorProjects, List<MavenProject> pomZipProjects) throws IOException, MojoExecutionException {
        File projectBaseDir = rootProject.getBasedir();
        File projectOutputFile = new File(projectBaseDir, "target/profile.zip");
        getLog().info("Generating " + projectOutputFile.getAbsolutePath() + " from root project " + rootProject.getArtifactId());
        File projectBuildDir = new File(projectBaseDir, reactorProjectOutputPath);

        createAggregatedZip(reactorProjects, projectBaseDir, projectBuildDir, reactorProjectOutputPath, projectOutputFile,
                includeReadMe, pomZipProjects);
        if (rootProject.getAttachedArtifacts() != null) {
            // need to remove existing as otherwise we get a WARN
            Artifact found = null;
            for (Artifact artifact : rootProject.getAttachedArtifacts()) {
                if (artifactClassifier != null && artifact.hasClassifier() && artifact.getClassifier().equals(artifactClassifier)) {
                    found = artifact;
                    break;
                }
            }
            if (found != null) {
                rootProject.getAttachedArtifacts().remove(found);
            }
        }

        getLog().info("Attaching aggregated zip " + projectOutputFile + " to root project " + rootProject.getArtifactId());
        projectHelper.attachArtifact(rootProject, artifactType, artifactClassifier, projectOutputFile);

        // if we are doing an install goal, then also install the aggregated zip manually
        // as maven will install the root project first, and then build the reactor projects, and at this point
        // it does not help to attach artifact to root project, as those artifacts will not be installed
        // so we need to install manually
        if (rootProject.hasLifecyclePhase("install")) {
            getLog().info("Installing aggregated zip " + projectOutputFile);
            InvocationRequest request = new DefaultInvocationRequest();
            request.setBaseDirectory(rootProject.getBasedir());
            request.setPomFile(new File("./pom.xml"));
            request.setGoals(Collections.singletonList("install:install-file"));
            request.setRecursive(false);
            request.setInteractive(false);

            String opts = String.format("-Dfile=target/profile.zip -DgroupId=%s -DartifactId=%s -Dversion=%s -Dclassifier=profile -Dpackaging=zip", rootProject.getGroupId(), rootProject.getArtifactId(), rootProject.getVersion());
            request.setMavenOpts(opts);

            getLog().info("Installing aggregated zip using: mvn install:install-file " + opts);
            Invoker invoker = new DefaultInvoker();
            try {
                InvocationResult result = invoker.execute(request);
                if (result.getExitCode() != 0) {
                    throw new IllegalStateException("Error invoking Maven goal install:install-file");
                }
            } catch (MavenInvocationException e) {
                throw new MojoExecutionException("Error invoking Maven goal install:install-file", e);
            }
        }
    }

    protected void generateZip() throws DependencyTreeBuilderException, MojoExecutionException, IOException {
        ProjectRequirements requirements = new ProjectRequirements();

        DependencyDTO rootDependency = null;
        if (isIncludeArtifact()) {
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

        // lets only generate a profile zip if we have a requirement (e.g. we're not a parent pom packaging project) and
        // we have defined some configuration files or dependencies
        // to avoid generating dummy profiles for parent poms
        if (hasConfigDir || rootDependency != null ||
                notEmpty(requirements.getBundles()) || notEmpty(requirements.getFeatures()) || notEmpty(requirements.getFeatureRepositories())) {

            if (includeReadMe) {
                copyReadMe(project.getFile().getParentFile(), profileBuildDir);
            }

            if (generateSummaryFile) {
                String description = project.getDescription();
                if (Strings.isNotBlank(description)) {
                    File summaryMd = new File(profileBuildDir, "Summary.md");
                    summaryMd.getParentFile().mkdirs();
                    if (!summaryMd.exists()) {
                        byte[] bytes = description.getBytes();
                        Files.copy(new ByteArrayInputStream(bytes), new FileOutputStream(summaryMd));
                    }
                }
            }

            if (isIncludeArtifact()) {
                writeProfileRequirements(requirements, profileBuildDir);
            }
            generateFabricAgentProperties(requirements, new File(profileBuildDir, "io.fabric8.agent.properties"));
            generateFabricContextPathProperties(requirements, new File(profileBuildDir, ChildConstants.WEB_CONTEXT_PATHS_PID + ".properties"));

            Zips.createZipFile(getLog(), buildDir, outputFile);

            projectHelper.attachArtifact(project, artifactType, artifactClassifier, outputFile);
            getLog().info("Created profile zip file: " + outputFile);
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
            writer.println("# Profile: " + profileId);
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

    protected void generateFabricContextPathProperties(ProjectRequirements requirements, File file) throws MojoExecutionException, IOException {
        String webContextPath = requirements.getWebContextPath();
        if (Strings.isNullOrBlank(webContextPath)) {
            // no file need to be generated
            return;
        }

        file.getParentFile().mkdirs();
        PrintWriter writer = new PrintWriter(new FileWriter(file));
        try {
            // the path must start with a leading slash
            String path = leadingSlash(webContextPath);
            String key = project.getGroupId() + "/" + project.getArtifactId();
            writer.println(escapeAgentPropertiesKey(key) + " = " + escapeAgentPropertiesValue(path));
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

    protected static String leadingSlash(String path) {
        if (path.startsWith("/")) {
            return path;
        } else {
            return "/" + path;
        }
    }

}
