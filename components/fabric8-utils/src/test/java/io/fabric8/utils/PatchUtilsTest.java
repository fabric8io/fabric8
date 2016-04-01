/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.utils;

import org.junit.Test;

import static io.fabric8.utils.PatchUtils.appendVersionRange;
import static io.fabric8.utils.PatchUtils.extractUrl;
import static io.fabric8.utils.PatchUtils.extractVersionRange;
import static org.junit.Assert.assertEquals;

/**
 * Test cases for {@link PatchUtils}
 */
public class PatchUtilsTest {

    @Test
    public void testExtractUrlAndRange() {
        String url = appendVersionRange("mvn:io.fabric8.test/test/1.0.0", "[1.0.0,1.1.0)");
        assertEquals("mvn:io.fabric8.test/test/1.0.0;range=[1.0.0,1.1.0)", url);
        doAssertExtract(url, "mvn:io.fabric8.test/test/1.0.0", "[1.0.0,1.1.0)");

        // ensure it also works if there's no version range
        doAssertExtract("mvn:io.fabric8.test/test/1.0.0",
                        "mvn:io.fabric8.test/test/1.0.0", null);
    }

    private void doAssertExtract(String override, String url, String range) {
        assertEquals("Should extract URL", url, extractUrl(override));
        assertEquals("Should extract version range", range, extractVersionRange(override));
    }

}
