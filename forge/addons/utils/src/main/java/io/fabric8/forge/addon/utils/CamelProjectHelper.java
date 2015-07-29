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
package io.fabric8.forge.addon.utils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.DependencyFacet;

public class CamelProjectHelper {

    public static Dependency findCamelCoreDependency(Project project) {
        return findCamelArtifactDependency(project, "camel-core");
    }

    public static Dependency findCamelSpringDependency(Project project) {
        return findCamelArtifactDependency(project, "camel-spring");
    }

    public static Dependency findCamelCDIDependency(Project project) {
        return findCamelArtifactDependency(project, "camel-cdi");
    }

    public static Dependency findCamelBlueprintDependency(Project project) {
        return findCamelArtifactDependency(project, "camel-blueprint");
    }

    public static Dependency findCamelArtifactDependency(Project project, String artifactId) {
        List<Dependency> dependencies = project.getFacet(DependencyFacet.class).getEffectiveDependencies();
        for (Dependency d : dependencies) {
            if ("org.apache.camel".equals(d.getCoordinate().getGroupId()) && artifactId.equals(d.getCoordinate().getArtifactId())) {
                return d;
            }
        }
        return null;
    }

    public static Set<Dependency> findCamelArtifacts(Project project) {
        Set<Dependency> answer = new LinkedHashSet<Dependency>();

        List<Dependency> dependencies = project.getFacet(DependencyFacet.class).getEffectiveDependencies();
        for (Dependency d : dependencies) {
            if ("org.apache.camel".equals(d.getCoordinate().getGroupId())) {
                answer.add(d);
            }
        }
        return answer;
    }

    public static boolean hasDependency(Project project, String groupId, String artifactId) {
        return hasDependency(project, groupId, artifactId, null);
    }

    public static boolean hasDependency(Project project, String groupId, String artifactId, String version) {
        List<Dependency> dependencies = project.getFacet(DependencyFacet.class).getEffectiveDependencies();
        for (Dependency d : dependencies) {
            if (d.getCoordinate().getGroupId().equals(groupId) && d.getCoordinate().getArtifactId().equals(artifactId)) {
                if (version == null || d.getCoordinate().getVersion().equals(version)) {
                    return true;
                }
            }
        }
        return false;
    }

}
