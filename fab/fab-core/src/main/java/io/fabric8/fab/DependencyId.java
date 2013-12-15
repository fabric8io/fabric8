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

import org.fusesource.common.util.Objects;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;

import static org.fusesource.common.util.Objects.compare;
import static org.fusesource.common.util.Objects.equal;
import static org.fusesource.common.util.Strings.notEmpty;

/**
 * Represents kind of dependency; so groupId, artifactId, classifier and extension
 * which is then attached to a version
 */
public class DependencyId implements Comparable<DependencyId> {
    private final String groupId;
    private final String artifactId;
    private final String classifier;
    private final String extension;
    private final int hashCode;


    public static DependencyId newInstance(DependencyNode node) {
        return newInstance(node.getDependency());
    }

    public static DependencyId newInstance(Dependency dependency) {
        return newInstance(dependency.getArtifact());
    }

    public static DependencyId newInstance(Artifact artifact) {
        return new DependencyId(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getExtension());
    }

    public DependencyId(String groupId, String artifactId) {
        this(groupId, artifactId, "");
    }

    public DependencyId(String groupId, String artifactId, String classifier) {
        this(groupId, artifactId, classifier, "jar");
    }

    public DependencyId(String groupId, String artifactId, String classifier, String extension) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier;
        this.extension = extension;
        this.hashCode = Objects.hashCode(groupId, artifactId, classifier, extension);
    }

    @Override
    public String toString() {
        return "DependencyId(" + groupId + ":" + artifactId +
                (notEmpty(classifier) ?  ":" + classifier : "") +
                (notEmpty(extension) ? ":" + extension : "") +
                ")";
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof DependencyId) {
            DependencyId that = (DependencyId) o;
            return this.hashCode() == that.hashCode() &&
                    equal(groupId, that.groupId) &&
                    equal(artifactId, that.artifactId) &&
                    equal(classifier, that.classifier) &&
                    equal(extension, that.extension);
        }
        return false;
    }

    public int compareTo(DependencyId that) {
        int answer = compare(groupId, that.groupId);
        if (answer == 0) answer = compare(artifactId, that.artifactId);
        if (answer == 0) answer = compare(classifier, that.classifier);
        if (answer == 0) answer = compare(extension, that.extension);
        return answer;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getExtension() {
        return extension;
    }
}
