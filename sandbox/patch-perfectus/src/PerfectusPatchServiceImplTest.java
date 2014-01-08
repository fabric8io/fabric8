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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.felix.utils.version.VersionTable;
import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class PerfectusPatchServiceImplTest {

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
        return PerfectusPatchServiceImpl.compareFuseVersions(v1, v2);
    }

    @Test
    public void testDownload() throws Exception {
        System.setProperty("karaf.home", "target/home");
        System.setProperty("karaf.default.repository", "system");
        System.setProperty("fuse.patch.location", "target/patches");

        PerfectusPatchServiceImpl service = new PerfectusPatchServiceImpl(null, null, null, null);

        List<String> repos = Arrays.asList("https://repo.fusesource.com/nexus/content/repositories/ea");

        long t0 = System.currentTimeMillis();
        Set<Patch> patches1 = service.loadPerfectusPatches(repos, true);
        long t1 = System.currentTimeMillis();
        Set<Patch> patches2 = service.loadPerfectusPatches(repos, false);
        long t2 = System.currentTimeMillis();

        assertEquals(patches1.size(), patches2.size());
        assertTrue(t2 - t1 < (t1 - t0) / 2);

        System.out.println(patches1);
    }

}
