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

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 */
public class MapTest {
    @Test
    public void testStringValues() throws Exception {
        Map map = new HashMap();
        map.put("foo", "a,b,c");

        assertArrayEquals(null, Maps.stringValues(map, "doesNotExist"));
        assertArrayEquals(new String[] {"a", "b", "c"}, Maps.stringValues(map, "foo"));

        Maps.setStringValues(map, "doesNotExist", null);
        assertArrayEquals(null, Maps.stringValues(map, "doesNotExist"));

        Maps.setStringValues(map, "foo", new String[] {"d", "e"});
        assertEquals("d,e", Maps.stringValue(map, "foo"));
        assertArrayEquals(new String[] {"d", "e"}, Maps.stringValues(map, "foo"));
    }

}
