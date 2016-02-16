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
package io.fabric8.profiles;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static io.fabric8.profiles.ProfilesHelpers.readPropertiesFile;
import static io.fabric8.profiles.ProfilesHelpers.readYamlFile;
import static io.fabric8.profiles.TestHelpers.PROJECT_BASE_DIR;
import static io.fabric8.profiles.TestHelpers.readTextFile;
import static io.fabric8.profiles.TestHelpers.recusiveDeleteIfExists;

public class ProfilesTest {

    @Test
    public void basicTest() throws IOException {
        Path target = PROJECT_BASE_DIR.resolve("target/test-data/materialize1");
        recusiveDeleteIfExists(target);
        Files.createDirectories(target);

        Path repository = PROJECT_BASE_DIR.resolve("src/test/profiles");
        new Profiles(repository).materialize(target, "d");

        Assert.assertEquals("d", readTextFile(target.resolve("test.txt")));
        Properties properties = readPropertiesFile(target.resolve("test.properties"));

        Assert.assertEquals("d", properties.getProperty("name"));

        Assert.assertEquals("a", readTextFile(target.resolve("a.txt")));
        Assert.assertEquals("b", readTextFile(target.resolve("b.txt")));
        Assert.assertEquals("c", readTextFile(target.resolve("c.txt")));
        Assert.assertEquals("d", readTextFile(target.resolve("d.txt")));


        JsonNode jsonNode = readYamlFile(target.resolve("test.yml"));
        Assert.assertEquals("d", jsonNode.get("name").textValue());
        Assert.assertEquals("maybe", jsonNode.get("attributes").get("awesome").textValue());
        Assert.assertEquals(4, jsonNode.get("pods").size());

    }

}