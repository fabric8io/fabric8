/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.etcd.core;

import io.fabric8.etcd.api.Builder;
import io.fabric8.etcd.api.Node;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class ImmutableNode implements Node<ImmutableNode> {

    public static class NodeBuilder implements Builder<ImmutableNode> {

        private String key;
        private long createdIndex;
        private long modifiedIndex;
        private String value;

        private String expiration;
        private int ttl;

        private Set<ImmutableNode> nodes = new LinkedHashSet<ImmutableNode>();
        private boolean dir;

        public NodeBuilder key(String key) {
            this.key = key;
            return this;
        }

        public NodeBuilder createIndex(long createdIndex) {
            this.createdIndex = createdIndex;
            return this;
        }

        public NodeBuilder modifiedIndex(long modifiedIndex) {
            this.modifiedIndex = modifiedIndex;
            return this;
        }

        public NodeBuilder value(String value) {
            this.value = value;
            return this;
        }

        public NodeBuilder expiration(String expiration) {
            this.expiration = expiration;
            return this;
        }

        public NodeBuilder ttl(int ttl) {
            this.ttl = ttl;
            return this;
        }

        public NodeBuilder nodes(Set<ImmutableNode> nodes) {
            this.nodes = nodes;
            return this;
        }

        public NodeBuilder addNode(ImmutableNode node) {
            this.nodes.add(node);
            return this;
        }

        NodeBuilder dir(boolean dir) {
            this.dir = dir;
            return this;
        }

        @Override
        public ImmutableNode build() {
            return new ImmutableNode(key, createdIndex, modifiedIndex, value, expiration, ttl, nodes, nodes.size() > 0 || dir);
        }
    }

    public static NodeBuilder builder() {
        return new NodeBuilder();
    }

    private final String key;
    private final long createdIndex;
    private final long modifiedIndex;
    private final String value;

    private final String expiration;
    private final int ttl;

    private final Set<ImmutableNode> nodes;
    private final boolean dir;

    public ImmutableNode(Node<?> node) {
        this.key = node.getKey();
        this.createdIndex = node.getCreatedIndex();
        this.modifiedIndex = node.getModifiedIndex();
        this.value = node.getValue();
        this.expiration = node.getExpiration();
        this.ttl = node.getTtl();
        Set<ImmutableNode> transformedNodes = new LinkedHashSet<>();
        if (node.getNodes() != null) {
            for (Node n : node.getNodes()) {
                transformedNodes.add(new ImmutableNode(n));
            }
        }
        this.nodes = Collections.unmodifiableSet(transformedNodes);
        this.dir = node.isDir();
    }

    public ImmutableNode(String key, long createdIndex, long modifiedIndex, String value, String expiration, int ttl, Set<ImmutableNode> nodes, boolean dir) {
        this.key = key;
        this.createdIndex = createdIndex;
        this.modifiedIndex = modifiedIndex;
        this.value = value;
        this.expiration = expiration;
        this.ttl = ttl;
        this.nodes = nodes != null ? Collections.unmodifiableSet(nodes) : Collections.<ImmutableNode>emptySet();
        this.dir = dir;
    }

    public String getKey() {
        return key;
    }

    public long getCreatedIndex() {
        return createdIndex;
    }

    public long getModifiedIndex() {
        return modifiedIndex;
    }

    public String getValue() {
        return value;
    }

    public String getExpiration() {
        return expiration;
    }

    public int getTtl() {
        return ttl;
    }

    public Set<ImmutableNode> getNodes() {
        return nodes;
    }

    public boolean isDir() {
        return dir;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImmutableNode node = (ImmutableNode) o;

        if (createdIndex != node.createdIndex) return false;
        if (modifiedIndex != node.modifiedIndex) return false;
        if (key != null ? !key.equals(node.key) : node.key != null) return false;
        if (value != null ? !value.equals(node.value) : node.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return key != null ? key.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ImmutableNode{" +
                "key='" + key + '\'' +
                ", createdIndex=" + createdIndex +
                ", modifiedIndex=" + modifiedIndex +
                ", value='" + value + '\'' +
                ", expiration='" + expiration + '\'' +
                ", ttl=" + ttl +
                ", nodes=" + nodes +
                ", dir=" + dir +
                '}';
    }
}
