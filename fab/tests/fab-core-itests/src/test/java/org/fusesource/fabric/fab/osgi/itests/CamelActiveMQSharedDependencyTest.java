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
package io.fabric8.fab.osgi.itests;

import io.fabric8.fab.DependencyTree;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CamelActiveMQSharedDependencyTest extends IntegrationTestSupport {

    @Test
    public void testFabTree() throws Exception {
        DependencyTree tree = doTestFabricBundle("fab-sample-camel-activemq-share");
        System.out.println(tree.getDescription());

        DependencyTree dependencyTree = assertDependencyMatching(tree, ":geronimo-jta_1.0*");
        assertEquals("Should have this dependency excluded due to its overloaded by the 1.1 version", true, dependencyTree.isAllPackagesHidden());
    }

}
