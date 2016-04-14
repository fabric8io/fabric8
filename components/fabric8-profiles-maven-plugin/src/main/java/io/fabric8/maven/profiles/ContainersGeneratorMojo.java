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
package io.fabric8.maven.profiles;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import io.fabric8.profiles.Profiles;
import io.fabric8.profiles.containers.Containers;
import io.fabric8.profiles.containers.JenkinsfileReifier;
import io.fabric8.profiles.containers.ProjectReifier;
import io.fabric8.profiles.containers.karaf.KarafProjectReifier;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Generates all containers, run on updates to the Profiles repository.
 */
@Mojo(name = "generate", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    defaultPhase = LifecyclePhase.GENERATE_SOURCES)
//@Execute(lifecycle = "fabric8-profiles", phase = LifecyclePhase.GENERATE_SOURCES)
public class ContainersGeneratorMojo extends AbstractProfilesMojo {

    /**
     * Reifier map, defaults to Karaf reifier for container type karaf.
     */
    @Parameter(readonly = false, required = false)
    protected Map<String, String> reifierMap;

    /**
     * Property map for reifiers, mapping container names to property maps.
     */
    @Parameter(readonly = false, required = false)
    protected Map<String, Properties> reifierProperties;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        // initialize inherited fields
        super.execute();

        // populate default reifiers if not set
        final Map<String, ProjectReifier> reifiers = new HashMap<>();
        if (reifierMap == null || reifierMap.isEmpty()) {

            // configure with default karaf container reifier
            final Properties defaultProps = new Properties(profilesProperties);
            if (reifierProperties != null) {
                final Properties map = reifierProperties.get(KarafProjectReifier.CONTAINER_TYPE);
                if (map != null) {
                    defaultProps.putAll(map);
                }
            }

            // add karaf and jenkins reifiers
            reifiers.put(KarafProjectReifier.CONTAINER_TYPE, new KarafProjectReifier(defaultProps));
            reifiers.put(JenkinsfileReifier.CONTAINER_TYPE, new JenkinsfileReifier(defaultProps));

        } else {

            // load reifiers from project dependencies
            final ClassLoader classLoader = getProjectClassLoader();
            for (Map.Entry<String, String> entry : reifierMap.entrySet()) {
                final String type = entry.getKey();
                final String className = entry.getValue();

                final Properties properties = new Properties(profilesProperties);
                if (reifierProperties != null) {
                    final Properties map = reifierProperties.get(type);
                    if (map != null) {
                        properties.putAll(map);
                    }
                }
                try {
                    final Class<?> aClass = classLoader.loadClass(className);
                    final Class<? extends ProjectReifier> reifierClass = aClass.asSubclass(ProjectReifier.class);
                    final Constructor<? extends ProjectReifier> constructor = reifierClass.getConstructor(Properties.class);
                    reifiers.put(KarafProjectReifier.CONTAINER_TYPE, constructor.newInstance(properties));
                } catch (ClassCastException e) {
                    throwMojoException("Class is not of type ProjectReifier", className, e);
                } catch (ReflectiveOperationException e) {
                    throwMojoException("Error loading ProjectReifier", className, e);
                }
            }
        }

        // create containers utility
        final Containers containers = new Containers(configs, reifiers, new Profiles(profiles));

        // list all containers and generate under targetDirectory
        final Path target = Paths.get(targetDirectory.getAbsolutePath());
        try {
            final List<Path> names = Files.list(configs.resolve("containers"))
                .filter(p -> p.getFileName().toString().endsWith(".cfg"))
                .collect(Collectors.toList());

            // generate all containers
            generateContainers(containers, target, names);

        } catch (IOException e) {
            throwMojoException("Error generating containers", e.getMessage(), e);
        }
    }

    /**
     * Allow overriding to do something sophisticated, for example,
     * check what changed in git log from last build to only build containers whose profiles changed.
     */
    protected void generateContainers(Containers containers, Path target, List<Path> names) throws IOException {
        for (Path path : names) {
            final String configFileName = path.getFileName().toString();
            final String name = configFileName.substring(0, configFileName.lastIndexOf('.'));

            // create target if it doesn't exist
            final Path targetDir = target.resolve(name);
            Files.createDirectories(targetDir);

            containers.reify(targetDir, name);
        }
    }

    protected ClassLoader getProjectClassLoader() throws MojoExecutionException {
        final List classpathElements;
        try {
            classpathElements = project.getRuntimeClasspathElements();
        } catch (org.apache.maven.artifact.DependencyResolutionRequiredException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        final URL[] urls = new URL[classpathElements.size()];
        int i = 0;
        for (Iterator it = classpathElements.iterator(); it.hasNext(); i++) {
            try {
                urls[i] = new File((String) it.next()).toURI().toURL();
                log.debug("Adding project path " + urls[i]);
            } catch (MalformedURLException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }

        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        return new URLClassLoader(urls, tccl != null ? tccl : getClass().getClassLoader());
    }
}
