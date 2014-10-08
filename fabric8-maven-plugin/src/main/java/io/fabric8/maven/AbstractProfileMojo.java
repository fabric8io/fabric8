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

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.fabric8.common.util.Files;
import io.fabric8.common.util.Strings;
import io.fabric8.deployer.dto.DependencyDTO;
import io.fabric8.deployer.dto.ProjectRequirements;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.codehaus.plexus.util.IOUtil;
import org.osgi.framework.Constants;

/**
 * Abstract base class for Profile based mojos
 */
public abstract class AbstractProfileMojo extends AbstractMojo {

    /**
     * The set of packaging types that should be omitted from the bundle spec
     * <ul>
     *   <li>'jar' - because the resolve implicitly searches for jars when no type is specified</li>
     *   <li>'bundle' - because a bundle is a jar, but the resolver doesn't account for this</li>
     * </ul>
     */
    private static final String[] OMITTED_BUNDLE_TYPES = new String[]{"jar", "bundle"};

    /**
     * The folder used for defining project specific files
     */
    @Parameter(property = "profileConfigDir", defaultValue = "${basedir}/src/main/fabric8")
    protected File profileConfigDir;

    @Component
    protected MavenProject project;

    @Component
    protected ArtifactCollector artifactCollector;

    @Component
    protected ArtifactFactory artifactFactory;

    @Component
    protected DependencyTreeBuilder dependencyTreeBuilder;

    @Component
    protected ArtifactMetadataSource metadataSource;

    @Component
    protected ArtifactResolver resolver;

    @Parameter(property = "localRepository", readonly = true, required = true)
    protected ArtifactRepository localRepository;

    @Parameter(property = "project.remoteArtifactRepositories")
    protected List remoteRepositories;

    /**
     * The scope to filter by when resolving the dependency tree
     */
    @Parameter(property = "fabric8.scope", defaultValue = "compile")
    private String scope;

    /**
     * The profile ID to deploy to. If not specified then it defaults to the groupId-artifactId of the project
     */
    @Parameter(property = "fabric8.profile")
    private String profile;

    /**
     * Whether the profile is abstract
     */
    @Parameter(property = "fabric8.abstractProfile", defaultValue = "false")
    private boolean abstractProfile;

    /**
     * The profile version to deploy to. If not specified then the current latest version is used.
     */
    @Parameter(property = "fabric8.profileVersion")
    private String version;

    /**
     * The profile base version used if the version specified is a new version.
     */
    @Parameter(property = "fabric8.baseVersion")
    private String baseVersion;

    /**
     * The space separated list of parent profile IDs to use for the profile
     */
    @Parameter(property = "fabric8.parentProfiles")
    private String parentProfiles;

    /**
     * The space separated list of bundle URLs (in addition to the project artifact) which should be added to the profile
     */
    @Parameter(property = "fabric8.bundles")
    private String bundles;

    /**
     * The space separated list of features to be added to the profile
     */
    @Parameter(property = "fabric8.features")
    private String features;

    /**
     * The space separated list of feature repository URLs to be added to the profile
     */
    @Parameter(property = "fabric8.featureRepos")
    private String featureRepos;

    /**
     * If enabled then the OSGi resolver is used to try deduce extra bundles or features
     * required to be added from the transitive maven dependencies.
     */
    @Parameter(property = "fabric8.useResolver", defaultValue = "true")
    private boolean useResolver;

    /**
     * Specifies whether or not to specify that the profile should be locked.
     */
    @Parameter(property = "fabric8.locked")
    private Boolean locked;

    /**
     * The minimum number of instances of this profile which we require to run.
     */
    @Parameter(property = "fabric8.minInstanceCount", defaultValue = "1")
    private Integer minInstanceCount;

    /**
     * Whether or not we should add the maven deployment unit to the fabric profile
     */
    @Parameter(property = "fabric8.includeArtifact", defaultValue = "true")
    private boolean includeArtifact;

    /**
     * Type to use for the project artifact bundle reference
     */
    @Parameter(property = "fabric8.artifactBundleType")
    private String artifactBundleType;

    /**
     * Classifier to use for the project artifact bundle reference
     */
    @Parameter(property = "fabric8.artifactBundleClassifier")
    private String artifactBundleClassifier;

    /**
     * Whether or not we should ignoreProject this maven project from goals like fabric8:deploy or fabric8:zip
     */
    @Parameter(property = "fabric8.ignoreProject", defaultValue = "false")
    private boolean ignoreProject;

    /**
     * Whether or not we should upload the project readme file if no specific readme file exists in the {@link #profileConfigDir}
     */
    @Parameter(property = "fabric8.includeReadMe", defaultValue = "true")
    protected boolean includeReadMe;

    /**
     * Whether or not we should generate a <code>Summary.md</code> file from the pom.xml &lt;description&gt; element text value.
     */
    @Parameter(property = "fabric8.generateSummaryFile", defaultValue = "true")
    protected boolean generateSummaryFile;

    /**
     * The context path to use for web applications for projects using the <code>war</code> packaging
     */
    @Parameter(property = "fabric8.webContextPath", defaultValue = "${project.artifactId}")
    private String webContextPath;

    /**
     * If provided then any links in the readme.md files will be replaced to include the given prefix
     */
    @Parameter(property = "fabric8.replaceReadmeLinksPrefix")
    protected String replaceReadmeLinksPrefix;

    public String getArtifactBundleType() {
        return artifactBundleType;
    }

    public String getArtifactBundleClassifier() {
        return artifactBundleClassifier;
    }

    protected static boolean isFile(File file) {
        return file != null && file.exists() && file.isFile();
    }

    public static void combineProfileFilesToFolder(MavenProject reactorProject, File buildDir, Log log, String reactorProjectOutputPath) throws IOException {
        File basedir = reactorProject.getBasedir();
        if (!basedir.exists()) {
            log.warn("No basedir " + basedir.getAbsolutePath() + " for project + " + reactorProject);
            return;
        }
        File outDir = new File(basedir, reactorProjectOutputPath);
        if (!outDir.exists()) {
            log.warn("No profile output dir at: " + outDir.getAbsolutePath() + " for project + " + reactorProject + " so ignoring this project.");
            return;
        }
        log.info("Copying profiles from " + outDir.getAbsolutePath() + " into the output directory: " + buildDir);
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

    public boolean isIncludeArtifact() {
        return includeArtifact && !"pom".equals(project.getPackaging());
    }

    public boolean isIgnoreProject() {
        return ignoreProject;
    }

    public String getWebContextPath() {
        return webContextPath;
    }

    public void setWebContextPath(String webContextPath) {
        this.webContextPath = webContextPath;
    }

    protected static List<String> parameterToStringList(String parameterValue) {
        List<String> answer = new ArrayList<String>();
        if (Strings.isNotBlank(parameterValue)) {
            String[] split = parameterValue.split("\\s");
            for (String text : split) {
                if (Strings.isNotBlank(text)) {
                    answer.add(text);
                }
            }
        }
        return answer;
    }

    protected String readInput(String prompt) {
        Console console = System.console();
        System.out.print(prompt);
        return console.readLine();
    }

    protected String readPassword(String prompt) {
        Console console = System.console();
        System.out.print(prompt);
        char[] pw = console.readPassword();
        return new String(pw);
    }

    protected void configureRequirements(ProjectRequirements requirements) throws MojoExecutionException {
        if (Strings.isNotBlank(profile)) {
            requirements.setProfileId(profile);
        } else {
            requirements.setProfileId(project.getGroupId() + "-" + project.getArtifactId());
        }
        requirements.setAbstractProfile(abstractProfile);

        String description = project.getDescription();
        if (Strings.isNotBlank(description)) {
            requirements.setDescription(description);
        }
        if (Strings.isNotBlank(version)) {
            requirements.setVersion(version);
        }
        if (Strings.isNotBlank(baseVersion)) {
            requirements.setBaseVersion(baseVersion);
        }
        if (Strings.isNotBlank(webContextPath)) {
            requirements.setWebContextPath(webContextPath);
        }
        if (locked != null) {
            requirements.setLocked(locked);
        }
        List<String> bundleList = parameterToStringList(bundles);
        if (parentProfiles == null || parentProfiles.length() <= 0) {
            parentProfiles = defaultParentProfiles(requirements);
        }
        List<String> profileParentList = parameterToStringList(parentProfiles);
        List<String> featureList = parameterToStringList(features);
        List<String> featureReposList = parameterToStringList(featureRepos);
        requirements.setParentProfiles(profileParentList);
        requirements.setBundles(bundleList);
        requirements.setFeatures(featureList);
        requirements.setFeatureRepositories(featureReposList);
        if (minInstanceCount != null) {
            requirements.setMinimumInstances(minInstanceCount);
        }
        if (useResolver) {
            requirements.setUseResolver(Boolean.TRUE);
        }
    }

    protected String defaultParentProfiles(ProjectRequirements requirements) throws MojoExecutionException {
        // TODO lets try figure out the best parent profile based on the project
        String packaging = project.getPackaging();
        if (packaging != null) {
            if ("jar".equals(packaging)) {
                // lets use the java container
                List<File> files = new ArrayList<File>();
                Set<String> classNames = findMainClasses(files);
                int classNameSize = classNames.size();
                if (classNameSize > 0) {
                    if (classNameSize > 1) {
                        getLog().warn("We found more than one executable main: " + classNames);
                    }
                    // TODO if we've a single className and we've not specified one via a properties file
                    // lets add it to the properties file?
                }

                List<URL> urls = new ArrayList<URL>();
                try {
                    for (File file : files) {
                        URL url = file.toURI().toURL();
                        urls.add(url);
                    }
                    return resolveProfileFromJars(urls);
                } catch (MalformedURLException e) {
                    getLog().warn("Failed to create URLClassLoader from files: " + files);
                }
                return "containers-java";
            } else if ("war".equals(packaging)) {
                return "containers-tomcat";
            } else if ("ear".equals(packaging)) {
                return "containers-wildfly";
            }
        }
        return findOSGiDefaultParentProfiles(requirements);
    }

    protected String resolveProfileFromJars(List<URL> urls) {
        URLClassLoader classLoader = createURLClassLoader(urls);
        Map<String, String> mainToProfileMap = getDefaultJavaClassToParentProfileMap();
        String defaultProfile = "containers-java";
        return resolveProfileFromClassMap(classLoader, mainToProfileMap, defaultProfile);
    }

    protected String findOSGiDefaultParentProfiles(ProjectRequirements requirements) throws MojoExecutionException {
        URLClassLoader classLoader = getCompileClassLoader();
        Map<String, String> classToProfileMap = getDefaultOSGiClassToParentProfileMap();
        Set<Map.Entry<String, String>> entries = classToProfileMap.entrySet();
        List<String> parentProfileNames = new ArrayList<>();
        for (Map.Entry<String, String> entry : entries) {
            String className = entry.getKey();
            String profileName = entry.getValue();
            if (hasClass(classLoader, className)) {
                getLog().info("Found class: " + className + " so adding the parent profile: " + profileName);
                parentProfileNames.add(profileName);
            }
        }
        if (parentProfileNames.isEmpty()) {
            return "karaf";
        } else {
            return Strings.join(parentProfileNames, " ");
        }
    }

    protected Map<String, String> getDefaultOSGiClassToParentProfileMap() {
        Map<String,String> classToProfileMap = new LinkedHashMap<String, String>();
        // TODO it'd be nice to find these automatically by querying the fabric itself for profiles
        // for a PID?
        classToProfileMap.put("org.apache.camel.CamelContext", "feature-camel");
        classToProfileMap.put("org.apache.cxf.Bus", "feature-cxf");
        return classToProfileMap;

    }

    protected String resolveProfileFromClassMap(URLClassLoader classLoader, Map<String, String> mainToProfileMap, String defaultProfile) {
        Set<Map.Entry<String, String>> entries = mainToProfileMap.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String mainClass = entry.getKey();
            String profileName = entry.getValue();
            if (hasClass(classLoader, mainClass)) {
                getLog().info("Found class: " + mainClass + " so defaulting the parent profile: " + profileName);
                return profileName;
            }
        }
        return defaultProfile;
    }

    protected URLClassLoader getCompileClassLoader() throws MojoExecutionException {
        List<URL> urls = new ArrayList<>();
        try {
            for (Object object : project.getCompileClasspathElements()) {
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

    protected static URLClassLoader createURLClassLoader(Collection<URL> jars) {
        return new URLClassLoader(jars.toArray(new URL[jars.size()]));
    }

    protected Map<String, String> getDefaultJavaClassToParentProfileMap() {
        Map<String,String> classToProfileMap = new LinkedHashMap<String, String>();
        // TODO it'd be nice to find these automatically by querying the fabric itself for profiles
        // for the PID and "mainClass" value?
        classToProfileMap.put("org.springframework.boot.SpringApplication", "containers-java.spring.boot");
        classToProfileMap.put("io.fabric8.process.spring.boot.container.FabricSpringApplication", "containers-java.spring.boot");
        classToProfileMap.put("org.apache.camel.spring.Main", "containers-java.camel.spring");
        classToProfileMap.put("org.osgi.framework.BundleContext", "containers-java.pojosr");
        classToProfileMap.put("org.apache.camel.blueprint.ErrorHandlerType", "containers-java.pojosr");
        classToProfileMap.put("javax.enterprise.context.ApplicationScoped", "containers-java.weld");
        return classToProfileMap;
    }

    protected boolean hasClass(URLClassLoader classLoader, String className) {
        try {
            classLoader.loadClass(className);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    protected Set<String> findMainClasses(List<File> files) throws MojoExecutionException {
        Set<String> classNames = new HashSet<String>();
        Artifact artifact = project.getArtifact();
        if (artifact != null) {
            File artifactFile = artifact.getFile();
            addMainClass(classNames, files, artifactFile);
        }
        try {
            for (Object object : project.getCompileClasspathElements()) {
                if (object != null) {
                    String path = object.toString();
                    File file = new File(path);
                    addMainClass(classNames, files, file);
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to resolve classpath: " + e, e);
        }
        return classNames;
    }

    protected void addMainClass(Set<String> classNames, List<File> files, File file) {
        if (file != null && file.exists() && file.isFile()) {
            files.add(file);
            try {
                JarFile jarFile = new JarFile(file);
                Manifest manifest = jarFile.getManifest();
                if (manifest != null) {
                    Attributes attributes = manifest.getMainAttributes();
                    if (attributes != null) {
                        String className = attributes.getValue(Attributes.Name.MAIN_CLASS);
                        if (className != null && className.length() > 0) {
                            getLog().debug("found main class " + className + " in " + file);
                            className = className.trim();
                            if (className.length() > 0) {
                                classNames.add(className);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                getLog().warn("Failed to parse manifest for " + file + ". " + e, e);
            }
        }
    }



    protected void addProjectArtifactBundle(ProjectRequirements requirements) throws MojoFailureException {
        DependencyDTO rootDependency = requirements.getRootDependency();
        if (rootDependency != null) {
            // we need url with type, so when we deploy war files the mvn url is correct
            StringBuilder urlBuffer = new StringBuilder(rootDependency.toBundleUrl());

            String apparentType = rootDependency.getType();
            String apparentClassifier = rootDependency.getClassifier();

            if (artifactBundleType == null) {
                for (String omit : OMITTED_BUNDLE_TYPES) {
                    if (omit.equals(apparentType)) {
                        apparentType = null;
                        break;
                    }
                }
            }

            handleArtifactBundleType(urlBuffer, apparentType);

            handleArtifactBundleClassifier(urlBuffer, apparentClassifier);

            String urlString = urlBuffer.toString();
            if (!requirements.getBundles().contains(urlString)) {
                requirements.getBundles().add(urlString);
            }
        }
    }

    private void handleArtifactBundleType(StringBuilder urlBuffer, String apparentType) {
        if (apparentType != null) {
            urlBuffer.append("/" + apparentType);
        }
    }

    private void handleArtifactBundleClassifier(StringBuilder urlBuffer, String apparentClassifier) throws MojoFailureException {
        String nextUrlComponent = "";
        if (apparentClassifier != null) {
            nextUrlComponent = "/" + apparentClassifier;
        }

        urlBuffer.append(nextUrlComponent);
    }

    private void throwClassifierWithoutTypeException() throws MojoFailureException {
        throw new MojoFailureException(
                "The property artifactBundleClassifier was specified as '" + artifactBundleClassifier
                        +"' without also specifying artifactBundleType");
    }

    protected DependencyDTO loadRootDependency() throws DependencyTreeBuilderException, MojoFailureException {
        ArtifactFilter artifactFilter = createResolvingArtifactFilter();
        DependencyNode dependencyNode = dependencyTreeBuilder.buildDependencyTree(project, localRepository, artifactFactory, metadataSource, artifactFilter, artifactCollector);
        DependencyDTO dependencyDTO = buildFrom(dependencyNode);
        if (artifactBundleType != null) {
            dependencyDTO.setType(artifactBundleType);
        }

        if (artifactBundleClassifier != null) {
            if (artifactBundleType != null) {
                dependencyDTO.setClassifier(artifactBundleClassifier);
            } else {
                throwClassifierWithoutTypeException();
            }
        }
        return dependencyDTO;
    }

    private DependencyDTO buildFrom(DependencyNode node) {
        Artifact artifact = node.getArtifact();
        if (artifact != null) {
            DependencyDTO answer = new DependencyDTO();
            answer.setGroupId(artifact.getGroupId());
            answer.setArtifactId(artifact.getArtifactId());
            answer.setVersion(artifact.getVersion());
            String scope = artifact.getScope();
            answer.setScope(scope);
            answer.setClassifier(artifact.getClassifier());
            answer.setType(artifact.getType());
            // there is a bug if we try to resolve the current projects artifact for a "jar" packaging
            // before we've installed it then this operation will force the jar not be installed
            // so lets ignore this for the maven project's artifact
            if (artifact.getClassifier() == null && "jar".equals(artifact.getType())) {
                if (project.getArtifact().equals(artifact)) {
                    getLog().debug("Ignoring bundle check on the maven project artifact: " + artifact + " as this causes issues with the maven-install-plugin and we can assume the project packaging is accurate");
                } else {
                    try {
                        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
                        request.setArtifact(artifact);
                        request.setRemoteRepositories(remoteRepositories);
                        request.setLocalRepository(localRepository);
                        resolver.resolve(request);
                        JarInputStream jis = new JarInputStream(new FileInputStream(artifact.getFile()));
                        Manifest man = jis.getManifest();
                        String bsn = man.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
                        if (bsn != null) {
                            answer.setType("bundle");
                        } else {
                            // Try to find a matching servicemix bundle for it
                        /*
                        Map<String, String> bundles = getAllServiceMixBundles();
                        getLog().debug("Trying to find a matching bundle for " + artifact);
                        String match = bundles.get(artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion());
                        if (match != null) {
                            String[] parts = match.split(":");
                            answer.setGroupId(parts[0]);
                            answer.setArtifactId(parts[1]);
                            answer.setVersion(parts[2]);
                            getLog().info("Replacing artifact " + artifact + " with servicemix bundle " + match);
                        }
                        */
                        }
                    } catch (Exception e) {
                        getLog().debug("Error checking artifact type for " + artifact, e);
                    }
                }
            }
            answer.setOptional(artifact.isOptional());

            String type = answer.getType();
            if (type != null && type.equals("pom")) {
                getLog().debug("Ignoring pom.xml for " + answer);
                return null;
            }
            int state = node.getState();
            if (state != DependencyNode.INCLUDED) {
                getLog().debug("Ignoring " + node);
                return null;
            }
            if (isWarProject()) {
                if (scope != null && !scope.equals("provided")) {
                    getLog().debug("WAR packaging so ignoring non-provided scope " + scope + " for " + node);
                    return null;
                }
            }
            List children = node.getChildren();
            for (Object child : children) {
                if (child instanceof DependencyNode) {
                    DependencyNode childNode = (DependencyNode) child;
                    if (childNode.getState() == DependencyNode.INCLUDED) {
                        String childScope = childNode.getArtifact().getScope();
                        if (!"test".equals(childScope) && !"provided".equals(childScope)) {
                            DependencyDTO childDTO = buildFrom(childNode);
                            if (childDTO != null) {
                                answer.addChild(childDTO);
                            }
                        } else {
                            getLog().debug("Ignoring artifact " + childNode.getArtifact() + " with scope " + childScope);
                        }
                    }
                }
            }
            return answer;
        }
        return null;
    }

    private synchronized Map<String, String> getAllServiceMixBundles() throws InterruptedException {
        if (servicemixBundles == null) {
            servicemixBundles =  doGetAllServiceMixBundles();
        }
        return servicemixBundles;
    }

    private Map<String, String> servicemixBundles;

    private Map<String, String> doGetAllServiceMixBundles() throws InterruptedException {
        getLog().info("Retrieving ServiceMix bundles on maven central");
        final Map<String, String> bundles = new HashMap<String, String>();
        final ExecutorService executor = Executors.newCachedThreadPool();
        try {
            String md = IOUtil.toString(new URL("http://central.maven.org/maven2/org/apache/servicemix/bundles/").openStream());
            Matcher matcher = Pattern.compile("<a href=\"(org\\.apache\\.servicemix\\.bundles\\.[^\"]*)/\">").matcher(md);
            while (matcher.find()) {
                final String artifactId = matcher.group(1);
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String mda = IOUtil.toString(new URL("http://central.maven.org/maven2/org/apache/servicemix/bundles/" + artifactId).openStream());
                            Matcher matcher = Pattern.compile("<a href=\"([^\\.][^\"]*)/\">").matcher(mda);
                            while (matcher.find()) {
                                final String version = matcher.group(1);
                                executor.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            String pom = IOUtil.toString(new URL("http://central.maven.org/maven2/org/apache/servicemix/bundles/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".pom").openStream());
                                            String pkgGroupId = extract(pom, "<pkgGroupId>(.*)</pkgGroupId>");
                                            String pkgArtifactId = extract(pom, "<pkgArtifactId>(.*)</pkgArtifactId>");
                                            String pkgVersion = extract(pom, "<pkgVersion>(.*)</pkgVersion>");
                                            if (pkgGroupId != null && pkgArtifactId != null && pkgVersion != null) {
                                                String key = pkgGroupId + ":" + pkgArtifactId + ":" + pkgVersion;
                                                getLog().info("Found ServiceMix bundle for " + key + " in version " + version);
                                                synchronized (bundles) {
                                                    String cur = bundles.get(key);
                                                    if (cur == null) {
                                                        bundles.put(key, "org.apache.servicemix.bundles:" + artifactId + ":" + version);
                                                    } else {
                                                        int v1 = extractBundleRelease(cur);
                                                        int v2 = extractBundleRelease(version);
                                                        if (v2 > v1) {
                                                            bundles.put(key, "org.apache.servicemix.bundles:" + artifactId + ":" + version);
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (IOException e) {
                                            getLog().warn("Error retrieving ServiceMix bundles list", e);
                                        }
                                    }
                                });
                            }
                        } catch (IOException e) {
                            getLog().warn("Error retrieving ServiceMix bundles list", e);
                        }
                    }
                });
            }
            executor.awaitTermination(5, TimeUnit.MINUTES);
        } catch (IOException e) {
            getLog().warn("Error retrieving ServiceMix bundles list", e);
        }
        return bundles;
    }

    private int extractBundleRelease(String version) {
        int i0 = version.lastIndexOf('_');
        int i1 = version.lastIndexOf('-');
        int i = Math.max(i0, i1);
        if (i > 0) {
            return Integer.parseInt(version.substring(i + 1));
        }
        return -1;
    }

    private String extract(String string, String regexp) {
        Matcher matcher = Pattern.compile(regexp).matcher(string);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * Returns true if this project builds a war
     */
    protected boolean isWarProject() {
        if (project != null) {
            String packaging = project.getPackaging();
            return packaging != null && packaging.equals("war");
        }
        return false;
    }

    protected void walkTree(DependencyNode node, int level) {
        if (node == null) {
            getLog().warn("Null node!");
            return;
        }
        getLog().info(indent(level) + node.getArtifact());
        List children = node.getChildren();
        for (Object child : children) {
            if (child instanceof DependencyNode) {
                walkTree((DependencyNode) child, level + 1);
            } else {
                getLog().warn("Unknown class " + child.getClass());
            }
        }
    }

    protected String indent(int level) {
        StringBuilder builder = new StringBuilder();
        while (level-- > 0) {
            builder.append("    ");
        }
        return builder.toString();
    }

    /**
     * Gets the artifact filter to use when resolving the dependency tree.
     *
     * @return the artifact filter
     */
    private ArtifactFilter createResolvingArtifactFilter() {
        ArtifactFilter filter;
        if (scope != null) {
            getLog().debug("+ Resolving dependency tree for scope '" + scope + "'");
            filter = new ScopeArtifactFilter(scope);
        } else {
            filter = null;
        }
        return filter;
    }

    protected void createAggregatedZip(List<MavenProject> reactorProjectList, File projectBaseDir, File projectBuildDir,
                                       String reactorProjectOutputPath, File projectOutputFile,
                                       boolean includeReadMe, List<MavenProject> pomZipProjects) throws IOException {
        projectBuildDir.mkdirs();

        for (MavenProject reactorProject : reactorProjectList) {
            // ignoreProject the execution root which just aggregates stuff
            if (!reactorProject.isExecutionRoot()) {
                Log log = getLog();
                combineProfileFilesToFolder(reactorProject, projectBuildDir, log, reactorProjectOutputPath);
            }
        }

        // we may want to include readme files for pom projects
        if (includeReadMe) {

            Map<String, File> pomNames = new HashMap<String, File>();

            for (MavenProject pomProjects : pomZipProjects) {
                File src = pomProjects.getFile().getParentFile();

                // must include first dir as prefix
                String root = projectBaseDir.getName();

                String relativePath = Files.getRelativePath(projectBaseDir, pomProjects.getBasedir());
                relativePath = root + File.separator + relativePath;

                // we must use dot instead of dashes in profile paths
                relativePath = pathToProfilePath(relativePath);

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
                        getLog().info("Replaced github links to fabric profiles in reaadme file: " + file);
                    }
                }
            }
        }

        Zips.createZipFile(getLog(), projectBuildDir, projectOutputFile);
        String relativePath = Files.getRelativePath(projectBaseDir, projectOutputFile);
        while (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        getLog().info("Created profile zip file: " + relativePath);
    }

    private static String pathToProfilePath(String path) {
        // TODO: use some common fabric util
        return path.replace('-', '.');
    }

    protected static File copyReadMe(File src, File profileBuildDir) throws IOException {
        File[] files = src.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase(Locale.ENGLISH).startsWith("readme.");
            }
        });
        if (files != null && files.length == 1) {
            File readme = files[0];
            File outFile = new File(profileBuildDir, readme.getName());
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
     * Replacing github links with fabric profiles links for our quickstarts
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
                    // need to ensure path is profile friendly
                    s2 = pathToProfilePath(s2);
                    if (relativePath != null && !"<root>".equals(relativePath)) {
                        s2 = addToPath(relativePath, s2);
                    }
                    // its a directory
                    matcher.appendReplacement(sb, "[$1](" + replaceReadmeLinksPrefix + s2 + ")");
                } else {
                    // need to ensure path is profile friendly
                    s2 = pathToProfilePath(s2);
                    if (relativePath != null && !"<root>".equals(relativePath)) {
                        s2 = addToPath(relativePath, s2);
                    }
                    // its a profile
                    matcher.appendReplacement(sb, "[$1](" + replaceReadmeLinksPrefix + s2 + ".profile)");
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

}
