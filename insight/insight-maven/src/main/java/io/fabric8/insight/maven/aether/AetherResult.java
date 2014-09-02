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
package io.fabric8.insight.maven.aether;

import java.io.File;
import java.util.List;

import org.eclipse.aether.graph.DependencyNode;

public class AetherResult implements AetherJarOrPom {

    private DependencyNode root;
    private List<File> resolvedFiles;
    private String resolvedClassPath;

    public AetherResult(DependencyNode root, List<File> resolvedFiles, String resolvedClassPath) {
        this.root = root;
        this.resolvedFiles = resolvedFiles;
        this.resolvedClassPath = resolvedClassPath;
    }

    @Override
    public void dump() {
        System.out.println("tree: " + tree());
    }

    @Override
    public String tree() {
        StringBuffer dump = new StringBuffer();
        displayTree(root, "", dump);
        return dump.toString();
    }

    @Override
    public DependencyNode root() {
        return this.root;
    }

    protected void displayTree(DependencyNode node, String indent, StringBuffer sb) {
        sb.append(indent).append(node.getDependency()).append(Aether.LINE_SEPARATOR);
        String childIndent = indent + "  ";
        for (DependencyNode child: node.getChildren()) {
            displayTree(child, childIndent, sb);
        }
    }

    public DependencyNode getRoot() {
        return root;
    }

    public List<File> getResolvedFiles() {
        return resolvedFiles;
    }

    public String getResolvedClassPath() {
        return resolvedClassPath;
    }

}
