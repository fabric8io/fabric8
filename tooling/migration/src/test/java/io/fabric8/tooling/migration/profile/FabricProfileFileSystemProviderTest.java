/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
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
