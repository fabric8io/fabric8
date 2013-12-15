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
import org.fusesource.common.util.Filters;
import org.junit.Test;
import org.sonatype.aether.resolution.ArtifactResolutionException;

import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;

public class DependencyTreeFilterTest extends DependencyTestSupport {
    @Test
    public void testFilters() throws Exception {
        DependencyTreeResult node = collectDependencies("test-override-spring.pom");

        assertShareFilter(node, "commons-logging", "commons-logging", false, "");
        assertShareFilter(node, "commons-logging", "commons-logging", true,
                // prefix wildcards
                "commons-*", "commons-*:*", "*:commons-*",

                // not wildcard matches
                "commons-logging:!foo",
                "!foo:*",

                "*", "commons-logging:*", "*:*", "*:commons-logging", "commons-logging:commons-logging",
                "foo:bar commons-logging:commons-logging",
                "foo:bar commons-logging:commons-logging xyz:bar",
                "commons-logging:commons-logging foo:bar ");

        assertShareFilter(node, "org.springframework", "spring-core", true,
                "*", "org.springframework:*", "org.springframework:spring-core");
    }

    @Test
    public void testProvidedDependencies() throws Exception {
        DependencyTreeResult node = collectDependencies("test-osgi-provided.pom");

        // we exclude optional dependencies by default
//        DependencyTree camelSpring = assertExcludeFilter(node, "org.apache.camel", "camel-spring", true, "");

        // we exclude provided dependencies by default
        DependencyTree osgi = assertShareFilter(node, "org.osgi", "org.osgi.core", true, "", "");

        assertEquals("getBundleId", "osgi.core", osgi.getBundleSymbolicName());
    }

//    @Test
//    public void testExcludedTransitiveSharedDependencies() throws Exception {
//        DependencyTreeResult node = collectDependencies("test-normal.pom");
//        DependencyTree camelSpring = assertExcludeFilter(node, "org.springframework", "spring-context", true, "");
//    }

    protected DependencyTree assertShareFilter(DependencyTreeResult result, String groupId, String artifactId, boolean expected, String... filterTexts) throws MalformedURLException, ArtifactResolutionException {
        DependencyTree tree = assertFindDependencyTree(result, groupId, artifactId);

        for (String filterText : filterTexts) {
            Filter<DependencyTree> filter = DependencyTreeFilters.parseShareFilter(filterText);
            boolean actual = filter.matches(tree);
            assertEquals("Filter failed for " + filterText, expected, actual);
            //System.out.println("Testing " + tree + " for filter: " + filterText + " = " + actual);
        }
        return tree;
    }


    protected DependencyTree assertExcludeFilter(DependencyTreeResult result, String groupId, String artifactId, boolean expected, String filterText) throws MalformedURLException, ArtifactResolutionException {
        DependencyTree tree = assertFindDependencyTree(result, groupId, artifactId);
        Filter<DependencyTree> filter = DependencyTreeFilters.parseExcludeFilter(filterText, Filters.falseFilter());
        boolean actual = filter.matches(tree);
        assertEquals("Filter failed for " + filterText, expected, actual);
        //System.out.println("Testing " + tree + " for filter: " + filterText + " = " + actual);
        return tree;
    }

}
