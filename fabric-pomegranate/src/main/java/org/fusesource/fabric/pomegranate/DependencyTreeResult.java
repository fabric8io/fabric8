/**
 *
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
package org.fusesource.fabric.pomegranate;

import org.sonatype.aether.graph.DependencyNode;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.List;

/**
 * Represents a tree of dependencies
 */
public class DependencyTreeResult {
    private final DependencyNode rootNode;
    private DependencyTree tree;

    public DependencyTreeResult(DependencyNode rootNode) {
        this.rootNode = rootNode;
    }

    public DependencyNode getRootNode() {
        return rootNode;
    }

    public DependencyTree getTree() throws MalformedURLException {
        if (tree == null) {
            tree = DependencyTree.newInstance(getRootNode());
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
            versions.add(node.getVersion().toString());
        }
        List<DependencyNode> children = node.getChildren();
        for (DependencyNode child : children) {
            addVersions(versions, child, dependencyId);
        }
    }
}
