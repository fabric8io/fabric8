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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;

public class CompareDependencyNode {

    private final DependencyNode node1;
    private final DependencyNode node2;

    public CompareDependencyNode(DependencyNode node1, DependencyNode node2) {
        this.node1 = node1;
        this.node2 = node2;
        if (node1 == null && node2 == null) {
            throw new IllegalArgumentException("Should have either node1 or node2 specified!");
        }
    }

    /**
     * Returns either the first or second defined node
     *
     * @return
     */
    private DependencyNode getNode() {
        return node1 != null ? node1 : node2;
    }

    public String groupId() {
        return Aether.groupId(getNode());
    }

    public String artifactId() {
        return Aether.artifactId(getNode());
    }

    public String extension() {
        return Aether.extension(getNode());
    }

    public String classifier() {
        return Aether.classifier(getNode());
    }

    public String getVersion1() {
        return node1 != null ? Aether.version(node1) : null;
    }

    public String getVersion2() {
        return node2 != null ? Aether.version(node2) : null;
    }

    public Artifact artifact() {
        return Aether.artifact(getNode());
    }

    public Dependency getDependency() {
        return getNode().getDependency();
    }

    public boolean isOptional() {
        return getDependency().isOptional();
    }

    public String getScope() {
        return getDependency().getScope();
    }

    public List<CompareDependencyNode> createChildren() {
        Map<String, DependencyNode> m1 = toDependencyMap(node1);
        Map<String, DependencyNode> m2 = toDependencyMap(node2);

        LinkedList<CompareDependencyNode> result = new LinkedList<CompareDependencyNode>();
        Set<String> keySet = new HashSet<String>();
        keySet.addAll(m1.keySet());
        keySet.addAll(m2.keySet());

        for (String key : keySet) {
            result.add(new CompareDependencyNode(m1.get(key), m2.get(key)));
        }

        return result;
    }

    private Map<String, DependencyNode> toDependencyMap(DependencyNode optionalNode) {
        List<DependencyNode> children = Collections.emptyList();
        if (optionalNode != null) {
            children = optionalNode.getChildren();
        }

        Map<String, DependencyNode> result = new HashMap<String, DependencyNode>();
        for (DependencyNode c : children) {
            result.put(Aether.groupId(c) + ":" + Aether.artifactId(c), c);
        }
        return result;
    }

    public VersionChange change() {
        String v1 = getVersion1();
        String v2 = getVersion2();
        if (v1 != null) {
            if (v2 != null) {
                if (v1.equals(v2)) {
                    return new SameVersion(v1);
                } else {
                    return new UpdateVersion(v1, v2);
                }
            } else {
                return new AddVersion(v1);
            }
        } else {
            if (v2 != null) {
                return new RemoveVersion(v2);
            } else {
                throw new IllegalArgumentException("Neither version has a value!");
            }
        }
    }

}
