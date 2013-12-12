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

import junit.framework.Assert;
import org.junit.Test;

import static io.fabric8.fab.DependencyTree.newBuilder;

public class DependencyTreeEqualsTest extends DependencyTestSupport {

    @Test
    public void testSimpleCompare() {
        assertCompare(clogging11, clogging11, true);
        assertCompare(clogging11, clogging104, false);
    }

    @Test
    public void testCompareWithTransientDependencies() {
        assertCompare(camel250_clogging11, camel250_clogging11, true);
        assertCompare(camel250_clogging104, camel250_clogging104, true);

        // same values, different object instances
        assertCompare(camel250_clogging11,
                newBuilder("org.apache.camel", "camel-core", "2.5.0", clogging11).build(),
                true);

        assertCompare(camel250_clogging11, camel250_clogging104, false);
    }

    @Test
    public void testCompareOutOfOrderTransientDependencies() {
        assertCompare(
                newBuilder("org.apache.camel", "camel-core", "2.5.0", clogging11, commonman).build(),
                newBuilder("org.apache.camel", "camel-core", "2.5.0", commonman, clogging11).build(),
                true);

        assertCompare(
                newBuilder("org.apache.camel", "camel-core", "2.5.0", clogging11, commonman).build(),
                newBuilder("org.apache.camel", "camel-core", "2.5.0", clogging11, commonman).build(),
                true);
    }

    protected void assertCompare(Object a, Object b, boolean expected) {
        int h1 = a.hashCode();
        int h2 = b.hashCode();

        if (expected) {
            Assert.assertEquals("hashCode " + a + ", " + b, h1, h2);
            Assert.assertEquals("" + a + ", " + b, a, b);
        } else {
            Assert.assertTrue("" + a + " != " + b, !a.equals(b));
        }
    }
}
