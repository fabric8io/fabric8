/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.pomegranate;

import junit.framework.TestCase;
import org.fusesource.fabric.pomegranate.util.Filter;
import org.junit.Test;

import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DependencyTreeFilterTest extends DependencyTestSupport {
    @Test
    public void testFilters() throws Exception {
        DependencyTreeResult node = collectDependencies("test-override-spring.pom");

        assertFilterMatches(node, "commons-logging", "commons-logging", false, "");
        assertFilterMatches(node, "commons-logging", "commons-logging", true,
                "*", "commons-logging:*", "*:*", "*:commons-logging", "commons-logging:commons-logging",
                "foo:bar commons-logging:commons-logging",
                "foo:bar commons-logging:commons-logging xyz:bar",
                "commons-logging:commons-logging foo:bar ");

        assertFilterMatches(node, "org.springframework", "spring-core", true,
                "*", "org.springframework:*", "org.springframework:spring-core");
    }

    protected void assertFilterMatches(DependencyTreeResult result, String groupId, String artifactId, boolean expected, String... filterTexts) throws MalformedURLException {
        DependencyTree tree = assertFindDependencyTree(result, groupId, artifactId);

        for (String filterText : filterTexts) {
            Filter<DependencyTree> filter = DependencyTreeFilters.parse(filterText);
            boolean actual = filter.matches(tree);
            assertEquals("Filter failed for " + filterText, expected, actual);
            //System.out.println("Testing " + tree + " for filter: " + filterText + " = " + actual);
        }
    }

}
