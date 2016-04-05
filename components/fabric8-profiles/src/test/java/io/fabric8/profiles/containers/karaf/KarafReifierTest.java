package io.fabric8.profiles.containers.karaf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import io.fabric8.profiles.Profiles;
import io.fabric8.profiles.ProfilesHelpers;

import org.junit.Test;

import static io.fabric8.profiles.TestHelpers.PROJECT_BASE_DIR;

/**
 * Test Karaf reifier.
 */
public class KarafReifierTest {

    @Test
    public void testReify() throws Exception {

        Path target = PROJECT_BASE_DIR.resolve("target/test-data/karaf1");
        ProfilesHelpers.deleteDirectory(target);
        Files.createDirectories(target);

        Path repository = PROJECT_BASE_DIR.resolve("src/test/it-repo/profiles");
        final Path materialized = PROJECT_BASE_DIR.resolve("target/test-data/karaf-profiles-materialized");
        ProfilesHelpers.deleteDirectory(materialized);
        Files.createDirectories(materialized);

        final Path containerConfig = PROJECT_BASE_DIR.resolve("src/test/it-repo/configs/containers/root.cfg");
        String[] profileNames = ProfilesHelpers.readPropertiesFile(containerConfig).getProperty("profiles").replaceAll(" ?fabric-ensemble-\\S+", "").split(" ");
        new Profiles(repository).materialize(materialized, profileNames);

        final Properties containerProperties = new Properties();
        containerProperties.put("groupId", "io.fabric8.quickstarts");
        containerProperties.put("artifactId", "root");
        containerProperties.put("version", "1.0-SNAPSHOT");
        containerProperties.put("name", "root");
        containerProperties.put("description", "Karaf root container");

        final KarafProjectReifier reifier = new KarafProjectReifier(null);
        reifier.reify(target, containerProperties, materialized);
    }
}
