/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.internal;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.fusesource.fabric.fab.DependencyTree;
import org.fusesource.fabric.fab.MavenResolver;
import org.fusesource.fabric.fab.PomDetails;
import org.fusesource.fabric.fab.VersionedDependencyId;
import org.fusesource.fabric.fab.util.Filter;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.graph.Dependency;

/**
 * Represents a facade to either a jar being deployed or a bundle already installed
 */
public interface FabFacade {

    File getJarFile() throws IOException;

    Configuration getConfiguration();

    PomDetails resolvePomDetails() throws IOException;

    MavenResolver getResolver();

    boolean isIncludeSharedResources();

    VersionedDependencyId getVersionedDependencyId() throws IOException, XmlPullParserException;

    String getProjectDescription();

    DependencyTree collectDependencyTree(boolean offline, Filter<Dependency> excludeDependencyFilter) throws RepositoryException, IOException, XmlPullParserException;

    /**
     * Lets convert the version to a version range depending on the default or FAB specific version range value
     */
    String toVersionRange(String version);
}
