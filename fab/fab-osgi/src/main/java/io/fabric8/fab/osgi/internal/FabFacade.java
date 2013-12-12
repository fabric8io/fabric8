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

package io.fabric8.fab.osgi.internal;

import java.io.File;
import java.io.IOException;

import io.fabric8.fab.DependencyTree;
import io.fabric8.fab.MavenResolver;
import io.fabric8.fab.PomDetails;
import io.fabric8.fab.VersionedDependencyId;
import org.fusesource.common.util.Filter;
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

    VersionedDependencyId getVersionedDependencyId() throws IOException;

    String getProjectDescription();

    DependencyTree collectDependencyTree(boolean offline, Filter<Dependency> excludeDependencyFilter) throws RepositoryException, IOException;

    /**
     * Lets convert the version to a version range depending on the default or FAB specific version range value
     */
    String toVersionRange(String version);

    /**
     * Check if a given dependency tree item is already installed in the facade's context
     *
     * @param tree the dependency tree
     * @return <code>true</code> is the dependency is already installed
     */
    boolean isInstalled(DependencyTree tree);


}
