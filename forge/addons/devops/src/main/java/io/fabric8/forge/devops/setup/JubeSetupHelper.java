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

import io.fabric8.forge.addon.utils.VersionHelper;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.maven.plugins.ExecutionBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;

public class JubeSetupHelper {

    public static void setupJube(DependencyInstaller dependencyInstaller, Project project, String fromImage) {
        MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);

        // add jube plugin
        MavenPluginBuilder plugin = MavenPluginBuilder.create()
                .setCoordinate(createCoordinate("io.fabric8.jube", "jube-maven-plugin", VersionHelper.jubeVersion(), null, null));
        plugin.addExecution(ExecutionBuilder.create().addGoal("build").setPhase("package"));
        pluginFacet.addPlugin(plugin);

        // TODO: the jube plugin can auto download the image, so we may not need to include this in the future
        // install jube image
        String jubeImage = asJubeImage(fromImage);
        Dependency bom = DependencyBuilder.create()
                .setCoordinate(createCoordinate("io.fabric8.jube.images.fabric8", jubeImage, VersionHelper.jubeVersion(), "image", "zip"));
        dependencyInstaller.installManaged(project, bom);
    }

    private static Coordinate createCoordinate(String groupId, String artifactId, String version, String classifier, String type) {
        CoordinateBuilder builder = CoordinateBuilder.create()
                .setGroupId(groupId)
                .setArtifactId(artifactId);
        if (version != null) {
            builder = builder.setVersion(version);
        }
        if (classifier != null) {
            builder = builder.setClassifier(classifier);
        }
        if (type != null) {
            builder = builder.setPackaging(type);
        }

        return builder;
    }

    private static String asJubeImage(String fromImage) {
        int idx = fromImage.indexOf('/');
        if (idx > 0) {
            return fromImage.substring(idx + 1);
        }
        return fromImage;
    }

}
