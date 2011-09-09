/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi;

import org.fusesource.fabric.fab.osgi.internal.Versions;
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
