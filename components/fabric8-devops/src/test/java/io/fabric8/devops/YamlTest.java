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
package io.fabric8.devops;


import io.fabric8.utils.Pair;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 */
public class YamlTest {
    @Test
    public void testGenerateYaml() throws Exception {
        ProjectConfig config = new ProjectConfig();
        config.setPipeline("maven/Deploy.groovy");
        config.setChatRoom("myroom");
        config.setCodeReview(true);
        config.setIssueProjectName("THING");
        config.addLink("Room", "http://letschat.vagrant.f8/rooms/foo");
        config.addLink("Jenkins", "http://jenkins.vagrant.f8/builds/foo");

        String yaml = ProjectConfigs.toYaml(config);
        System.out.println("Config: " + config + " is yaml: " + yaml);
    }

    @Test
    public void testParseYaml() throws Exception {
        String basedir = System.getProperty("basedir", ".");
        File file = new File(basedir, "src/test/resources/fabric8.yml");
        assertThat(file).exists();

        ProjectConfig config = ProjectConfigs.parseProjectConfig(file);
        System.out.println("Parsed: " + config);

        assertThat(config.getChatRoom()).isEqualTo("myroom");
        assertThat(config.getIssueProjectName()).isEqualTo("foo");
        assertThat(config.getPipeline()).isEqualTo("maven/CanaryReleaseThenStage.groovy");
        LinkedHashMap<String, String> environments = config.getEnvironments();

        // lets assert that things are in the correct order...
        List<Pair<String,String>> expectedEnvironents = Arrays.asList(
            new Pair<String,String>("Testing", "gogsadmin-james4-testing"),
            new Pair<String,String>("Staging", "gogsadmin-james4-staging"),
            new Pair<String,String>("Production", "gogsadmin-james4-prod")
        );
        int idx = 0;
        Set<Map.Entry<String, String>> entries = environments.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue();

            System.out.println("Found environment " + key + " = " + value);
            Pair<String, String> actual = new Pair<>(key, value);
            assertTrue("Too many entries - found unexpected value: " + actual, expectedEnvironents.size() > idx);
            Pair<String, String> expected = expectedEnvironents.get(idx++);
            assertEquals("environment " + idx, expected, actual);
        }
    }

}
