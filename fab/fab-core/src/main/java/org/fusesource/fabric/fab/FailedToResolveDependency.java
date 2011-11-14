/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab;

import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.resolution.ArtifactResolutionException;

/**
 * An exception thrown if a dependency cannot resolve its dependents
 */
public class FailedToResolveDependency extends RepositoryException {
    private final Dependency dependency;

    public FailedToResolveDependency(Dependency dependency, Exception cause) {
        super("Failed to resolve dependency of " + dependency.getArtifact() + ". " + cause.getMessage(), cause);
        this.dependency = dependency;
    }

    public Dependency getDependency() {
        return dependency;
    }
}
