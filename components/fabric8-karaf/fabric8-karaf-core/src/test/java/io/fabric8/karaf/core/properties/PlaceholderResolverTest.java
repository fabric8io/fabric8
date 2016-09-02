/**
 * Copyright 2005-2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.karaf.core.properties;

import java.util.HashMap;
import java.util.Map;

import io.fabric8.karaf.core.properties.function.PropertiesFunction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PlaceholderResolverTest {
    private PlaceholderResolverImpl resolver;

    @Before
    public void setUp() {
        resolver = new PlaceholderResolverImpl();
        resolver.bindFunction(new Map1());
        resolver.bindFunction(new Map2());
    }

    @Test
    public void testResolve() {
        Assert.assertEquals("hello1", resolver.resolve("map1:prop1"));
        Assert.assertEquals("hello2", resolver.resolve("map2:prop1"));
        Assert.assertNull(resolver.resolve("map1:noReplace"));
    }

    @Test
    public void testReplace() {
        Assert.assertEquals("hello1 world2", resolver.replace("$[map1:prop1] $[map2:prop2]"));
        Assert.assertEquals("hello2 world1", resolver.replace("$[map2:prop1] $[map1:prop2]"));
        Assert.assertEquals("hello nested world!", resolver.replace("$[map2:prop-$[map1:prop5]]"));
    }

    @Test(expected = IllegalStateException.class)
    public void tesInfiniteLoopSimple() {
        resolver.replace("$[map1:prop6]");
    }

    @Test(expected = IllegalStateException.class)
    public void tesInfiniteLoopPingPong() {
        resolver.replace("$[map1:prop7]");
    }

    @Test
    public void testNoReplace() {
        Assert.assertEquals("$[map1:noReplace]", resolver.replace("$[map1:noReplace]"));
    }

    @Test
    public void testReplaceAll() {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "$[map1:prop1] $[map2:prop2]");
        map.put("key2", "$[map2:prop1] $[map1:prop2]");
        map.put("key3", "$[map2:prop-$[map1:prop5]]");
        map.put("key4", 1);
        map.put("key5", "no replace");

        Assert.assertTrue(resolver.replaceAll(map));
        Assert.assertEquals("hello1 world2", map.get("key1"));
        Assert.assertEquals("hello2 world1", map.get("key2"));
        Assert.assertEquals("hello nested world!", map.get("key3"));
        Assert.assertEquals(1, map.get("key4"));
        Assert.assertEquals("no replace", map.get("key5"));
    }

    // *************************************************************************
    //
    // *************************************************************************

    private class Map1 implements PropertiesFunction {
        private final Map<String, String> map;

        public Map1() {
            map = new HashMap<>();
            map.put("prop1","hello1");
            map.put("prop2","world1");
            map.put("prop3","10");
            map.put("prop4","20");
            map.put("prop5","nested");
            map.put("prop6","$[map1:prop6]");
            map.put("prop7","$[map1:prop8]");
            map.put("prop8","$[map1:prop7]");
        }

        public String getName() {
            return "map1";
        }

        @Override
        public String apply(String remainder) {
            return map.get(remainder);
        }
    }

    private class Map2 implements PropertiesFunction {
        private final Map<String, String> map;

        public Map2() {
            map = new HashMap<>();
            map.put("prop1","hello2");
            map.put("prop2","world2");
            map.put("prop-nested","hello nested world!");
            map.put("prop-recursive-1","$[prop-recursive-2]");
            map.put("prop-recursive-2","$[prop-recursive-3]");
            map.put("prop-recursive-3","recursive-3");
        }

        public String getName() {
            return "map2";
        }

        @Override
        public String apply(String remainder) {
            return map.get(remainder);
        }
    }
}
