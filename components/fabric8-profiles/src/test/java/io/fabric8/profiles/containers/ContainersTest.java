package io.fabric8.profiles.containers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Properties;

import io.fabric8.profiles.ProfilesHelpers;
import io.fabric8.profiles.containers.karaf.KarafProjectReifier;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.junit.Test;

import static io.fabric8.profiles.ProfilesHelpers.deleteDirectory;
import static io.fabric8.profiles.TestHelpers.PROJECT_BASE_DIR;

/**
 * Test Containers.
 */
public class ContainersTest {

    @Test
    public void testReify() throws Exception {
        // temp karaf project output dir
        Path target = PROJECT_BASE_DIR.resolve("target/test-data/karaf1");
        deleteDirectory(target);
        Files.createDirectories(target);

        // temp profile git repo
        final Path sourceRepository = PROJECT_BASE_DIR.resolve("src/test/karaf-profiles/fabric/fabric/profiles");
        final Path repository = PROJECT_BASE_DIR.resolve("target/test-data/karaf-profiles");
        ProfilesHelpers.copyDirectory(sourceRepository, repository);
        try (final Git profileRepo = new InitCommand().setDirectory(repository.toFile()).call()) {
            profileRepo.add().addFilepattern(".").call();
            profileRepo.commit().setMessage("Adding version 1.0").call();
            profileRepo.branchRename().setOldName("master").setNewName("1.0").call();

            final Properties karafDefaults = new Properties();
            karafDefaults.put("groupId", "io.fabric8.karaf-swarm");
            karafDefaults.put("description", "Karaf Swarm container");

            final Path containerRepository = PROJECT_BASE_DIR.resolve("src/test/zk");
            final HashMap<String, ProjectReifier> reifierMap = new HashMap<>();
            reifierMap.put(Containers.DEFAULT_CONTAINER_TYPE, new KarafProjectReifier(karafDefaults));

            final Containers containers = new Containers(containerRepository, reifierMap, repository.toUri().toString());
            containers.reify(target, "root");
        }
    }

}