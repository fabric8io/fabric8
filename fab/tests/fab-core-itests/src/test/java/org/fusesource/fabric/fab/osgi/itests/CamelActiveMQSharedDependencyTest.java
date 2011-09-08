package org.fusesource.fabric.fab.osgi.itests;

import org.fusesource.fabric.fab.DependencyTree;
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
