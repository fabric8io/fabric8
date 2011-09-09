/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab;

import org.fusesource.fabric.fab.util.Filter;
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
