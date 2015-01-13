package io.fabric8.tooling.migration.profile;

import java.io.File;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;

import org.apache.karaf.profile.Profile;
import org.apache.karaf.profile.impl.Profiles;
import org.junit.Test;

public class FabricProfileFileSystemProviderTest {

    @Test
    public void testFileSystem() throws Exception {
        URL readme = getClass().getClassLoader().getResource("profiles/ReadMe.md");
        Path fsRoot = new File(readme.toURI()).getParentFile().toPath();

        Path profileRoot = FileSystems.newFileSystem(fsRoot, null).getPath("/");

        Map<String, Profile> profiles = Profiles.loadProfiles(profileRoot);
        for (Profile profile : profiles.values()) {
            System.out.println("Profile: " + profile.getId());
            for (String name : profile.getConfigurationFileNames()) {
                System.out.println("\t" + name);
            }
        }
    }
}
