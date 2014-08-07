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
package io.fabric8.fab;

import io.fabric8.common.util.Filter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;

/**
 * Matches a {@link Dependency} using a groupId and artifactId filter
 */
public class DependencyFilter implements Filter<Dependency> {

    private final Filter<String> groupFilter;
    private final Filter<String> artifactFilter;

    public DependencyFilter(Filter<String> groupFilter, Filter<String> artifactFilter) {
        this.groupFilter = groupFilter;
        this.artifactFilter = artifactFilter;
    }

    @Override
    public boolean matches(Dependency dependencyTree) {
        Artifact artifact = dependencyTree.getArtifact();
        String groupId = artifact.getGroupId();
        String artifactId = artifact.getArtifactId();
        return groupFilter.matches(groupId) && artifactFilter.matches(artifactId);
    }

    @Override
    public String toString() {
        return "DependencyFilter(" + groupFilter + ":" + artifactFilter + ")";
    }
}
