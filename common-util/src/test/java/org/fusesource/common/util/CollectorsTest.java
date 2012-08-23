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
package org.fusesource.common.util;

import org.fusesource.common.util.Collector;
import org.fusesource.common.util.Collectors;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Test cases for {@link org.fusesource.common.util.Collectors}
 */
public class CollectorsTest {

    @Test
    public void testGetCollection() {
        Collectors<String> collectors = new Collectors<String>(createCollector("a", "b", "c"),
                                                               createCollector("b", "d", "e"));

        Collection<String> result = collectors.getCollection();
        assertCollectionContains(result, "a", "b", "c", "d", "e");
    }

    @Test
    public void testAddCollection() {
        Collectors<String> collectors = new Collectors<String>();
        collectors.addCollection(Arrays.asList("a", "b", "c"));
        collectors.addCollection(Arrays.asList("b", "d", "e"));

        Collection<String> result = collectors.getCollection();
        assertCollectionContains(result, "a", "b", "c", "d", "e");
    }


    private void assertCollectionContains(Collection<String> collection, String... expected) {
        assertNotNull(collection);
        assertEquals(expected.length, collection.size());
        for (String element : expected) {
            assertTrue(collection.contains(element));
        }
    }

    private Collector<String> createCollector(final String... elements) {
        return new Collector<String>() {
            @Override
            public Collection<String> getCollection() {
                return Arrays.asList(elements);
            }
        };
    }


}
