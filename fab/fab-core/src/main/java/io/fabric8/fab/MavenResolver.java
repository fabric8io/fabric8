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

import java.io.File;
import java.io.IOException;

import org.fusesource.common.util.Filter;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.resolution.ArtifactResolutionException;

public interface MavenResolver {

    File resolveFile(Artifact artifact) throws ArtifactResolutionException;

    DependencyTreeResult collectDependencies(PomDetails details, boolean offline, Filter<Dependency> excludeDependencyFilter) throws IOException, RepositoryException;

    DependencyTreeResult collectDependencies(VersionedDependencyId id, boolean offline, Filter<Dependency> excludeDependencyFilter) throws IOException, RepositoryException;

    PomDetails findPomFile(File fileJar) throws IOException;


}
