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

import io.fabric8.maven.util.MavenUtils;
import org.eclipse.aether.AbstractForwardingRepositorySystemSession;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencyManager;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.collection.DependencyTraverser;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.util.artifact.DefaultArtifactTypeRegistry;
import org.eclipse.aether.util.graph.manager.ClassicDependencyManager;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.eclipse.aether.util.graph.transformer.ConflictMarker;
import org.eclipse.aether.util.graph.transformer.JavaDependencyContextRefiner;
import org.eclipse.aether.util.graph.traverser.FatArtifactTraverser;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;

/**
 * A simplistic repository system session that mimics Maven's behavior to help third-party developers that want to embed
 * Maven's dependency resolution into their own applications. <strong>Warning:</strong> This class is not intended for
 * usage by Maven plugins, those should always acquire the current repository system session via parameter injection.
 */
public class MavenRepositorySystemSession
    extends AbstractForwardingRepositorySystemSession {
    private DefaultRepositorySystemSession delegate;

    protected RepositorySystemSession getSession() {
        return delegate;
    }

    public DefaultRepositorySystemSession getDelegate() {
        return delegate;
    }

    /**
     * Creates a new Maven-like repository system session by initializing the session with values typical for
     * Maven-based resolution.
     */
    public MavenRepositorySystemSession() {
        delegate = new DefaultRepositorySystemSession();

        delegate.setMirrorSelector(new DefaultMirrorSelector());
        delegate.setAuthenticationSelector(new DefaultAuthenticationSelector());
        delegate.setProxySelector(MavenUtils.getProxySelector());
        delegate.setDependencyTraverser(new FatArtifactTraverser());
        delegate.setDependencyManager(new ClassicDependencyManager());

        DependencySelector depFilter = new AndDependencySelector(
            new ScopeDependencySelector("test", "provided"),
            new OptionalDependencySelector(),
            new ExclusionDependencySelector()
        );
        delegate.setDependencySelector(depFilter);

        DependencyGraphTransformer transformer = new ChainedDependencyGraphTransformer(
            new ConflictMarker(),
            new JavaDependencyContextRefiner()
//                new ConflictResolver(), TODO
        );
        delegate.setDependencyGraphTransformer(transformer);

        DefaultArtifactTypeRegistry stereotypes = new DefaultArtifactTypeRegistry();
        stereotypes.add(new DefaultArtifactType("pom"));
        stereotypes.add(new DefaultArtifactType("maven-plugin", "jar", "", "java"));
        stereotypes.add(new DefaultArtifactType("jar", "jar", "", "java"));
        stereotypes.add(new DefaultArtifactType("ejb", "jar", "", "java"));
        stereotypes.add(new DefaultArtifactType("ejb-client", "jar", "client", "java"));
        stereotypes.add(new DefaultArtifactType("test-jar", "jar", "tests", "java"));
        stereotypes.add(new DefaultArtifactType("javadoc", "jar", "javadoc", "java"));
        stereotypes.add(new DefaultArtifactType("java-source", "jar", "sources", "java", false, false));
        stereotypes.add(new DefaultArtifactType("war", "war", "", "java", false, true));
        stereotypes.add(new DefaultArtifactType("ear", "ear", "", "java", false, true));
        stereotypes.add(new DefaultArtifactType("rar", "rar", "", "java", false, true));
        stereotypes.add(new DefaultArtifactType("par", "par", "", "java", false, true));
        delegate.setArtifactTypeRegistry(stereotypes);

/*
        delegate.setIgnoreInvalidArtifactDescriptor(true);
        delegate.setIgnoreMissingArtifactDescriptor(true);
*/

        delegate.setConfigProperties(System.getProperties());
        delegate.setSystemProperties(System.getProperties());
    }

    void setRepositoryListener(RepositoryListener listener) {
        delegate.setRepositoryListener(listener);
    }

    void setLocalRepositoryManager(LocalRepositoryManager manager) {
        delegate.setLocalRepositoryManager(manager);
    }

    void setDependencySelector(DependencySelector selector) {
        delegate.setDependencySelector(selector);
    }

    void setOffline(boolean offline) {
        delegate.setOffline(offline);
    }
}
