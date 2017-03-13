/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api.pipelines;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

import static io.fabric8.kubernetes.api.pipelines.PipelineConfiguration.FABRIC8_PIPELINES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

/**
 */
public class PipelineConfigurationParseTest {
    public static ConfigMap loadTestConfigMap() throws IOException {
        String path = FABRIC8_PIPELINES + ".yml";
        URL resource = PipelineConfigurationParseTest.class.getClassLoader().getResource(path);
        assertNotNull("Could not load resource: " + path, resource);

        try (InputStream in = resource.openStream()) {
            assertNotNull("Could not open stream for resource: " + resource, in);
            ConfigMap configMap = KubernetesHelper.loadYaml(in, ConfigMap.class);
            assertNotNull("Should have loaded " + resource, configMap);
            return configMap;
        }
    }

    @Test
    public void testParsePipelineConfiguration() throws Exception {
        ConfigMap configMap = loadTestConfigMap();

        PipelineConfiguration pipelineConfiguration = PipelineConfiguration.getPipelineConfiguration(configMap);
        System.out.println("Loaded: " + pipelineConfiguration);

        assertThat(pipelineConfiguration.getCdBranchPatterns()).describedAs("getCdBranchPatterns()").contains("release");
        assertThat(pipelineConfiguration.getCiBranchPatterns()).describedAs("getCiBranchPatterns()").contains("PR-.*").contains("whatever");

        assertThat(pipelineConfiguration.getJobNameToKindMap()).describedAs("getJobNameToKindMap()").
                containsEntry("foo", PipelineKind.CD).
                containsEntry("bar", PipelineKind.CI).
                containsEntry("whatnot", PipelineKind.Developer);
        assertThat(pipelineConfiguration.getCdGitHostAndOrganisationToBranchPatterns()).describedAs("getCdGitHostAndOrganisationToBranchPatterns()").
                containsEntry("github.com/fabric8io", Arrays.asList("master")).
                containsEntry("github.com/fabric8-quickstarts", Arrays.asList("PR-.*"));
    }

}
