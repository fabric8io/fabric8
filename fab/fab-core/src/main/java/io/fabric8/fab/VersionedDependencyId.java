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

import org.apache.maven.model.Model;

import static org.fusesource.common.util.Strings.notEmpty;
import static org.fusesource.common.util.Strings.nullIfEmpty;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class VersionedDependencyId extends DependencyId {

    final private String version;

    public VersionedDependencyId(String groupId, String artifactId, String version, String extension, String classifier) {
        super(groupId, artifactId, classifier, notEmpty(extension) ? extension : "jar");
        this.version = version;
    }

    public VersionedDependencyId(Model tree) {
        this(tree.getGroupId(), tree.getArtifactId(), tree.getVersion(), tree.getPackaging(), null);
    }

    public String getRepositoryPath() {
        return getGroupId().replace('.', '/')+"/"+getArtifactId()+"/"+getVersion()+"/"+getArtifactId()+"-"+getVersion() +
               (notEmpty(getClassifier()) ? "-"+getClassifier() : "") +
               "."+getExtension();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VersionedDependencyId)) return false;
        if (!super.equals(o)) return false;

        VersionedDependencyId that = (VersionedDependencyId) o;

        if (version != null ? !version.equals(that.version) : that.version != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

    public DependencyId toDependencyId() {
        return new DependencyId(getGroupId(), getArtifactId(), getClassifier(), getExtension());
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return getGroupId() + ":" + getArtifactId() + ":" + getVersion() + ":" + getExtension() +
                (notEmpty(getClassifier()) ?  ":" + getClassifier() : "")
                ;

    }

     public static VersionedDependencyId fromString(String value) {
        String[] parts = value.split(":");
        if( parts.length < 3 ) {
            throw new IllegalArgumentException("Invalid dependency id: "+value);
        }
        String groupId = parts[0];
        String artifactId = parts[1];
        String version = parts[2];
        String extension = parts.length > 3 ? nullIfEmpty(parts[3]) : null;
        String classifier = parts.length > 4 ?  nullIfEmpty(parts[4]) : null;
        return new VersionedDependencyId(groupId, artifactId, version, extension, classifier);
    }


}
