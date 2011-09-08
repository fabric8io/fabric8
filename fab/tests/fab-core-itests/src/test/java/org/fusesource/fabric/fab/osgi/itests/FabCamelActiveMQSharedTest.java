package org.fusesource.fabric.fab.osgi.itests;

import org.fusesource.fabric.fab.DependencyTree;
import org.junit.Test;

public class FabCamelActiveMQSharedTest extends IntegrationTestSupport {

    @Test
    public void testFabTree() throws Exception {
        DependencyTree tree = doTestFabricBundle("fab-sample-camel-activemq-share");
        System.out.println(tree.getDescription());

        tree.findDependency("*");
    }
}
