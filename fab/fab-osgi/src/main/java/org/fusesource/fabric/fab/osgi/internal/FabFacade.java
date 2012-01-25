/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
