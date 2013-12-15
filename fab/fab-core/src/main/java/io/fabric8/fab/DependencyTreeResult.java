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

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.List;

import org.fusesource.common.util.Filter;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.resolution.ArtifactResolutionException;

/**
 * Represents a tree of dependencies
 */
public class DependencyTreeResult {
    private final DependencyNode rootNode;
    private final MavenResolver resolver;
    private final Filter<Dependency> excludeDependencyFilter;
    private DependencyTree tree;

    public DependencyTreeResult(DependencyNode rootNode, MavenResolver resolver, Filter<Dependency> excludeDependencyFilter) {
        this.rootNode = rootNode;
        this.resolver = resolver;
        this.excludeDependencyFilter = excludeDependencyFilter;
    }

    public DependencyNode getRootNode() {
        return rootNode;
    }

    public DependencyTree getTree() throws MalformedURLException, ArtifactResolutionException {
        if (tree == null) {
            tree = DependencyTree.newInstance(getRootNode(), resolver, excludeDependencyFilter);
        }
        return tree;
    }

    public void dump(StringBuffer buffer) {
        displayTree(rootNode, "", buffer);
    }

    public String getTreeDescription() {
        StringBuffer buffer = new StringBuffer();
        dump(buffer);
        return buffer.toString();
    }

    protected void displayTree(DependencyNode node, String indent, StringBuffer buffer) {
        buffer.append(indent + node.getDependency()).append("\n");
        String childIndent = indent + "  ";
        for (DependencyNode child : node.getChildren()) {
            displayTree(child, childIndent, buffer);
        }
    }

    public void addVersions(Collection<String> versions, DependencyId artifactId) {
        addVersions(versions, rootNode, artifactId);
    }

    protected void addVersions(Collection<String> versions, DependencyNode node, DependencyId dependencyId) {
        DependencyId thatId = DependencyId.newInstance(node);
        if (dependencyId.equals(thatId)) {
            String version = node.getVersion().toString();
            versions.add(version);
        }
        List<DependencyNode> children = node.getChildren();
        for (DependencyNode child : children) {
            addVersions(versions, child, dependencyId);
        }
    }
}
