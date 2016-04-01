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
package io.fabric8.maven;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.fabric8.utils.Files;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
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
 * Generates a ZIP file of the App for the current maven project.
 */
@Mojo(name = "zip", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ZipMojo extends AbstractFabric8Mojo {

    /**
     * Name of the directory used to create the app configuration zip
     */
    @Parameter(property = "fabric8.zip.buildDir", defaultValue = "${project.build.directory}/generated-app")
    private File buildDir;

    /**
     * Whether to include legal files in META-INF directory of the app zip file
     */
    @Parameter(property = "fabric8.zip.includeLegal", defaultValue = "true")
    private boolean includeLegal;

    /**
     * Name of the aggregated app zip file
     */
    @Parameter(property = "fabric8.aggregated.zip.outFile", defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}-app.zip")
    private File aggregatedZipOutputFile;

    @Component
    private MavenProjectHelper projectHelper;

    // this is required for the deploy phase, but end user may just use a install phase only, so let required = false
    @Parameter(defaultValue = "${project.distributionManagementArtifactRepository}", readonly = true, required = false)
    private ArtifactRepository deploymentRepository;

    @Parameter(defaultValue = "${altDeploymentRepository}", readonly = true, required = false)
    private String altDeploymentRepository;

    /**
     * The artifact type for attaching the generated app zip file to the project
     */
    @Parameter(property = "fabric8.zip.artifactType", defaultValue = "zip")
    private String artifactType = "zip";

    /**
     * The artifact classifier for attaching the generated app zip file to the project
     */
    @Parameter(property = "fabric8.zip.artifactClassifier", defaultValue = "app")
    private String artifactClassifier = "app";

    /**
     * The projects in the reactor.
     */
    @Parameter(defaultValue = "${reactorProjects}")
    private List<MavenProject> reactorProjects;

    /**
     * Name of the directory used to create the app zip files in each reactor project when creating an aggregated zip
     * for all the {@link #reactorProjects}
     */
    @Parameter(property = "fabric8.fullzip.reactorProjectOutputPath", defaultValue = "target/generated-app")
    private String reactorProjectOutputPath;

    /**
     * Whether or not we should upload the project readme file if no specific readme file exists in the {@link #appConfigDir}
     */
    @Parameter(property = "fabric8.includeReadMe", defaultValue = "true")
    protected boolean includeReadMe;

    /**
     * If provided then any links in the readme.md files will be replaced to include the given prefix
     */
    @Parameter(property = "fabric8.replaceReadmeLinksPrefix")
    protected String replaceReadmeLinksPrefix;

    /**
     * Whether or not we should generate a <code>Summary.md</code> file from the pom.xml &lt;description&gt; element text value.
     */
    @Parameter(property = "fabric8.generateSummaryFile", defaultValue = "true")
    protected boolean generateSummaryFile;

    /**
     * Whether or not we should generate a <code>fabric8.properties</code> file from the pom.xml.
     */
    @Parameter(property = "fabric8.generateAppPropertiesFile", defaultValue = "true")
    protected boolean generateAppPropertiesFile;

    /**
     * The name of the path inside the zip where the app is generated.
     */
    @Parameter(property = "fabric8.pathInZip", defaultValue = "${project.artifactId}")
    protected String pathInZip;

    /**
     * The maven goal used to deploy aggregated zips. Could be <code>deploy:deploy-file</code> to perform a regular deploy
     * or <code>gpg:sign-and-deploy-file</code> to sign and deploy the file
     */
    @Parameter(property = "fabric8.deployFileGoal", defaultValue = "gpg:sign-and-deploy-file")
    protected String deployFileGoal;

    /**
     * When deploying aggregated zips what URL should we deploy to. Defaults to the
     */
    @Parameter(property = "fabric8.deployFileUrl")
    protected String deployFileUrl;

    /**
     * Whether or not we should ignoreProject this maven project from this goal
     */
    @Parameter(property = "fabric8.ignoreProject", defaultValue = "false")
    private boolean ignoreProject;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (isIgnoreProject()) {
                return;
            }

            if (shouldGenerateForThisProject()) {
                // generate app zip (which we cannot do for a pom project)
                generateZip();
            }

            boolean isLastProject = getProject() == reactorProjects.get(reactorProjects.size() - 1);
            getLog().debug("Is last project? " + isLastProject + " -> " + getProject().getArtifactId());

            if (isLastProject) {
                getLog().info("Last project done. Now generating aggregated zips for the entire project(s).");
                generateAggregatedZips();
            }

        } catch (MojoFailureException | MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Error executing", e);
        }
    }

    protected void generateZip() throws DependencyTreeBuilderException, MojoExecutionException, IOException,
            MojoFailureException {

        File appBuildDir = buildDir;
        if (Strings.isNotBlank(pathInZip)) {
            appBuildDir = new File(buildDir, pathInZip);
        }
        appBuildDir.mkdirs();

        if (hasConfigDir()) {
            copyAppConfigFiles(appBuildDir, appConfigDir);

        } else {
            getLog().info("The app configuration files directory " + appConfigDir + " doesn't exist, so not copying any additional project documentation or configuration files");
        }
        MavenProject project = getProject();

        if (!ignoreProject) {
            File kubernetesJson = getKubernetesJson();
            if (kubernetesJson != null && kubernetesJson.isFile() && kubernetesJson.exists()) {
                File jsonFile = new File(appBuildDir, "kubernetes.json");
                jsonFile.getParentFile().mkdirs();
                Files.copy(kubernetesJson, jsonFile);
            }

            // TODO if no iconRef is specified we could try guess based on the project?

            // lets check if we can use an icon reference
            copyIconToFolder(appBuildDir);

        }

        // lets only generate a app zip if we have a requirement (e.g. we're not a parent pom packaging project) and
        // we have defined some configuration files or dependencies
        // to avoid generating dummy apps for parent poms
        if (hasConfigDir() || !ignoreProject) {

            if (includeReadMe) {
                copyReadMe(appBuildDir);
            }

            if (generateSummaryFile) {
                copySummaryText(appBuildDir);
            }

            if (generateAppPropertiesFile) {
                String name = project.getName();
                if (Strings.isNullOrBlank(name)) {
                    name = project.getArtifactId();
                }
                String description = project.getDescription();
                Properties appProperties = new Properties();
                appProperties.put("name", name);
                if (Strings.isNotBlank(description)) {
                    appProperties.put("description", description);
                }
                appProperties.put("groupId", project.getGroupId());
                appProperties.put("artifactId", project.getArtifactId());
                appProperties.put("version", project.getVersion());
                File appPropertiesFile = new File(appBuildDir, "fabric8.properties");
                appPropertiesFile.getParentFile().mkdirs();
                if (!appPropertiesFile.exists()) {
                    appProperties.store(new FileWriter(appPropertiesFile), "Fabric8 Properties");
                }
            }

            File outputZipFile = getZipFile();
            File legalDir = null;
            if (includeLegal) {
                legalDir = new File(project.getBuild().getOutputDirectory(), "META-INF");
            }
            Zips.createZipFile(getLog(), buildDir, outputZipFile, legalDir);

            projectHelper.attachArtifact(project, artifactType, artifactClassifier, outputZipFile);
            getLog().info("Created app zip file: " + outputZipFile);
        }
    }

    protected void generateAggregatedZips() throws IOException, MojoExecutionException {
        List<MavenProject> zipGoalProjects = fabricZipGoalProjects();
        // we want to walk backwards
        Collections.reverse(zipGoalProjects);

        Set<MavenProject> doneParents = new HashSet<>();
        for (MavenProject zipProject : zipGoalProjects) {

            MavenProject parent = zipProject.getParent();
            if (parent == null) {
                continue;
            }

            // are there 2 or more projects with the same parent
            // then we need to aggregate them to their parent (if we have not done so before)
            Set<MavenProject> group = sameParent(parent, zipGoalProjects);
            if (group.size() >= 2 && !doneParents.contains(parent)) {
                doneParents.add(parent);

                // find transitive sub groups
                Set<MavenProject> nested = sameParentTransitive(parent, zipGoalProjects);
                if (!nested.isEmpty()) {
                    group.addAll(nested);
                }

                generateAggregatedZip(parent, reactorProjects, group);
            }
        }
    }

    private Set<MavenProject> sameParent(MavenProject parent, List<MavenProject> projects) {
        Set<MavenProject> answer = new LinkedHashSet<>();
        for (MavenProject zip : projects) {
            if (Objects.equal(parent, zip.getParent())) {
                answer.add(zip);
            }
        }
        return answer;
    }

    private Set<MavenProject> sameParentTransitive(MavenProject parent, List<MavenProject> projects) {
        Set<MavenProject> answer = new LinkedHashSet<>();
        for (MavenProject zip : projects) {
            if (hasAncestor(parent, zip)) {
                answer.add(zip);
            }
        }
        return answer;
    }

    private List<MavenProject> fabricZipGoalProjects() {
        List<MavenProject> answer = new ArrayList<>();
        if (reactorProjects != null) {
            List<MavenProject> pomZipProjects = new ArrayList<>();
            for (MavenProject reactorProject : reactorProjects) {
                if (isPom(reactorProject)) {
                    pomZipProjects.add(reactorProject);
                }

                List<Plugin> buildPlugins = reactorProject.getBuildPlugins();
                for (Plugin buildPlugin : buildPlugins) {
                    String artifactId = buildPlugin.getArtifactId();
                    if ("fabric8-maven-plugin".equals(artifactId)) {
                        Object goals = buildPlugin.getGoals();
                        boolean hasZipGoal = goals != null && goals.toString().contains("zip");
                        List<PluginExecution> executions = buildPlugin.getExecutions();
                        for (PluginExecution execution : executions) {
                            List<String> execGoals = execution.getGoals();
                            if (execGoals.contains("zip")) {
                                hasZipGoal = true;
                            }
                        }
                        getLog().debug("Project " + reactorProject.getArtifactId() + " has zip goal: " + hasZipGoal);
                        if (hasZipGoal) {
                            answer.add(reactorProject);
                        }
                    }
                }
            }
        }
        return answer;
    }

    protected void generateAggregatedZip(MavenProject rootProject, List<MavenProject> reactorProjects, Set<MavenProject> pomZipProjects) throws IOException, MojoExecutionException {
        File projectBaseDir = rootProject.getBasedir();
        String rootProjectGroupId = rootProject.getGroupId();
        String rootProjectArtifactId = rootProject.getArtifactId();
        String rootProjectVersion = rootProject.getVersion();

        String aggregatedZipFileName = "target/" + rootProjectArtifactId + "-" + rootProjectVersion + "-app.zip";
        File projectOutputFile = new File(projectBaseDir, aggregatedZipFileName);
        getLog().info("Generating " + projectOutputFile.getAbsolutePath() + " from root project " + rootProjectArtifactId);
        File projectBuildDir = new File(projectBaseDir, reactorProjectOutputPath);

        if (projectOutputFile.exists()) {
            projectOutputFile.delete();
        }
        createAggregatedZip(projectBaseDir, projectBuildDir, reactorProjectOutputPath, projectOutputFile,
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
        List<String> activeProfileIds = new ArrayList<>();
        List<Profile> activeProfiles = rootProject.getActiveProfiles();
        if (activeProfiles != null) {
            for (Profile profile : activeProfiles) {
                String id = profile.getId();
                if (Strings.isNotBlank(id)) {
                    activeProfileIds.add(id);
                }
            }
        }
        if (rootProject.hasLifecyclePhase("install")) {
            getLog().info("Installing aggregated zip " + projectOutputFile);
            InvocationRequest request = new DefaultInvocationRequest();
            request.setBaseDirectory(rootProject.getBasedir());
            request.setPomFile(new File("./pom.xml"));
            request.setGoals(Collections.singletonList("install:install-file"));
            request.setRecursive(false);
            request.setInteractive(false);
            request.setProfiles(activeProfileIds);

            Properties props = new Properties();
            props.setProperty("file", aggregatedZipFileName);
            props.setProperty("groupId", rootProjectGroupId);
            props.setProperty("artifactId", rootProjectArtifactId);
            props.setProperty("version", rootProjectVersion);
            props.setProperty("classifier", "app");
            props.setProperty("packaging", "zip");
            props.setProperty("generatePom", "false");
            request.setProperties(props);

            getLog().info("Installing aggregated zip using: mvn install:install-file" + serializeMvnProperties(props));
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

        if (rootProject.hasLifecyclePhase("deploy")) {

            if (deploymentRepository == null && Strings.isNullOrBlank(altDeploymentRepository)) {
                String msg = "Cannot run deploy phase as Maven project has no <distributionManagement> with the maven url to use for deploying the aggregated zip file, neither an altDeploymentRepository property.";
                getLog().warn(msg);
                throw new MojoExecutionException(msg);
            }

            getLog().info("Deploying aggregated zip " + projectOutputFile + " to root project " + rootProject.getArtifactId());
            getLog().info("Using deploy goal: " + deployFileGoal + " with active profiles: " + activeProfileIds);

            InvocationRequest request = new DefaultInvocationRequest();
            request.setBaseDirectory(rootProject.getBasedir());
            request.setPomFile(new File("./pom.xml"));
            request.setGoals(Collections.singletonList(deployFileGoal));
            request.setRecursive(false);
            request.setInteractive(false);
            request.setProfiles(activeProfileIds);
            request.setProperties(getProjectAndFabric8Properties(getProject()));

            Properties props = new Properties();
            props.setProperty("file", aggregatedZipFileName);
            props.setProperty("groupId", rootProjectGroupId);
            props.setProperty("artifactId", rootProjectArtifactId);
            props.setProperty("version", rootProjectVersion);
            props.setProperty("classifier", "app");
            props.setProperty("packaging", "zip");
            String deployUrl = null;

            if (! Strings.isNullOrBlank(deployFileUrl)) {
                deployUrl = deployFileUrl;
            } else if (altDeploymentRepository != null && altDeploymentRepository.contains("::")) {
                deployUrl = altDeploymentRepository.substring(altDeploymentRepository.lastIndexOf("::") + 2);
            } else {
                deployUrl = deploymentRepository.getUrl();
            }

            props.setProperty("url", deployUrl);
            props.setProperty("repositoryId", deploymentRepository.getId());
            props.setProperty("generatePom", "false");
            request.setProperties(props);

            getLog().info("Deploying aggregated zip using: mvn deploy:deploy-file" + serializeMvnProperties(props));
            Invoker invoker = new DefaultInvoker();
            try {
                InvocationResult result = invoker.execute(request);
                if (result.getExitCode() != 0) {
                    throw new IllegalStateException("Error invoking Maven goal deploy:deploy-file");
                }
            } catch (MavenInvocationException e) {
                throw new MojoExecutionException("Error invoking Maven goal deploy:deploy-file", e);
            }
        }
    }

    private static boolean hasAncestor(MavenProject root, MavenProject target) {
        if (target.getParent() == null) {
            return false;
        }
        if (Objects.equal(root, target.getParent())) {
            return true;
        } else {
            return hasAncestor(root, target.getParent());
        }
    }

    private static String getReadMeFileKey(String relativePath) {
        String answer = relativePath;

        if (Strings.isNullOrBlank(answer)) {
            return "<root>";
        }

        // remove leading path which can be either unix or windows style
        int pos = relativePath.indexOf('/');
        int pos2 = relativePath.indexOf('\\');
        if (pos > 0 && pos2 > 0) {
            pos = Math.max(pos, pos2);
        } else if (pos2 > 0) {
            pos = pos2;
        }
        if (pos > -1) {
            answer = relativePath.substring(pos);
        }

        // and remove any leading path separators
        answer = Files.stripLeadingSeparator(answer);

        if (Strings.isNullOrBlank(answer)) {
            answer = "<root>";
        }
        return answer;
    }

    /**
     * Replacing github links with fabric apps links for our quickstarts
     */
    protected String replaceGithubLinks(Set<String> names, String relativePath, String line) {
        boolean changed = false;
        Pattern pattern = Pattern.compile("\\[(.*?)\\]\\((.*?)\\)");
        Matcher matcher = pattern.matcher(line);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String s2 = matcher.group(2);
            if (s2.startsWith("http:") || s2.startsWith("https:")) {
                // leave it as-is
                matcher.appendReplacement(sb, "[$1]($2)");
            } else {
                if (names.contains(s2) || names.contains(relativePath + s2) || names.contains(relativePath + "/" + s2)) {
                    // need to ensure path is app friendly
                    s2 = s2;
                    if (relativePath != null && !"<root>".equals(relativePath)) {
                        s2 = addToPath(relativePath, s2);
                    }
                    // its a directory
                    matcher.appendReplacement(sb, "[$1](" + replaceReadmeLinksPrefix + s2 + ")");
                } else {
                    // need to ensure path is app friendly
                    s2 = s2;
                    if (relativePath != null && !"<root>".equals(relativePath)) {
                        s2 = addToPath(relativePath, s2);
                    }
                    // its a app
                    matcher.appendReplacement(sb, "[$1](" + replaceReadmeLinksPrefix + s2 + ".app)");
                }
                changed = true;
            }
        }
        matcher.appendTail(sb);
        if (changed) {
            return sb.toString();
        } else {
            return null;
        }
    }

    private static String addToPath(String path, String add) {
        if (add.startsWith("/") || path.endsWith("/")) {
            return path + add;
        } else {
            return path + "/" + add;
        }
    }

    protected void createAggregatedZip(File projectBaseDir, File projectBuildDir,
                                       String reactorProjectOutputPath, File projectOutputFile,
                                       boolean includeReadMe, Set<MavenProject> pomZipProjects) throws IOException {
        projectBuildDir.mkdirs();

        for (MavenProject reactorProject : pomZipProjects) {
            // ignoreProject the execution root which just aggregates stuff
            if (!reactorProject.isExecutionRoot()) {
                Log log = getLog();

                // TODO allow the project nesting to be defined via a property?
                String relativePath = getChildProjectRelativePath(projectBaseDir, reactorProject);
                File outDir = new File(projectBuildDir, relativePath);
                combineAppFilesToFolder(reactorProject, outDir, log, reactorProjectOutputPath);
            }
        }

        // we may want to include readme files for pom projects
        if (includeReadMe) {

            Map<String, File> pomNames = new HashMap<String, File>();

            for (MavenProject pomProject : pomZipProjects) {
                File src = pomProject.getFile().getParentFile();
                String relativePath = getChildProjectRelativePath(projectBaseDir, pomProject);
                File outDir = new File(projectBuildDir, relativePath);
                File copiedFile = copyReadMe(src, outDir);

                if (copiedFile != null) {
                    String key = getReadMeFileKey(relativePath);
                    pomNames.put(key, copiedFile);
                }
            }

            if (replaceReadmeLinksPrefix != null) {

                // now parse each readme file and replace github links
                for (Map.Entry<String, File> entry : pomNames.entrySet()) {
                    File file = entry.getValue();
                    String key = entry.getKey();

                    boolean changed = false;
                    List<String> lines = Files.readLines(file);
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        String newLine = replaceGithubLinks(pomNames.keySet(), key, line);
                        if (newLine != null) {
                            lines.set(i, newLine);
                            changed = true;
                        }
                    }
                    if (changed) {
                        Files.writeLines(file, lines);
                        getLog().info("Replaced github links to fabric apps in reaadme file: " + file);
                    }
                }
            }
        }

        Zips.createZipFile(getLog(), projectBuildDir, projectOutputFile, null);
        String relativePath = Files.getRelativePath(projectBaseDir, projectOutputFile);
        while (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        getLog().info("Created app zip file: " + relativePath);
    }

    protected static String getChildProjectRelativePath(File projectBaseDir, MavenProject pomProject) throws IOException {
        // must include first dir as prefix
        String root = projectBaseDir.getName();
        String relativePath = Files.getRelativePath(projectBaseDir, pomProject.getBasedir());
        relativePath = root + File.separator + relativePath;
        return relativePath;
    }

    /**
     * Combines any files from the appSourceDir into the output directory
     */
    public static void appendAppConfigFiles(File appSourceDir, File outputDir) throws IOException {
        if (appSourceDir.exists() && appSourceDir.isDirectory()) {
            File[] files = appSourceDir.listFiles();
            if (files != null) {
                outputDir.mkdirs();
                for (File file : files) {
                    File outFile = new File(outputDir, file.getName());
                    if (file.isDirectory()) {
                        appendAppConfigFiles(file, outFile);
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

    protected static void combineAppFilesToFolder(MavenProject reactorProject, File buildDir, Log log, String reactorProjectOutputPath) throws IOException {
        File basedir = reactorProject.getBasedir();
        if (!basedir.exists()) {
            log.warn("No basedir " + basedir.getAbsolutePath() + " for project + " + reactorProject);
            return;
        }
        File outDir = new File(basedir, reactorProjectOutputPath);
        if (!outDir.exists()) {
            log.warn("No app output dir at: " + outDir.getAbsolutePath() + " for project + " + reactorProject + " so ignoring this project.");
            return;
        }
        log.info("Copying apps from " + outDir.getAbsolutePath() + " into the output directory: " + buildDir);

        File[] files = outDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    appendAppConfigFiles(file, buildDir);
                }
            }
        }
    }

    /**
     * For 2 properties files the source and dest file, lets combine the values so that all the values of the sourceFile are in the dest file
     */
    protected static void combinePropertiesFiles(File sourceFile, File destFile) throws IOException {
        Properties source = loadProperties(sourceFile);
        Properties dest = loadProperties(destFile);
        Set<Map.Entry<Object, Object>> entries = source.entrySet();
        for (Map.Entry<Object, Object> entry : entries) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (key != null && value != null) {
                String keyText = key.toString();
                String valueText = value.toString();
                String oldValue = dest.getProperty(keyText);
                if (oldValue == null || oldValue.trim().length() == 0) {
                    dest.setProperty(keyText, valueText);
                } else {
                    if (oldValue.contains(valueText)) {
                        // we've already added it so ignoreProject!
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

    protected String escapeAgentPropertiesKey(String text) {
        return text.replaceAll("\\:", "\\\\:");
    }

    protected String escapeAgentPropertiesValue(String text) {
        return escapeAgentPropertiesKey(text);
    }

    private static String leadingSlash(String path) {
        if (path.startsWith("/")) {
            return path;
        } else {
            return "/" + path;
        }
    }

    private String serializeMvnProperties(Properties properties) {
        StringBuilder sb = new StringBuilder();
        if (properties != null) {
            for (Iterator it = properties.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();

                String key = (String) entry.getKey();
                String value = (String) entry.getValue();

                sb.append(" -D").append(key).append('=').append(value);
            }
        }
        return sb.toString();
    }

}
