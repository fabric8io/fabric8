/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.fab;

import org.fusesource.common.util.Filter;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.Dependency;

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
