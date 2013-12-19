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
package org.fusesource.fabric.service;

import java.util.List;
import java.util.Set;

import org.apache.felix.utils.version.VersionTable;
import org.fusesource.fabric.api.Issue;
import org.fusesource.fabric.service.Patch;

public class PatchImpl implements Patch, Comparable<PatchImpl> {
    private final String id;
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final Set<String> artifacts;
    private final List<Issue> issues;

    public PatchImpl(String id, String location, Set<String> artifacts, List<Issue> issues) {
        this.id = id;
        String[] mvn = location.split("\\|");
        this.groupId = mvn[1];
        this.artifactId = mvn[2];
        this.version = mvn[3];
        this.artifacts = artifacts;
        this.issues = issues;
    }

    public String getId() {
        return id;
    }

    public Set<String> getArtifacts() {
        return artifacts;
    }

    public List<Issue> getIssues() {
        return issues;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public int compareTo(PatchImpl o) {
        int c = this.groupId.compareTo(o.groupId);
        if (c != 0) {
            return c;
        }
        c = this.artifactId.compareTo(o.artifactId);
        if (c != 0) {
            return c;
        }
        org.osgi.framework.Version v1 = VersionTable.getVersion(this.version);
        org.osgi.framework.Version v2 = VersionTable.getVersion(o.version);
        return PerfectusPatchServiceImpl.compareFuseVersions(v1, v2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PatchImpl patch = (PatchImpl) o;
        return this.compareTo(patch) == 0;
    }

    @Override
    public int hashCode() {
        int result = groupId != null ? groupId.hashCode() : 0;
        result = 31 * result + (artifactId != null ? artifactId.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Patch[" +
                "id='" + id + '\'' +
                ", groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                //", artifacts=" + artifacts +
                //", issues=" + issues +
                ']';
    }
}
