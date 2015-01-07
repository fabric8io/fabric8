/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.camel.commands.project;

import java.util.List;

import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.DependencyFacet;

public class CamelProjectHelper {

    public static Dependency findCamelCoreDependency(Project project) {
        List<Dependency> dependencies = project.getFacet(DependencyFacet.class).getEffectiveDependencies();
        for (Dependency d : dependencies) {
            if ("org.apache.camel".equals(d.getCoordinate().getGroupId()) && "camel-core".equals(d.getCoordinate().getArtifactId())) {
                return d;
            }
        }
        return null;
    }

    public static boolean hasDependency(Project project, String groupId, String artifactId, String version) {
        List<Dependency> dependencies = project.getFacet(DependencyFacet.class).getEffectiveDependencies();
        for (Dependency d : dependencies) {
            if (d.getCoordinate().getGroupId().equals(groupId)
                    && d.getCoordinate().getArtifactId().equals(artifactId)
                    && d.getCoordinate().getVersion().equals(version)) {
                return true;
            }
        }
        return false;
    }

}
