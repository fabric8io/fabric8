/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.api;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class VersionSequenceTest {

    @Test
    public void testSort() throws Exception {
        List<VersionSequence> list = versions("1.15", "1.5", "1.0.10", "1.0", "1.0.5");
        Collections.sort(list);
        Assert.assertEquals(versions("1.0", "1.0.5", "1.0.10", "1.5", "1.15"), list);
    }

    @Test
    public void testComparator() throws Exception {
        List<String> list = Arrays.asList("1.15", "1.5", "1.0.10", "1.0", "1.0.5");
        Collections.sort(list, VersionSequence.getComparator());
        Assert.assertEquals(Arrays.asList("1.0", "1.0.5", "1.0.10", "1.5", "1.15"), list);
    }

    @Test
    public void testNextVersion() throws Exception {
        assertNext("1", "2");
        assertNext("1.0", "1.1");
        assertNext("1.9", "1.10");
        assertNext("1.2.3.9", "1.2.3.10");
    }

    private List<VersionSequence> versions(String... names) {
        List<VersionSequence> answer = new ArrayList<VersionSequence>();
        for (String name : names) {
            answer.add(new VersionSequence(name));
        }
        return answer;
    }

    private void assertNext(String versionName, String expectedVersionName) {
        VersionSequence vs = new VersionSequence(versionName);
        VersionSequence next = vs.next();
        Assert.assertNotNull(next);
        String name = next.getName();
        Assert.assertEquals("Next version number is not correct", expectedVersionName, name);

        int less = vs.compareTo(next);
        Assert.assertTrue("Old value should be less than new one: " + less, less < 0);

        int greater = next.compareTo(vs);
        Assert.assertTrue("New value should be greater than old one: " + greater, greater > 0);
    }


}
