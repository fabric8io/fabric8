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

import org.junit.Test;

/**
 * Lets test sharing and non sharing of classes based on the dependencies
 */
public class DependencyClassLoaderSharingTest extends DependencyTestSupport {

    @Test
    public void testShareCommonClasses() throws Exception {
        // should be using the same camel and clogging dependencies

        DependencyClassLoader cl1 = getClassLoaderForPom("test-override-clogging-1-1-1.pom");
        DependencyClassLoader cl2 = getClassLoaderForPom("test-override-spring.pom");

        // these classes should be shared across common class loaders
        assertSameClasses(cl1, cl2, "org.apache.commons.logging.Log", "org.apache.commons.logging.LogFactory", "org.apache.camel.CamelContext");
    }

    @Test
    public void testUseDifferentClassesWhenTransientDependenciesChange() throws Exception {
        // transient dependencies of camel-core are different so can't use same classes for clogging or camel

        DependencyClassLoader cl1 = getClassLoaderForPom("test-override-clogging-1-1-1.pom");
        DependencyClassLoader cl2 = getClassLoaderForPom("test-override-clogging.pom");

        // these classes should be different due to the transient spring dependency change
        assertDifferentClasses(cl1, cl2, "org.apache.camel.CamelContext", "org.apache.commons.logging.Log");
    }
}
