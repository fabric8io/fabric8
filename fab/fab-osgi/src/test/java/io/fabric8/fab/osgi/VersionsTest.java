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
package io.fabric8.fab.osgi;

import io.fabric8.fab.osgi.internal.Versions;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VersionsTest {

    @Test
    public void testVersionRanges() {
        assertVersionRange("2.5.6.qualifier", 0, "[2.5.6.qualifier,2.5.6.qualifier]");
        assertVersionRange("2.5.6.qualifier", 1, "[2.5.6.qualifier,2.5.7)");
        assertVersionRange("2.5.6.qualifier", 2, "[2.5.6.qualifier,2.6)");
        assertVersionRange("2.5.6.qualifier", 3, "[2.5.6.qualifier,3)");
        assertVersionRange("2.5.6.qualifier", 4, "[2.5.6.qualifier,)");
    }

    @Test
    public void testExistingRangeUnaffected() {
        assertVersionRange("[1.2.3.foo,2.3.4)", 1, "[1.2.3.foo,2.3.4)");
    }

    protected void assertVersionRange(String version, int digits, String expected) {
        String actual = Versions.toVersionRange(version, digits);
        assertEquals("Versions should be the same for " + version + " with digits: " + digits, expected, actual);
    }

}
