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

import io.fabric8.agent.utils.NullArgumentException;

/**
 * Represents an artifact version.
 *
 * @author Alin Dreghiciu
 * @since 0.2.0, January 30, 2008
 */
public class Version
        implements Comparable<Version> {

    /**
     * Original string version.
     */
    private final String m_version;

    /**
     * Version segments.
     */
    private final VersionSegment[] m_segments;

    /**
     * Creates a new version.
     *
     * @param version version as a string
     * @throws io.fabric8.agent.utils.NullArgumentException
     *          if version is null or empty
     */
    public Version(final String version) {
        NullArgumentException.validateNotEmpty(version, true, "Version");

        m_version = version;
        final String[] segments = version.split("[\\.-]");
        m_segments = new VersionSegment[segments.length];
        for (int i = 0; i < segments.length; i++) {
            final String trimmedSegment = segments[i].trim();
            if ("".equals(trimmedSegment)) {
                m_segments[i] = new NullVersionSegment();
            } else if ("SNAPSHOT".equals(trimmedSegment)) {
                m_segments[i] = new SnapshotVersionSegment();
            } else {
                try {
                    m_segments[i] = new IntegerVersionSegment(trimmedSegment);
                } catch (NumberFormatException ignore) {
                    m_segments[i] = new StringVersionSegment(trimmedSegment);
                }
            }
        }
    }

    /**
     * Getter.
     *
     * @return version segements
     */
    VersionSegment[] getSegments() {
        final VersionSegment[] copy = new VersionSegment[m_segments.length];
        System.arraycopy(m_segments, 0, copy, 0, m_segments.length);
        return copy;
    }

    /**
     * Compars to versions. Comparation is done by comparing each segment that makes up the version. If the version to
     * compare to is null then 1 (greather) is returned.
     *
     * @param version version to compare to
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than
     *         the specified
     * @see Comparable#compareTo(Object)
     */
    public int compareTo(final Version version) {
        // consider null as the lowest version possible
        if (version == null) {
            return 1;
        }
        final VersionSegment[] otherSegments = version.getSegments();
        final int maxSegments = Math.max(m_segments.length, otherSegments.length);
        for (int i = 0; i < maxSegments; i++) {
            VersionSegment thisSegment = new NullVersionSegment();
            if (i < m_segments.length) {
                thisSegment = m_segments[i];
            }
            VersionSegment otherSegment = new NullVersionSegment();
            if (i < otherSegments.length) {
                otherSegment = otherSegments[i];
            }

            int compResult = 0;
            if (thisSegment.getClass().isAssignableFrom(otherSegment.getClass())) {
                compResult = thisSegment.compareTo(otherSegment);
            } else if (thisSegment instanceof StringVersionSegment) {
                compResult = thisSegment.compareTo(new StringVersionSegment(otherSegment.toString()));
            } else if (otherSegment instanceof StringVersionSegment) {
                compResult = new StringVersionSegment(thisSegment.toString()).compareTo(otherSegment);
            }
            if (compResult != 0) {
                return compResult;
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        return m_version.equals(((Version) o).m_version);
    }

    @Override
    public int hashCode() {
        return m_version.hashCode();
    }

    @Override
    public String toString() {
        return m_version;
    }

}
