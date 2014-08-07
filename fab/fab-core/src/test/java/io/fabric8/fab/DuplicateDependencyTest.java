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
package io.fabric8.fab;

import java.util.List;
import java.util.logging.Logger;

import junit.framework.Assert;
import org.junit.Test;

import static io.fabric8.fab.DependencyTree.newBuilder;

public class DuplicateDependencyTest {
    private static final transient Logger LOG = Logger.getLogger(DuplicateDependencyTest.class.getName());

    DependencyTree clogging11 = newBuilder("commons-logging", "commons-logging-api", "1.1").build();
    DependencyTree clogging104 = newBuilder("commons-logging", "commons-logging-api", "1.04").build();

    DependencyTree commonman = newBuilder("org.fusesource.commonman", "commons-management", "1.0").build();

    DependencyTree camel250_clogging = newBuilder("org.apache.camel", "camel-core", "2.5.0", clogging11).build();
    DependencyTree camel250_clogging_commonman = newBuilder("org.apache.camel", "camel-core", "2.5.0", clogging11, commonman).build();

    DependencyTree duplicateCamel = newBuilder("org.apache.karaf.pomegrenatel", "test-duplicates", "1.0-SNAPSHOT", camel250_clogging, camel250_clogging_commonman).build();

    @Test
    public void testDuplicates() {
        assertDuplicates(camel250_clogging, 0);
        assertDuplicates(camel250_clogging_commonman, 0);
        assertDuplicates(duplicateCamel, 1);
    }

    protected void assertDuplicates(DependencyTree tree, int expectedDuplicateCount) {
        List<DependencyTree.DuplicateDependency> duplicateDependencies = tree.checkForDuplicateDependencies();
        LOG.info(String.valueOf(duplicateDependencies));
        Assert.assertEquals("Expected duplicate dependencies: " + duplicateDependencies, expectedDuplicateCount, duplicateDependencies.size());
    }
}
