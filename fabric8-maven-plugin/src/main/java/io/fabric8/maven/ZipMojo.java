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

import io.fabric8.common.util.Files;
import io.fabric8.common.util.Objects;
import io.fabric8.common.util.Strings;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * Name of the created app zip file
     */
    @Parameter(property = "fabric8.zip.outFile", defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}-app.zip")
    private File outputFile;

    /**
     * Name of the aggregated app zip file
     */
    @Parameter(property = "fabric8.aggregated.zip.outFile", defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}-app.zip")
    private File aggregatedZipOutputFile;

    @Component
    private MavenProjectHelper projectHelper;

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
     * Name of the directory used to create the app zip files in each reactor project when creating an aggregated zip
     * for all the {@link #reactorProjects}
     */
    @Parameter(property = "fabric8.fullzip.reactorProjectOutputPath", defaultValue = "target/generated-app")
    private String reactorProjectOutputPath;

    /**
     * The folder used for defining project specific files
     */
    @Parameter(property = "appConfigDir", defaultValue = "${basedir}/src/main/fabric8")
    protected File appConfigDir;

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
     * Provides the resource name of the icon to use; found using the current classpath (including the ones shippped inside the maven plugin).
     */
    @Parameter(property = "fabric8.iconRef")
    protected String iconRef;

    /**
     * Whether or not we should generate a <code>Summary.md</code> file from the pom.xml &lt;description&gt; element text value.
     */
    @Parameter(property = "fabric8.generateSummaryFile", defaultValue = "true")
    protected boolean generateSummaryFile;

    /**
     * The name of the path inside the zip where the app is generated.
     */
    @Parameter(property = "fabric8.pathInZip", defaultValue = "")
    protected String pathInZip;


    /**
     * Whether or not we should ignoreProject this maven project from this goal
     */
    @Parameter(property = "fabric8.ignoreProject", defaultValue = "false")
    private boolean ignoreProject;

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
                MavenProject project = getProject();
                MavenProject parentProject = project.getParent();
                List<MavenProject> projectsWithSameParent = new ArrayList<>();

                for (MavenProject zipGoalProject : fabricZipGoalProjects) {
                    MavenProject zipGoalProjectParent = zipGoalProject.getParent();
                    if (parentProject != null && Objects.equal(parentProject, zipGoalProjectParent)) {
                        projectsWithSameParent.add(zipGoalProject);
                    }
                }
                MavenProject rootProject = null;
                int projectsWithSameParentSize = projectsWithSameParent.size();
                if (projectsWithSameParentSize > 1) {
                    MavenProject lastProject = projectsWithSameParent.get(projectsWithSameParentSize - 1);
                    if (Objects.equal(lastProject, project)) {
                        rootProject = parentProject;
                    }
                }

                if (rootProject != null) {
                    getLog().info("");
                    getLog().info("Creating aggregated app zip");
                    getLog().info("built the last fabric8:zip project so generating a combined zip for all " + projectsWithSameParentSize + " projects with a fabric8:zip goal: " + projectsWithSameParent);

                    getLog().info("Choosing root project " + rootProject.getArtifactId() + " for generation of aggregated zip");
                    generateAggregatedZip(rootProject, fabricZipGoalProjects, projectsWithSameParent);
                }
            }

        } catch (MojoFailureException e) {
            throw e;
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Error executing", e);
        }
    }

    public boolean isIncludeArtifact() {
        return !"pom".equals(getProject().getPackaging());
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

        getLog().info("Attaching aggregated zip " + projectOutputFile + " to root project " + rootProjectArtifactId);
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

            Properties props = new Properties();
            props.setProperty("file", aggregatedZipFileName);
            props.setProperty("groupId", rootProjectGroupId);
            props.setProperty("artifactId", rootProjectArtifactId);
            props.setProperty("version", rootProjectVersion);
            props.setProperty("classifier", "app");
            props.setProperty("packaging", "zip");
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
    }

    protected void generateZip() throws DependencyTreeBuilderException, MojoExecutionException, IOException,
            MojoFailureException {

        File appBuildDir = buildDir;
        if (Strings.isNotBlank(pathInZip)) {
            appBuildDir = new File(buildDir, pathInZip);
        }
        appBuildDir.mkdirs();

        boolean hasConfigDir = appConfigDir.isDirectory();
        if (hasConfigDir) {
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
            if (Strings.isNotBlank(iconRef)) {
                File[] icons = appBuildDir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        if (name == null) {
                            return false;
                        }
                        String lower = name.toLowerCase();
                        return lower.startsWith("icon.") &&
                                (lower.endsWith(".svg") || lower.endsWith(".png") || lower.endsWith(".gif") || lower.endsWith(".jpg") || lower.endsWith(".jpeg"));
                    }
                });
                if (icons == null || icons.length == 0) {
                    // lets copy the iconRef
                    InputStream in = loadPluginResource(iconRef);
                    if (in == null) {
                        getLog().warn("Could not find icon: " + iconRef + " on the ClassPath!");
                    } else {
                        String fileName = "icon." + Files.getFileExtension(iconRef);
                        File outFile = new File(appBuildDir, fileName);
                        Files.copy(in, new FileOutputStream(outFile));
                        getLog().info("Generated icon file " + outFile + " from icon reference: " + iconRef);
                    }
                }
            }

        }

        // lets only generate a app zip if we have a requirement (e.g. we're not a parent pom packaging project) and
        // we have defined some configuration files or dependencies
        // to avoid generating dummy apps for parent poms
        if (hasConfigDir || !ignoreProject) {

            if (includeReadMe) {
                copyReadMe(project.getFile().getParentFile(), appBuildDir);
            }

            if (generateSummaryFile) {
                String description = project.getDescription();
                if (Strings.isNotBlank(description)) {
                    File summaryMd = new File(appBuildDir, "Summary.md");
                    summaryMd.getParentFile().mkdirs();
                    if (!summaryMd.exists()) {
                        byte[] bytes = description.getBytes();
                        Files.copy(new ByteArrayInputStream(bytes), new FileOutputStream(summaryMd));
                    }
                }
            }

            Zips.createZipFile(getLog(), buildDir, outputFile);

            projectHelper.attachArtifact(project, artifactType, artifactClassifier, outputFile);
            getLog().info("Created app zip file: " + outputFile);
        }
    }

    protected InputStream loadPluginResource(String iconRef) throws MojoExecutionException {
        InputStream answer = Thread.currentThread().getContextClassLoader().getResourceAsStream(iconRef);
        if (answer == null) {
            answer = getTestClassLoader().getResourceAsStream(iconRef);
        }
        return answer;
    }

    protected static URLClassLoader createURLClassLoader(Collection<URL> jars) {
        return new URLClassLoader(jars.toArray(new URL[jars.size()]));
    }


    protected URLClassLoader getTestClassLoader() throws MojoExecutionException {
        List<URL> urls = new ArrayList<>();
        try {
            for (Object object : getProject().getTestClasspathElements()) {
                if (object != null) {
                    String path = object.toString();
                    File file = new File(path);
                    URL url = file.toURI().toURL();
                    urls.add(url);
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to resolve classpath: " + e, e);
        }
        return createURLClassLoader(urls);
    }



    protected static File copyReadMe(File src, File appBuildDir) throws IOException {
        File[] files = src.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase(Locale.ENGLISH).startsWith("readme.");
            }
        });
        if (files != null && files.length == 1) {
            File readme = files[0];
            File outFile = new File(appBuildDir, readme.getName());
            Files.copy(readme, outFile);
            return outFile;
        }

        return null;
    }

    private String getReadMeFileKey(String relativePath) {
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
                                       boolean includeReadMe, List<MavenProject> pomZipProjects) throws IOException {
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

        Zips.createZipFile(getLog(), projectBuildDir, projectOutputFile);
        String relativePath = Files.getRelativePath(projectBaseDir, projectOutputFile);
        while (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        getLog().info("Created app zip file: " + relativePath);
    }

    protected String getChildProjectRelativePath(File projectBaseDir, MavenProject pomProject) throws IOException {
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


    protected static boolean isFile(File file) {
        return file != null && file.exists() && file.isFile();
    }

    public static void combineAppFilesToFolder(MavenProject reactorProject, File buildDir, Log log, String reactorProjectOutputPath) throws IOException {
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
        appendAppConfigFiles(outDir, buildDir);
    }

    /**
     * For 2 properties files the source and dest file, lets combine the values so that all the values of the sourceFile are in the dest file
     */
    public static void combinePropertiesFiles(File sourceFile, File destFile) throws IOException {
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

    public static boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }

    /**
     * Copies any local configuration files into the app directory
     */
    protected void copyAppConfigFiles(File appBuildDir, File appConfigDir) throws IOException {

        File[] files = appConfigDir.listFiles();

        if (files != null) {
            appBuildDir.mkdirs();
            for (File file : files) {
                if (!toBeExclude(file.getName())) {
                    File outFile = new File(appBuildDir, file.getName());
                    if (file.isDirectory()) {
                        copyAppConfigFiles(outFile, file);
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
