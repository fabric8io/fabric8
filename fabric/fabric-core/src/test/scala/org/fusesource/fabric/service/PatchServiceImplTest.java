/*
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
package org.fusesource.fabric.service;

import org.apache.felix.utils.version.VersionTable;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;

public class PatchServiceImplTest {

    @Test
    public void testVersionComparison() {
        assertSmaller("2.5.0.fuse-beta-7-039", "2.5.0.fuse-70-070");
        assertSmaller("2.5.0.fuse-beta-7-039", "2.5.0.fuse-beta-7-042");
        assertSmaller("2.5.0.fuse-beta-70-039", "2.5.0.fuse-beta-71-010");
        assertSmaller("2.5.0.fuse-7-061", "2.5.0.fuse-70-070");
        assertSmaller("2.5.0.fuse-7-061", "2.5.0.fuse-7-0-068");
        assertSmaller("2.5.0.fuse-7-0-061", "2.5.0.fuse-70-068");
        assertSmaller("2.5.0.fuse-7-0-061", "2.5.0.fuse-71-018");
    }

    private void assertSmaller(String o1, String o2) {
        assertTrue(o1 + " < " + o2, compare(o1, o2) < 0);
    }

    private int compare(String o1, String o2) {
        org.osgi.framework.Version v1 = VersionTable.getVersion(o1);
        org.osgi.framework.Version v2 = VersionTable.getVersion(o2);
        return PatchServiceImpl.compareFuseVersions(v1, v2);
    }
}
