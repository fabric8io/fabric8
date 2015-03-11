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
package io.fabric8.forge.camel.commands.project;

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

        if (main != null) {
            ConfigurationElement cfgMain = ConfigurationElementBuilder.create().setName("MAIN").setText("${docker.env.MAIN}");
            ConfigurationElement cfgEnv = ConfigurationElementBuilder.create().setName("env");
            cfgEnv.getChildren().add(cfgMain);
            cfgBuild.getChildren().add(cfgMain);
        }

        ConfigurationElement cfgImage = ConfigurationElementBuilder.create().setName("image");
        cfgImage.getChildren().add(cfgName);
        cfgImage.getChildren().add(cfgBuild);

        ConfigurationElement cfgImages = ConfigurationElementBuilder.create().setName("images");
        cfgImages.getChildren().add(cfgImage);

        String packaging = getProjectPackaging(project);

        boolean war = packaging != null && packaging.equals("war");
        boolean bundle = packaging != null && packaging.equals("bundle");
        boolean jar = packaging != null && packaging.equals("jar");

        // update properties section in pom.xml
        MavenFacet maven = project.getFacet(MavenFacet.class);
        Model pom = maven.getModel();
        Properties properties = pom.getProperties();
        properties.put("docker.registryPrefix", "${env.DOCKER_REGISTRY}/");
        properties.put("docker.from", fromImage);
        properties.put("docker.image", "${docker.registryPrefix}fabric8/${project.artifactId}:${project.version}");
        properties.put("docker.port.container.jolokia", "8778");
        if (war) {
            properties.put("docker.assemblyDescriptorRef", "rootWar");
            properties.put("docker.port.container.http", "8080");
        } else if (bundle) {
            properties.put("docker.assemblyDescriptorRef", "artifact-with-dependencies");
            properties.put("docker.port.container.http", "8181");
        } else {
            properties.put("docker.assemblyDescriptorRef", "artifact-with-dependencies");
        }

        // to save then set the model
        maven.setModel(pom);

        // add docker-maven-plugin using latest version
        MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);
        plugin.createConfiguration().addConfigurationElement(cfgImages);
        pluginFacet.addPlugin(plugin);
    }

    public static String defaultDockerImage(Project project) {
        String packaging = getProjectPackaging(project);
        if ("jar".equals(packaging)) {
            return jarImages[0];
        } else if ("bundle".equals(packaging)) {
            return bundleImages[0];
        } else if ("war".equals(packaging)) {
            return warImages[0];
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

    private static Coordinate createCoordinate(String groupId, String artifactId, String version) {
        CoordinateBuilder builder = CoordinateBuilder.create()
                .setGroupId(groupId)
                .setArtifactId(artifactId);
        if (version != null) {
            builder = builder.setVersion(version);
        }

        return builder;
    }


}
