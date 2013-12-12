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
package io.fabric8.fab;

import org.fusesource.common.util.Filter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@link DependencyTreeFilter}
 */
public class DependencyTreeFiltersTest {

    @Test
    public void testPrune() {
        final DependencyTree sub11 = DependencyTree.newBuilder("test", "child1-sub1", "1.0").build();
        final DependencyTree sub12 = DependencyTree.newBuilder("test", "child1-sub2", "1.0").build();
        final DependencyTree child1 = DependencyTree.newBuilder("test", "child1", "1.0", sub11,  sub12).build();

        final DependencyTree child2 = DependencyTree.newBuilder("test", "child2", "1.0").build();

        final DependencyTree root = DependencyTree.newBuilder("test", "root", "1.0", child1, child2).build();

        DependencyTreeFilters.prune(root, new Filter<DependencyTree>() {
            @Override
            public boolean matches(DependencyTree dependencyTree) {
            // pruning child1 from the dependency tree
            return dependencyTree == child1;
            }
        });

        // we only expect child 2 to remain in the tree
        assertEquals(1, root.getDescendants().size());
        assertTrue(root.getDescendants().contains(child2));
    }

    @Test
    public void testParseWithNullArgument() {
        Filter<DependencyTree> filter = DependencyTreeFilters.parse(null);
        assertNotNull(filter);
    }
}
