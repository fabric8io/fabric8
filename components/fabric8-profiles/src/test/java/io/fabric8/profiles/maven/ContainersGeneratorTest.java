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