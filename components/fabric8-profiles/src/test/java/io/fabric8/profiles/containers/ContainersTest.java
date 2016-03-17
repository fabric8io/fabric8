package io.fabric8.profiles.containers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Properties;

import io.fabric8.profiles.Profiles;
import io.fabric8.profiles.containers.karaf.KarafProjectReifier;

import org.junit.Test;

import static io.fabric8.profiles.TestHelpers.PROJECT_BASE_DIR;
import static io.fabric8.profiles.TestHelpers.recusiveDeleteIfExists;

/**
 * Test Containers.
 */
public class ContainersTest {

    @Test
    public void testReify() throws Exception {
        Path target = PROJECT_BASE_DIR.resolve("target/test-data/karaf1");
        recusiveDeleteIfExists(target);
        Files.createDirectories(target);
        Path repository = PROJECT_BASE_DIR.resolve("src/test/karaf-profiles/fabric/fabric/profiles");

        final Properties karafDefaults = new Properties();
        karafDefaults.put("groupId", "io.fabric8.quickstarts");
        karafDefaults.put("description", "Karaf Swarm container");

        final Path containerRepository = PROJECT_BASE_DIR.resolve("src/test/zk");
        final HashMap<String, ProjectReifier> reifierMap = new HashMap<>();
        reifierMap.put(Containers.DEFAULT_CONTAINER_TYPE, new KarafProjectReifier(karafDefaults));

        final Containers containers = new Containers(containerRepository, reifierMap, new Profiles(repository));
        containers.reify(target, "root");
    }
}