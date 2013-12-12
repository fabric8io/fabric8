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
package io.fabric8.api;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 */
public class VersionSequenceTest {

    @Test
    public void testSort() throws Exception {
        List<VersionSequence> list = versions("1.15", "1.5", "1.0.10", "1.0", "1.0.5");
        Collections.sort(list);
        assertEquals(versions("1.0", "1.0.5", "1.0.10", "1.5", "1.15"), list);
    }

    @Test
    public void testNextVersion() throws Exception {
        assertNext("1", "2");
        assertNext("1.0", "1.1");
        assertNext("1.9", "1.10");
        assertNext("1.2.3.9", "1.2.3.10");
    }

    protected List<VersionSequence> versions(String... names) {
        List<VersionSequence> answer = new ArrayList<VersionSequence>();
        for (String name : names) {
            answer.add(new VersionSequence(name));
        }
        return answer;
    }

    protected void assertNext(String versionName, String expectedVersionName) {
        VersionSequence vs = new VersionSequence(versionName);
        VersionSequence next = vs.next();
        assertNotNull(next);
        String name = next.getName();
        assertEquals("Next version number is not correct", expectedVersionName, name);

        int less = vs.compareTo(next);
        assertTrue("Old value should be less than new one: " + less, less < 0);

        int greater = next.compareTo(vs);
        assertTrue("New value should be greater than old one: " + greater, greater > 0);
    }


}
