package org.fusesource.fabric.pomegranate;

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
        assertVersions(node, "org.springframework", "spring-core", "3.0.0.RELEASE");
    }


}
