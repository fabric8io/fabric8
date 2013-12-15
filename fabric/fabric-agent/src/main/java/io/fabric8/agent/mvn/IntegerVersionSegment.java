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
 * A version segment backed up by a String.
 *
 * @author Alin Dreghiciu
 * @since 0.2.0, January 30, 2008
 */
class IntegerVersionSegment
        implements VersionSegment {

    /**
     * Version segment.
     */
    private final Integer m_segment;

    /**
     * Creates a new integer version segment based on a string. If the version string cannot be converted into an
     * integer it throws a NumberFormatException.
     *
     * @param segment version segment. Cannot be null.
     * @throws NumberFormatException if the version cannot be converted to an integer.
     */
    IntegerVersionSegment(final String segment)
            throws NumberFormatException {
        NullArgumentException.validateNotNull(segment, "Segment");
        m_segment = Integer.valueOf(segment);
    }

    /**
     * Creates a new integer version segment based on an integer
     *
     * @param segment version segment. Cannot be null.
     * @throws NumberFormatException if the version cannot be converted to an integer.
     */
    IntegerVersionSegment(final Integer segment)
            throws NumberFormatException {
        NullArgumentException.validateNotNull(segment, "Segment");
        m_segment = segment;
    }

    /**
     * @see Comparable#compareTo(Object)
     */
    public int compareTo(final VersionSegment versionSegment) {
        if (versionSegment == null) {
            return 1;
        }
        return m_segment.compareTo(((IntegerVersionSegment) versionSegment).m_segment);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IntegerVersionSegment)) {
            return false;
        }

        IntegerVersionSegment that = (IntegerVersionSegment) o;

        return m_segment.equals(that.m_segment);
    }

    @Override
    public int hashCode() {
        return m_segment.hashCode();
    }

    @Override
    public String toString() {
        return m_segment.toString();
    }

}