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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.collection.DependencyGraphTransformationContext;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.graph.DependencyNode;

public class DuplicateTransformer implements DependencyGraphTransformer {
    public DependencyNode transformGraph(DependencyNode node, DependencyGraphTransformationContext context) throws RepositoryException {
        Set<DependencyNode> visited = new TreeSet<>(DependencyNodeComparator.INSTANCE);
        visit(node, visited);
        return node;
    }

    private void visit(DependencyNode node, Set<DependencyNode> visited) {
        List<DependencyNode> newChildren = new ArrayList<>();
        for (DependencyNode childNode : node.getChildren()) {
            if (visited.add(childNode)) {
                newChildren.add(childNode);
                visit(childNode, visited);
            }
        }
        node.setChildren(newChildren);
    }
}
