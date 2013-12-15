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
 * Represents a version range as the OSGi version range but without the contraints related to number of segments of that
 * each segment must be a number.
 * The range consist of two versions separated by "," (comma) that starts and ends with "[" for inclusive match or "("
 * for exclusive match and ends with "]" for inclusive match or ")" for exclusive match
 *
 * @author Alin Dreghiciu
 * @since 0.2.0, January 30, 2008
 */
public class VersionRange {

    /**
     * Range as string
     */
    private final String m_range;
    /**
     * Lowest version.
     */
    private final Version m_lowestVersion;
    /**
     * Highest version.
     */
    private final Version m_highestVersion;
    /**
     * True if the lowest version is exclusive (range starts with ().
     */
    private final boolean m_lowestExclusive;
    /**
     * True if the highest version is exclusive (range ends with )).
     */
    private final boolean m_highestExclusive;

    /**
     * Creates a new version range form a string.
     *
     * @param range version range
     * @throws io.fabric8.agent.utils.NullArgumentException
     *                                  if range is empty or null
     * @throws IllegalArgumentException if the version does not comply to specs
     */
    public VersionRange(final String range)
            throws IllegalArgumentException {
        NullArgumentException.validateNotEmpty(range, true, "Range");
        if (!range.contains(",")) {
            throw new IllegalArgumentException("Versions in range must be separated by comma.");
        }
        if (!range.startsWith("[") && !range.startsWith("(")) {
            throw new IllegalArgumentException("Range must start with [ or (.");
        }
        if (!range.endsWith("]") && !range.endsWith(")")) {
            throw new IllegalArgumentException("Range must end with [ or (.");
        }

        m_range = range;
        final String[] versions = m_range.split(",");
        if (versions.length > 2) {
            throw new IllegalArgumentException("Range must contain only one comma.");
        }
        versions[0] = versions[0].trim();
        if (versions[0].length() == 1) {
            throw new IllegalArgumentException("Versions in range cannot be empty.");
        }
        versions[1] = versions[1].trim();
        if (versions[1].length() == 1) {
            throw new IllegalArgumentException("Versions in range cannot be empty.");
        }

        m_lowestVersion = new Version(versions[0].substring(1));
        m_highestVersion = new Version(versions[1].substring(0, versions[1].length() - 1));

        m_lowestExclusive = versions[0].startsWith("(");
        m_highestExclusive = versions[1].endsWith(")");
    }

    /**
     * Getter.
     *
     * @return staring version
     */
    public Version getLowestVersion() {
        return m_lowestVersion;
    }

    /**
     * Getter.
     *
     * @return ending version
     */
    public Version getHighestVersion() {
        return m_highestVersion;
    }

    /**
     * Checks if a version falls into the range.
     *
     * @param version to chack
     * @return true if range includes the version
     */
    public boolean includes(final Version version) {
        if (version == null) {
            return false;
        }

        // check if bigger then lowest version
        int compare = version.compareTo(m_lowestVersion);
        if (compare == -1 || (compare == 0 && m_lowestExclusive)) {
            return false;
        }
        // check if smaller then highest version
        compare = version.compareTo(m_highestVersion);
        return !(compare == 1 || (compare == 0 && m_highestExclusive));
    }

    @Override
    public String toString() {
        return m_range;
    }

}