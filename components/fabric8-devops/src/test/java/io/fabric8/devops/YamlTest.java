/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.devops;


import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class YamlTest {
    @Test
    public void testGenerateYaml() throws Exception {
        ProjectConfig config = new ProjectConfig();
        config.addFlow("io.fabric8.flow.maven.Deploy.groovy");
        config.setChatRoom("myroom");
        config.setCodeReview(true);
        config.setIssueProjectName("THING");

        String yaml = YamlHelper.toYaml(config);
        System.out.println("Config: " + config + " is yaml: " + yaml);
    }

    @Test
    public void testParseYaml() throws Exception {
        String basedir = System.getProperty("basedir", ".");
        File file = new File(basedir, "src/test/resources/fabric8.yml");
        assertThat(file).exists();

        ProjectConfig config = YamlHelper.parseProjectConfig(file);
        System.out.println("Parsed: " + config);

        assertThat(config.getChatRoom()).isEqualTo("myroom");
        assertThat(config.getIssueProjectName()).isEqualTo("foo");
        assertThat(config.getFlows()).contains("io.fabric8.flow.maven.CanaryReleaseThenStage.groovy");
    }

}
