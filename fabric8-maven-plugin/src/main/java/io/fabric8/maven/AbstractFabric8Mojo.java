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

import io.fabric8.maven.support.JsonSchema;
import io.fabric8.maven.support.JsonSchemas;
import io.fabric8.utils.Strings;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static io.fabric8.utils.PropertiesHelper.findPropertiesWithPrefix;

/**
 * Abstract base class for Fabric8 based Mojos
 */
public abstract class AbstractFabric8Mojo extends AbstractMojo {

    /**
     * Name of the created app zip file
     */
    @Parameter(property = "fabric8.zip.file", defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}-app.zip")
    protected File zipFile;

    /**
     * The folder used for defining project specific files
     */
    @Parameter(property = "appConfigDir", defaultValue = "${basedir}/src/main/fabric8")
    protected File appConfigDir;


    /**
     * The generated kubernetes JSON file
     */
    @Parameter(property = "kubernetesJson", defaultValue = "${basedir}/target/classes/kubernetes.json")
    private File kubernetesJson;

    /**
     * The source kubernetes JSON file
     */
    @Parameter(property = "kubernetesSourceJson", defaultValue = "${basedir}/src/main/fabric8/kubernetes.json")
    protected File kubernetesSourceJson;

    @Component
    private MavenProject project;

    /**
     * The number of replicas of this container if we are auto generating the kubernetes JSON file (creating
     * a <a href="http://fabric8.io/v2/replicationControllers.html">Replication Controller</a> if this value
     * is greater than 0 or a <a href="http://fabric8.io/v2/pods.html">pod</a> if not).
     */
    @Parameter(property = "fabric8.replicas", defaultValue = "1")
    private Integer replicas;


    /**
     * Whether or not we should ignoreProject this maven project from goals like fabric8:deploy
     */
    @Parameter(property = "fabric8.ignoreProject", defaultValue = "false")
    private boolean ignoreProject;

    protected static URLClassLoader createURLClassLoader(Collection<URL> jars) {
        return new URLClassLoader(jars.toArray(new URL[jars.size()]));
    }

    public File getKubernetesJson() {
        return kubernetesJson;
    }

    public MavenProject getProject() {
        return project;
    }

    public Integer getReplicas() {
        return replicas;
    }

    public boolean isIgnoreProject() {
        return ignoreProject;
    }

    public File getZipFile() {
        return zipFile;
    }

    /**
     * Returns true if this project is a pom packaging project
     */
    protected boolean isPom(MavenProject reactorProject) {
        return "pom".equals(reactorProject.getPackaging());
    }

    protected InputStream loadPluginResource(String iconRef) throws MojoExecutionException {
        InputStream answer = Thread.currentThread().getContextClassLoader().getResourceAsStream(iconRef);
        if (answer == null) {
            answer = getTestClassLoader().getResourceAsStream(iconRef);
        }
        if (answer == null) {
            answer = this.getClass().getResourceAsStream(iconRef);
        }
        return answer;
    }

    protected URLClassLoader getCompileClassLoader() throws MojoExecutionException {
        try {
            List<String> classpathElements = getProject().getCompileClasspathElements();
            return createClassLoader(classpathElements, getProject().getBuild().getOutputDirectory());
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to resolve classpath: " + e, e);
        }
    }

    protected URLClassLoader getTestClassLoader() throws MojoExecutionException {
        try {
            List<String> classpathElements = getProject().getTestClasspathElements();
            return createClassLoader(classpathElements, getProject().getBuild().getTestOutputDirectory());
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to resolve classpath: " + e, e);
        }
    }

    protected URLClassLoader createClassLoader(List<String> classpathElements, String... paths) throws MalformedURLException {
        List<URL> urls = new ArrayList<>();
        for (String path : paths) {
            URL url = pathToUrl(path);
            urls.add(url);
        }
        for (Object object : classpathElements) {
            if (object != null) {
                String path = object.toString();
                URL url = pathToUrl(path);
                urls.add(url);
            }
        }
        getLog().debug("Creating class loader from: " + urls);
        return createURLClassLoader(urls);
    }

    private URL pathToUrl(String path) throws MalformedURLException {
        File file = new File(path);
        return file.toURI().toURL();
    }

    protected boolean hasConfigDir() {
        return appConfigDir.isDirectory();
    }

    protected boolean isPomProject() {
        return isPom(getProject());
    }

    protected boolean shouldGenerateForThisProject() {
        return !isPomProject() || hasConfigDir();
    }

    /**
     * Returns all the environment variable properties defined in the pom.xml which are prefixed with "fabric8.env."
     */
    public Map<String, String> getEnvironmentVariableProperties() {
        return findPropertiesWithPrefix(getProject().getProperties(), "fabric8.env.", Strings.toEnvironmentVariableFunction());
    }

    public JsonSchema getEnvironmentVariableJsonSchema() throws IOException, MojoExecutionException {
        JsonSchema schema = JsonSchemas.loadEnvironmentSchemas(getCompileClassLoader(), getProject().getBuild().getOutputDirectory());
        if (schema == null) {
            getLog().info("No environment schemas found for file: " + JsonSchemas.ENVIRONMENT_SCHEMA_FILE);
            schema = new JsonSchema();
        }
        Map<String, String> envs = getEnvironmentVariableProperties();
        JsonSchemas.addEnvironmentVariables(schema, envs);
        return schema;
    }
}
