package io.fabric8.profiles.maven;

import java.nio.file.Files;
import java.nio.file.Path;

import io.fabric8.profiles.ProfilesHelpers;
import io.fabric8.profiles.TestHelpers;

import org.codehaus.plexus.PlexusTestCase;
import org.junit.Test;

/**
 * Test container generation using Mojo.
 */
public class ContainersGeneratorTest extends PlexusTestCase {

    @Test
    public void testExecute() throws Exception {
        // set mojo parameters
        ContainersGenerator generator = new ContainersGenerator();

        generator.sourceDirectory = TestHelpers.PROJECT_BASE_DIR.resolve("src/test/it-repo").toFile();
        final Path target = TestHelpers.PROJECT_BASE_DIR.resolve("target/generated-containers");
        ProfilesHelpers.deleteDirectory(target);
        Files.createDirectories(target);

        generator.targetDirectory = target.toFile();
        generator.execute();
    }
}