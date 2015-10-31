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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.DependencyFacet;

public class SetupProjectHelper {

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

}
