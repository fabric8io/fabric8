/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.forge.devops.setup;

import io.fabric8.forge.addon.utils.CamelProjectHelper;
import io.fabric8.forge.addon.utils.MavenHelpers;
import io.fabric8.forge.addon.utils.VersionHelper;
import io.fabric8.utils.Strings;
import org.apache.maven.model.Model;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.maven.plugins.Configuration;
import org.jboss.forge.addon.maven.plugins.ConfigurationBuilder;
import org.jboss.forge.addon.maven.plugins.ConfigurationElement;
import org.jboss.forge.addon.maven.plugins.ConfigurationElementBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPlugin;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.projects.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class DockerSetupHelper {
    private static final transient Logger LOG = LoggerFactory.getLogger(DockerSetupHelper.class);

    public static final String DEFAULT_KARAF_IMAGE = "fabric8/karaf-2.4";
    public static final String DEFAULT_TOMCAT_IMAGE = "fabric8/tomcat-8.0";
    public static final String DEFAULT_WILDFLY_IMAGE = "jboss/wildfly:9.0.2.Final";
    public static final String DEFAULT_JAVA_IMAGE = "fabric8/java-jboss-openjdk8-jdk:1.0.10";
    public static final String S2I_JAVA_IMAGE = "fabric8/s2i-java:1.1.6";

    // see https://github.com/fabric8io/fabric8/issues/4160
    private static String dockerFromImagePrefix = "docker.io/";

    private static String[] jarImages = new String[]{DEFAULT_JAVA_IMAGE}; // s2i is not yet supported
    private static String[] bundleImages = new String[]{DEFAULT_KARAF_IMAGE};
    private static String[] warImages = new String[]{DEFAULT_TOMCAT_IMAGE, DEFAULT_WILDFLY_IMAGE};

    public static void setupDocker(Project project, String fromImage, String main) {
        MavenFacet maven = project.getFacet(MavenFacet.class);
        Model pom = maven.getModel();

        boolean springBoot = hasSpringBootMavenPlugin(project);
        String packaging = getProjectPackaging(project);
        boolean war = packaging != null && packaging.equals("war");
        boolean bundle = packaging != null && packaging.equals("bundle");
        boolean jar = packaging == null || packaging.equals("jar"); // jar is default packaging

        Map<String, String> envs = new LinkedHashMap<>();
        if (springBoot) {
            envs.put("JAR", "${project.artifactId}-${project.version}.war");
            envs.put("JAVA_OPTIONS", "-Djava.security.egd=/dev/./urandom");
        } else if (war) {
            envs.put("CATALINA_OPTS", "-javaagent:/opt/tomcat/jolokia-agent.jar=host=0.0.0.0,port=8778");
        } else if (jar && main != null) {
            // only include main for JAR deployment as WAR/bundle is container based
            envs.put("JAVA_MAIN_CLASS", main);
        }

        MavenPluginBuilder pluginBuilder;
        ConfigurationBuilder configurationBuilder;
        MavenPlugin plugin = MavenHelpers.findPlugin(project, "org.jolokia", "docker-maven-plugin");
        if (plugin != null) {
            // if there is an existing then leave it as-is
            LOG.info("Found existing docker-maven-plugin");
            pluginBuilder = null;
            configurationBuilder = null;
        } else {
            LOG.info("Adding docker-maven-plugin");
            pluginBuilder = MavenPluginBuilder.create()
                    .setCoordinate(MavenHelpers.createCoordinate("org.jolokia", "docker-maven-plugin", VersionHelper.dockerVersion()));
            configurationBuilder = ConfigurationBuilder.create(pluginBuilder);
            pluginBuilder.setConfiguration(configurationBuilder);
        }

        // if we are adding then we need to include some configurations as well
        if (pluginBuilder != null) {
            String commandShell = null;
            if (bundle) {
                commandShell = "/usr/bin/deploy-and-start";
            }
            setupDockerConfiguration(configurationBuilder, envs, commandShell, springBoot, war, bundle, jar);

            MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);
            pluginFacet.addPlugin(pluginBuilder);
        }

        setupDockerProperties(project, fromImage);
    }

    protected static void setupDockerConfiguration(ConfigurationBuilder config, Map<String, String> envs, String commandShell,
                                                   boolean springBoot, boolean war, boolean bundle, boolean jar) {
        ConfigurationElement images = MavenHelpers.getOrCreateElement(config, "images");
        ConfigurationElement image = MavenHelpers.getOrCreateElement(images, "image");
        ConfigurationElement build = MavenHelpers.getOrCreateElement(image, "build");
        ConfigurationElement env = MavenHelpers.getOrCreateElement(build, "env");
        for (Map.Entry<String, String> entry : envs.entrySet()) {
            ConfigurationElement cfg = ConfigurationElementBuilder.create().setName(entry.getKey()).setText(entry.getValue());
            env.getChildren().add(cfg);
        }
        if (Strings.isNotBlank(commandShell)) {
            ConfigurationElementBuilder shell = MavenHelpers.getOrCreateElementBuilder(build, "cmd", "shell");
            if (Strings.isNullOrBlank(shell.getText())) {
                MavenHelpers.asConfigurationElementBuilder(shell).setText(commandShell);
            }
        }

        ConfigurationElementBuilder from = MavenHelpers.getOrCreateElementBuilder(build, "from");
        if (Strings.isNullOrBlank(from.getText())) {
            from.setText("${docker.from}");
        }
        ConfigurationElementBuilder name = MavenHelpers.getOrCreateElementBuilder(image, "name");
        if (Strings.isNullOrBlank(name.getText())) {
            name.setText("${docker.image}");
        }

        ConfigurationElementBuilder assembly = MavenHelpers.getOrCreateElementBuilder(build, "assembly");
        if (springBoot || jar) {
            if (!assembly.hasChildByName("basedir")) {
                ConfigurationElementBuilder baseDir = MavenHelpers.getOrCreateElementBuilder(assembly, "basedir");
                baseDir.setText("/app");
            }
        }
        if (!assembly.hasChildByName("descriptor")) {
            ConfigurationElementBuilder descriptorRef = MavenHelpers.getOrCreateElementBuilder(assembly, "descriptorRef");
            if (Strings.isNullOrBlank(descriptorRef.getText())) {
                descriptorRef.setText("${docker.assemblyDescriptorRef}");
            }
        }
    }

    public static void setupDockerProperties(Project project, String fromImage) {
        String packaging = getProjectPackaging(project);

        boolean springBoot = hasSpringBootMavenPlugin(project);
        boolean war = packaging != null && packaging.equals("war");
        boolean bundle = packaging != null && packaging.equals("bundle");
        boolean jar = packaging != null && packaging.equals("jar");

        // update properties section in pom.xml
        MavenFacet maven = project.getFacet(MavenFacet.class);
        Model pom = maven.getModel();
        Properties properties = pom.getProperties();
        boolean updated = false;
        updated = MavenHelpers.updatePomProperty(properties, "docker.registryPrefix", "${env.DOCKER_REGISTRY}/", updated);
        if (Strings.isNotBlank(fromImage)) {
            updated = MavenHelpers.updatePomProperty(properties, "docker.from", dockerFromImagePrefix + fromImage, updated);
        }
        updated = MavenHelpers.updatePomProperty(properties, "docker.image", "${docker.registryPrefix}fabric8/${project.artifactId}:${project.version}", updated);
        // jolokia is exposed on our docker images on port 8778
        updated = MavenHelpers.updatePomProperty(properties, "docker.port.container.jolokia", "8778", updated);

        if (springBoot) {
            // spring-boot is packaged as war but runs as fat WARs
            updated = MavenHelpers.updatePomProperty(properties, "docker.assemblyDescriptorRef", "artifact", updated);
            updated = MavenHelpers.updatePomProperty(properties, "docker.port.container.http", "8080", updated);
        } else if (war) {
            // tomcat/jetty on port 8080
            updated = MavenHelpers.updatePomProperty(properties, "docker.assemblyDescriptorRef", "rootWar", updated);
            updated = MavenHelpers.updatePomProperty(properties, "docker.port.container.http", "8080", updated);
        } else if (bundle) {
            // karaf
            updated = MavenHelpers.updatePomProperty(properties, "docker.assemblyDescriptorRef", "artifact-with-dependencies", updated);
            updated = MavenHelpers.updatePomProperty(properties, "docker.port.container.http", "8181", updated);
        } else {
            updated = MavenHelpers.updatePomProperty(properties, "docker.assemblyDescriptorRef", "artifact-with-dependencies", updated);
        }

        // to save then set the model
        if (updated) {
            maven.setModel(pom);
        }
    }

    public static boolean hasSpringBootMavenPlugin(Project project) {
        if (project != null) {
            MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);
            Coordinate coor = CoordinateBuilder.create("org.springframework.boot:spring-boot-maven-plugin");
            return pluginFacet.hasPlugin(coor);
        }
        return false;
    }

    public static String defaultDockerImage(Project project) {
        String packaging = getProjectPackaging(project);
        if ("jar".equals(packaging)) {
            return jarImages[0];
        } else if ("bundle".equals(packaging)) {
            return bundleImages[0];
        } else if ("war".equals(packaging)) {
            // we have both tomcat or jboss
            return null;
        }
        return null;
    }

    private static String getProjectPackaging(Project project) {
        if (project != null) {
            MavenFacet maven = project.getFacet(MavenFacet.class);
            return maven.getModel().getPackaging();
        }
        return null;
    }

    public static boolean isJarImage(String fromImage) {
        // is required for jar images
        for (String jar : jarImages) {
            if (jar.equals(fromImage)) {
                return true;
            }
        }
        return false;
    }

    private static Coordinate createCoordinate(String groupId, String artifactId, String version) {
        CoordinateBuilder builder = CoordinateBuilder.create()
                .setGroupId(groupId)
                .setArtifactId(artifactId);
        if (version != null) {
            builder = builder.setVersion(version);
        }

        return builder;
    }

    /**
     * Tries to guess a good default main class to use based on the project.
     *
     * @return the suggested main class, or <tt>null</tt> if not possible to guess/find a good candidate
     */
    public static String defaultMainClass(Project project) {
        // try to guess a default main class
        MavenFacet maven = project.getFacet(MavenFacet.class);
        if (maven != null) {
            String answer = null;
            MavenPlugin plugin = MavenHelpers.findPlugin(project, "org.jolokia", "docker-maven-plugin");
            if (plugin != null) {
                Configuration config = plugin.getConfig();
                ConfigurationElement element = MavenHelpers.getConfigurationElement(config, "images", "image", "build", "env", "JAVA_MAIN_CLASS");
                if (element != null) {
                    answer = element.getText();
                }
            }

            if (Strings.isNullOrBlank(answer)) {
                Model pom = maven.getModel();
                if (pom != null) {
                    Properties properties = pom.getProperties();
                    answer = properties.getProperty("docker.env.MAIN");
                    if (Strings.isNullOrBlank(answer)) {
                        answer = properties.getProperty("fabric8.env.MAIN");
                    }
                }
            }
            if (Strings.isNotBlank(answer)) {
                return answer;
            }
        }

        // if camel-spring is on classpath
        if (CamelProjectHelper.findCamelCDIDependency(project) != null) {
            return "org.apache.camel.cdi.Main";
        } else if (CamelProjectHelper.findCamelSpringDependency(project) != null) {
            return "org.apache.camel.spring.Main";
        } else if (CamelProjectHelper.findCamelBlueprintDependency(project) != null) {
            return "org.apache.camel.test.blueprint.Main";
        }

        // TODO: what about spring-boot / docker-swarm??
        return null;
    }

}
