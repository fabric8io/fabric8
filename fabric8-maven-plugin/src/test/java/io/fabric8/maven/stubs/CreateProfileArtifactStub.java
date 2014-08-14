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
package io.fabric8.maven.stubs;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;

public class CreateProfileArtifactStub extends ArtifactStub {

    /**
     * ArtifactStub does not store version range, so we store it here.
     */
    private VersionRange versionRange;

    public CreateProfileArtifactStub( String groupId, String artifactId,
                                          String version, String type )
    {
        setGroupId(groupId);
        setArtifactId(artifactId);
        setVersion(version);
        setType(type);
        versionRange = VersionRange.createFromVersion( version );
    }

    /**
     * Gets the stored version range
     * @return version range
     */
    public VersionRange getVersionRange()
    {
        return versionRange;
    }

    /**
     * Sets the version range instead of discarding it (as in the parent class)
     * @param versionRange the version range
     */
    public void setVersionRange( VersionRange versionRange )
    {
        this.versionRange = versionRange;
    }

}
