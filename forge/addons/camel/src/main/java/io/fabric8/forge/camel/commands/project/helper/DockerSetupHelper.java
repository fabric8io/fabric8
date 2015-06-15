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
package io.fabric8.forge.camel.commands.project.helper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.maven.plugins.ConfigurationElement;
import org.jboss.forge.addon.maven.plugins.ConfigurationElementBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.projects.Project;

public class DockerSetupHelper {

    // see https://github.com/fabric8io/fabric8/issues/4160
    private static String dockerFromImagePrefix = "docker.io/";

    private static String[] jarImages = new String[]{"fabric8/java"};
    private static String[] bundleImages = new String[]{"fabric8/karaf-2.4"};
    private static String[] warImages = new String[]{"fabric8/tomcat-8.0", "jboss/wildfly"};

    public static void setupDocker(Project project, String fromImage, String main) {
        MavenPluginBuilder plugin = MavenPluginBuilder.create()
                .setCoordinate(createCoordinate("org.jolokia", "docker-maven-plugin", VersionHelper.dockerVersion()));

        ConfigurationElement cfgName = ConfigurationElementBuilder.create().setName("name").setText("${docker.image}");
        ConfigurationElement cfgFrom = ConfigurationElementBuilder.create().setName("from").setText("${docker.from}");
        ConfigurationElement cfgDescriptorRef = ConfigurationElementBuilder.create().setName("descriptorRef").setText("${docker.assemblyDescriptorRef}");

        ConfigurationElement cfgAssembly = ConfigurationElementBuilder.create().setName("assembly");
        cfgAssembly.getChildren().add(cfgDescriptorRef);

        ConfigurationElement cfgBuild = ConfigurationElementBuilder.create().setName("build");
        cfgBuild.getChildren().add(cfgFrom);
        cfgBuild.getChildren().add(cfgAssembly);

        Map<String, String> envs = new LinkedHashMap<>();

        boolean springBoot = hasSpringBootMavenPlugin(project);
        String packaging = getProjectPackaging(project);
        boolean war = packaging != null && packaging.equals("war");
        boolean bundle = packaging != null && packaging.equals("bundle");
        boolean jar = packaging != null && packaging.equals("jar");

        if (springBoot) {
            envs.put("JAR", "${project.artifactId}-${project.version}.war");
            envs.put("JAVA_OPTIONS", "-Djava.security.egd=/dev/./urandom");
        } else if (war) {
            // need jolokia-access workaround for jolokia jvm javaagent to be able to load the policy file from embedded in the ROOT.war file
            envs.put("CATALINA_OPTS", "-javaagent:/opt/tomcat/jolokia-agent.jar=host=0.0.0.0,port=8778,policyLocation=jar:file:///maven/ROOT.war!/WEB-INF/classes/jolokia-access.xml");
        } else if (jar && main != null) {
            // only include main for JAR deployment as WAR/bundle is container based
            envs.put("MAIN", main);
        }

        if (!envs.isEmpty()) {
            ConfigurationElement cfgEnv = ConfigurationElementBuilder.create().setName("env");
            cfgBuild.getChildren().add(cfgEnv);
            for (Map.Entry<String, String> env : envs.entrySet()) {
                ConfigurationElement cfg = ConfigurationElementBuilder.create().setName(env.getKey()).setText(env.getValue());
                cfgEnv.getChildren().add(cfg);
            }
        }

        if (bundle) {
            // need to add command config when using bundle/karaf
            ConfigurationElement cfgCommand = ConfigurationElementBuilder.create().setName("command").setText("/usr/bin/deploy-and-start");
            cfgBuild.getChildren().add(cfgCommand);
        }

        ConfigurationElement cfgImage = ConfigurationElementBuilder.create().setName("image");
        cfgImage.getChildren().add(cfgName);
        cfgImage.getChildren().add(cfgBuild);

        ConfigurationElement cfgImages = ConfigurationElementBuilder.create().setName("images");
        cfgImages.getChildren().add(cfgImage);

        setupDockerProperties(project, fromImage);

        // add docker-maven-plugin using latest version
        MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);
        plugin.createConfiguration().addConfigurationElement(cfgImages);
        pluginFacet.addPlugin(plugin);
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
        properties.put("docker.registryPrefix", "${env.DOCKER_REGISTRY}/");
        properties.put("docker.from", dockerFromImagePrefix + fromImage);
        properties.put("docker.image", "${docker.registryPrefix}fabric8/${project.artifactId}:${project.version}");
        // jolokia is exposed on our docker images on port 8778
        properties.put("docker.port.container.jolokia", "8778");

        if (springBoot) {
            // spring-boot is packaged as war but runs as fat WARs
            properties.put("docker.assemblyDescriptorRef", "artifact");
            properties.put("docker.port.container.http", "8080");
        } else if (war) {
            // tomcat/jetty on port 8080
            properties.put("docker.assemblyDescriptorRef", "rootWar");
            properties.put("docker.port.container.http", "8080");
        } else if (bundle) {
            // karaf
            properties.put("docker.assemblyDescriptorRef", "artifact-with-dependencies");
            properties.put("docker.port.container.http", "8181");
        } else {
            properties.put("docker.assemblyDescriptorRef", "artifact-with-dependencies");
        }

        // to save then set the model
        maven.setModel(pom);
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

        // if camel-spring is on classpath
        if (CamelProjectHelper.findCamelCDIDependency(project) != null) {
            return "org.apache.camel.cdi.Main";
        } else if (CamelProjectHelper.findCamelSpringDependency(project) != null) {
            return "org.apache.camel.spring.Main";
        } else if (CamelProjectHelper.findCamelBlueprintDependency(project) != null) {
            return "org.apache.camel.test.blueprint.Main";
        }

        // TODO: what about camel-spring-boot ?

        return null;
    }

}
