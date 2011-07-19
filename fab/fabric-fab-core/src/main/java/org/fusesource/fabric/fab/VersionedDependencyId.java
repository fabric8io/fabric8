/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab;

import static org.fusesource.fabric.fab.util.Strings.notEmpty;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class VersionedDependencyId extends DependencyId {

    final private String version;

    public VersionedDependencyId(String groupId, String artifactId, String version, String classifier, String extension) {
        super(groupId, artifactId, classifier, extension);
        this.version = version;
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
        return getGroupId() + ":" + getArtifactId() + ":" + getVersion() +
                (notEmpty(getClassifier()) ?  ":" + getClassifier() : "") +
                (notEmpty(getExtension()) ? ":" + getExtension() : "");

    }
}
