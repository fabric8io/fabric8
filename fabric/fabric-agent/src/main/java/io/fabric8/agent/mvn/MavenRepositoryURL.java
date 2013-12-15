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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import io.fabric8.agent.utils.NullArgumentException;

/**
 * An URL of Maven repository that knows if it contains SNAPSHOT versions and/or releases.
 *
 * @author Alin Dreghiciu
 * @since 0.2.1, February 07, 2008
 */
public class MavenRepositoryURL {

    /**
     * Repository Id.
     */
    private String m_id;
    /**
     * Repository URL.
     */
    private final URL m_repositoryURL;
    /**
     * Repository file (only if URL is a file URL).
     */
    private final File m_file;
    /**
     * True if the repository contains snapshots.
     */
    private final boolean m_snapshotsEnabled;
    /**
     * True if the repository contains releases.
     */
    private final boolean m_releasesEnabled;

    /**
     * Creates a maven repository URL bases on a string spec. The path can be marked with @snapshots and/or @noreleases
     * (not case sensitive).
     *
     * @param repositorySpec url spec of repository
     * @throws MalformedURLException if spec contains a malformed maven repository url
     * @throws NullArgumentException if repository spec is null or empty
     */
    public MavenRepositoryURL(final String repositorySpec)
            throws MalformedURLException {
        NullArgumentException.validateNotEmpty(repositorySpec, true, "Repository spec");

        final String[] segments = repositorySpec.split(MavenConstants.SEPARATOR_OPTIONS);
        final StringBuilder urlBuilder = new StringBuilder();
        boolean snapshotEnabled = false;
        boolean releasesEnabled = true;
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].trim().equalsIgnoreCase(MavenConstants.OPTION_ALLOW_SNAPSHOTS)) {
                snapshotEnabled = true;
            } else if (segments[i].trim().equalsIgnoreCase(MavenConstants.OPTION_DISALLOW_RELEASES)) {
                releasesEnabled = false;
            } else if (segments[i].trim().startsWith(MavenConstants.OPTION_ID)) {
                if (segments[i].length() > MavenConstants.OPTION_ID.length()) {
                    m_id = segments[i].substring(MavenConstants.OPTION_ID.length() + 1);
                }
            } else {
                if (i > 0) {
                    urlBuilder.append(MavenConstants.SEPARATOR_OPTIONS);
                }
                urlBuilder.append(segments[i]);
            }
        }
        String spec = urlBuilder.toString();
        if (!spec.endsWith("\\") && !spec.endsWith("/")) {
            spec = spec + "/";
        }
        m_repositoryURL = new URL(spec);
        m_snapshotsEnabled = snapshotEnabled;
        m_releasesEnabled = releasesEnabled;
        m_id = m_id != null ? m_id : "" + spec.hashCode();
        if (m_repositoryURL.getProtocol().equals("file")) {
            m_file = new File(m_repositoryURL.getPath());
        } else {
            m_file = null;
        }
    }

    /**
     * Getter.
     *
     * @return repository id
     */
    public String getId() {
        return m_id;
    }

    /**
     * Getter.
     *
     * @return repository URL
     */
    public URL getURL() {
        return m_repositoryURL;
    }

    /**
     * Getter.
     *
     * @return repository file
     */
    public File getFile() {
        return m_file;
    }

    /**
     * Getter.
     *
     * @return true if the repository contains releases
     */
    public boolean isReleasesEnabled() {
        return m_releasesEnabled;
    }

    /**
     * Getter.
     *
     * @return true if the repository contains snapshots
     */
    public boolean isSnapshotsEnabled() {
        return m_snapshotsEnabled;
    }

    /**
     * Getter.
     *
     * @return if the repository is a file based repository.
     */
    public boolean isFileRepository() {
        return m_file != null;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append(m_repositoryURL.toString())
                .append(",releases=").append(m_releasesEnabled)
                .append(",snapshots=").append(m_snapshotsEnabled)
                .toString();
    }

}
