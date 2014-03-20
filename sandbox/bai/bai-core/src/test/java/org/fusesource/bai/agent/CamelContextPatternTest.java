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
package org.fusesource.bai.agent;

import org.fusesource.bai.agent.filters.CamelContextFilters;
import org.fusesource.common.util.Filter;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 */
public class CamelContextPatternTest {
    @Test
    public void testMatchesPatterns() throws Exception {
        assertMatchesBundle("com.acme.foo.bar", "myContext",
                "*",
                "*:*",
                "com.acme*",
                "com.acme*:*",
                "com.acme.foo.bar",
                "com.acme.foo.bar:*",
                "com.acme.foo.bar:my*",
                "com.acme.foo.bar:myContext",
                "*:myContext",
                "*:my*"
        );
    }

    @Test
    public void testNotMatchesPatterns() throws Exception {
        assertNotMatchesBundle("com.acme.foo.bar", "myContext",
                "",
                "a",
                "a:*",
                "a*:*",
                "*:my",
                "*:x*",
                "myContext"
        );
    }

    protected static void assertMatchesBundle(String bundleSymbolicName, String camelContextId, String... patterns) {
        assertBundlePattern(bundleSymbolicName, camelContextId, true, patterns);
    }

    protected static void assertNotMatchesBundle(String bundleSymbolicName, String camelContextId, String... patterns) {
        assertBundlePattern(bundleSymbolicName, camelContextId, false, patterns);
    }

    private static void assertBundlePattern(String bundleSymbolicName, String camelContextId, boolean expected, String[] patterns) {
        for (String pattern : patterns) {
            CamelContextService camelService = new StubCamelContextService(bundleSymbolicName, camelContextId);
            Filter<CamelContextService> filter = CamelContextFilters.createCamelContextFilter(pattern);

            boolean matches = filter.matches(camelService);

            String message = "'" + pattern + "'" +
                    " for bundleSymbolicName: " + bundleSymbolicName
                    + " camelContextId: " + camelContextId;
            if (expected) {
                assertTrue("Should match pattern " + message, matches);
            } else {
                assertFalse("Should not match pattern " + message, matches);
            }
        }
    }
}
