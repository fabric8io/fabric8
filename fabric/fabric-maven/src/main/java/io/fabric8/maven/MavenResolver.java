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
package io.fabric8.maven;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.fabric8.common.util.Filter;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;

public interface MavenResolver {

    /**
     * Access to the RepositorySystem
     */
    RepositorySystem getRepositorySystem();

    /**
     * Create a new session
     */
    RepositorySystemSession createSession();

    /**
     * Retrieve the list of configured repositories
     */
    List<RemoteRepository> getRepositories();

    /**
     * Resolve and download a maven based url
     */
    File download(String url) throws IOException;

    /**
     * Resolve and download an artifact
     */
    File resolveFile(Artifact artifact) throws IOException;

    /**
     * Build a tree of dependencies for the specified jar file
     */
    DependencyNode collectDependenciesForJar(File artifactFile, Filter<Dependency> excludeFilter) throws IOException, RepositoryException;

}
