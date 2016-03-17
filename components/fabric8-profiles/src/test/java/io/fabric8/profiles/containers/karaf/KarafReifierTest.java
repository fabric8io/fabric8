package io.fabric8.profiles.containers.karaf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import io.fabric8.profiles.Profiles;

import org.junit.Test;

import static io.fabric8.profiles.TestHelpers.PROJECT_BASE_DIR;
import static io.fabric8.profiles.TestHelpers.recusiveDeleteIfExists;

/**
 * Test Karaf reifier.
 */
public class KarafReifierTest {

    @Test
    public void testReify() throws Exception {

        Path target = PROJECT_BASE_DIR.resolve("target/test-data/karaf1");
        recusiveDeleteIfExists(target);
        Files.createDirectories(target);
        Path repository = PROJECT_BASE_DIR.resolve("src/test/karaf-profiles/fabric/fabric/profiles");

        final Properties containerProperties = new Properties();
        containerProperties.put("groupId", "io.fabric8.quickstarts");
        containerProperties.put("artifactId", "root");
        containerProperties.put("version", "1.0-SNAPSHOT");
        containerProperties.put("name", "root");
        containerProperties.put("description", "Karaf root container");

        final KarafProjectReifier reifier = new KarafProjectReifier(null);
        reifier.reify(target, containerProperties, new Profiles(repository), "jboss-fuse-minimal");
    }
}
