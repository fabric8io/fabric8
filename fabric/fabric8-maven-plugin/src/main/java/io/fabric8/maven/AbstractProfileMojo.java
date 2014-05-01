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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import io.fabric8.common.util.Strings;
import io.fabric8.deployer.dto.DependencyDTO;
import io.fabric8.deployer.dto.ProjectRequirements;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;

/**
 * Abstract base class for Profile based mojos
 */
public abstract class AbstractProfileMojo extends AbstractMojo {
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

    @Parameter(property = "localRepository", readonly = true, required = true)
    protected ArtifactRepository localRepository;

    @Parameter(property = "project.remoteArtifactRepositories")
    protected List<?> remoteRepositories;

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
     * The profile version to deploy to. If not specified then the current latest version is used.
     */
    @Parameter(property = "fabric8.version")
    private String version;

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

    protected static boolean isFile(File file) {
        return file != null && file.exists() && file.isFile();
    }

    protected static List<String> parameterToStringList(String parameterValue) {
        List<String> answer = new ArrayList<String>();
        if (Strings.isNotBlank(parameterValue)) {
            String[] split = parameterValue.split("\\s");
            if (split != null) {
                for (String text : split) {
                    if (Strings.isNotBlank(text)) {
                        answer.add(text);
                    }
                }
            }
        }
        return answer;
    }

    protected String readInput(String prompt) {
        while (true) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print(prompt);
            try {
                String line = reader.readLine();
                if (line != null && Strings.isNotBlank(line)) {
                    return line;
                }
            } catch (IOException e) {
                getLog().warn("Failed to read input: " + e, e);
            }
        }
    }

    protected void configureRequirements(ProjectRequirements requirements) throws MojoExecutionException {
        if (Strings.isNotBlank(profile)) {
            requirements.setProfileId(profile);
        } else {
            requirements.setProfileId(project.getGroupId() + "-" + project.getArtifactId());
        }
        if (Strings.isNotBlank(version)) {
            requirements.setVersion(version);
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
    }

    protected String defaultParentProfiles(ProjectRequirements requirements) throws MojoExecutionException {
        // TODO lets try figure out the best parent profile based on the project
        String packaging = project.getPackaging();
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
                URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
                Map<String, String> mainToProfileMap = getDefaultClassToProfileMap();

                Set<Map.Entry<String, String>> entries = mainToProfileMap.entrySet();
                for (Map.Entry<String, String> entry : entries) {
                    String mainClass = entry.getKey();
                    String profileName = entry.getValue();
                    if (hasClass(classLoader, mainClass)) {
                        getLog().info("Found class: " + mainClass + " so defaulting the parent profile: " + profileName);
                        return profileName;
                    }
                }
            } catch (MalformedURLException e) {
                getLog().warn("Failed to create URLClassLoader from files: " + files);
            }
            return "containers-java";
        }
        return "karaf";
    }

    protected Map<String, String> getDefaultClassToProfileMap() {
        Map<String,String> mainToProfileMap = new LinkedHashMap<String, String>();
        // TODO it'd be nice to find these automatically by querying the fabric itself for profiles
        // for the PID and "mainClass" value?
        mainToProfileMap.put("org.apache.camel.spring.Main", "containers-java.camel.spring");
        mainToProfileMap.put("org.osgi.framework.BundleContext", "containers-java.pojosr");
        mainToProfileMap.put("org.apache.camel.blueprint.ErrorHandlerType", "containers-java.pojosr");
        return mainToProfileMap;
    }

    protected boolean hasClass(URLClassLoader classLoader, String className) {
        try {
            Class<?> aClass = classLoader.loadClass(className);
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

    protected void addProjectArtifactBundle(ProjectRequirements requirements) {
        DependencyDTO rootDependency = requirements.getRootDependency();
        if (rootDependency != null) {
            String url = rootDependency.toBundleUrl();
            if (!requirements.getBundles().contains(url)) {
                requirements.getBundles().add(url);
            }
        }
    }
    protected DependencyDTO loadRootDependency() throws DependencyTreeBuilderException {
        ArtifactFilter artifactFilter = createResolvingArtifactFilter();
        DependencyNode dependencyNode = dependencyTreeBuilder.buildDependencyTree(project, localRepository, artifactFactory, metadataSource, artifactFilter, artifactCollector);
        return buildFrom(dependencyNode);
    }

    private DependencyDTO buildFrom(DependencyNode node) {
        Artifact artifact = node.getArtifact();
        if (artifact != null) {
            DependencyDTO answer = new DependencyDTO();
            answer.setGroupId(artifact.getGroupId());
            answer.setArtifactId(artifact.getArtifactId());
            answer.setVersion(artifact.getVersion());
            answer.setClassifier(artifact.getClassifier());
            answer.setScope(artifact.getScope());
            answer.setType(artifact.getType());
            answer.setOptional(artifact.isOptional());

            List children = node.getChildren();
            for (Object child : children) {
                if (child instanceof DependencyNode) {
                    DependencyNode childNode = (DependencyNode) child;
                    DependencyDTO childDTO = buildFrom(childNode);
                    answer.addChild(childDTO);
                }
            }
            return answer;
        }
        return null;
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
}
