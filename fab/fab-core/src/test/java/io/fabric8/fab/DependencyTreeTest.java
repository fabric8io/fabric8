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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DependencyTreeTest extends DependencyTestSupport {

    @Test
    public void testTransitiveDependencies() throws Exception {
        DependencyTreeResult node = collectDependencies("test-normal.pom");
        assertVersions(node, "commons-logging", "commons-logging-api", "1.1");
    }

    @Test
    public void testOverrideClogging() throws Exception {
        DependencyTreeResult node = collectDependencies("test-override-clogging.pom");

        // since we overload clogging, we should have 2 dependencies now; one in the root
        // and one in our transitive dependency
        assertVersions(node, "commons-logging", "commons-logging-api", "1.0.4", "1.0.4");
    }

    @Test
    public void testOverrideSpring() throws Exception {
        DependencyTreeResult node = collectDependencies("test-override-spring.pom");
        assertVersions(node, "commons-logging", "commons-logging", "1.1.1", "1.1.1");
        assertVersions(node, "org.springframework", "spring-core", "3.0.5.RELEASE");
    }


}
