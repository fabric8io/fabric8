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
package io.fabric8.agent.mvn;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import io.fabric8.agent.utils.NullArgumentException;
import io.fabric8.agent.utils.URLUtils;

/**
 * An artifact that can be downloaded.
 *
 * @author Alin Dreghiciu
 * @since 0.2.0, January 30, 2008
 */
public class DownloadableArtifact {

    /**
     * Artifact version.
     */
    private final Version m_version;
    /**
     * Priority order.
     */
    private final int m_priority;
    /**
     * The full url from where the artifact can be downloaded.
     */
    private final URL m_artifactURL;
    /**
     * True if the certificate should be checked on SSL connection, false otherwise.
     */
    private final Boolean m_checkCertificate;
    /**
     * True if the snapshot is a local built snapshot artifact.
     */
    private final boolean m_localSnapshotBuild;

    /**
     * Creates a new downloadable artifact.
     *
     * @param version            artifact version. Cannot be null or empty
     * @param priority           priority
     * @param repositoryURL      url to reporsitory. Cannot be null
     * @param path               a path to the artifact jar file. Cannot be null
     * @param localSnapshotBuild if the artifact is a local built snapshot
     * @param checkCertificate   if the certificate should be checked on an SSL connection. Cannot be null
     * @throws java.io.IOException   re-thrown
     * @throws NullArgumentException if any of the parameters is null or version is empty
     */
    public DownloadableArtifact(final String version,
                                final int priority,
                                final URL repositoryURL,
                                final String path,
                                final boolean localSnapshotBuild,
                                final Boolean checkCertificate)
            throws IOException {
        m_priority = priority;
        NullArgumentException.validateNotEmpty(version, "Version");
        NullArgumentException.validateNotNull(repositoryURL, "Repository URL");
        NullArgumentException.validateNotNull(path, "Path");
        NullArgumentException.validateNotNull(localSnapshotBuild, "Local snapshot build");
        NullArgumentException.validateNotNull(checkCertificate, "Certificate check");

        m_version = new Version(version);
        String repository = repositoryURL.toExternalForm();
        if (!repository.endsWith(Parser.FILE_SEPARATOR)) {
            repository = repository + Parser.FILE_SEPARATOR;
        }
        m_artifactURL = new URL(repository + path);
        m_localSnapshotBuild = localSnapshotBuild;
        m_checkCertificate = checkCertificate;
    }

    /**
     * Return the input stream to artifact.
     *
     * @return prepared input stream
     * @throws IOException re-thrown
     * @see URLUtils#prepareInputStream(java.net.URL, boolean)
     */
    public InputStream getInputStream()
            throws IOException {
        return URLUtils.prepareInputStream(m_artifactURL, !m_checkCertificate);
    }

    /**
     * Getter.
     *
     * @return artifact version
     */
    public Version getVersion() {
        return m_version;
    }

    /**
     * Getter.
     *
     * @return repository priority
     */
    public int getPriority() {
        return m_priority;
    }

    /**
     * Getter.
     *
     * @return artifact URL
     */
    public URL getArtifactURL() {
        return m_artifactURL;
    }

    /**
     * Getter.
     *
     * @return true if the artifacts is  local built snapshot
     */
    public boolean isLocalSnapshotBuild() {
        return m_localSnapshotBuild;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        return m_version.equals(((DownloadableArtifact) o).m_version);

    }

    @Override
    public int hashCode() {
        return m_version.hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("Version [").append(m_version).append("]")
                .append(" from URL [").append(m_artifactURL).append("]")
                .toString();
    }
}
