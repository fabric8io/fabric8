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

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static io.fabric8.kubernetes.api.pipelines.PipelineConfiguration.FABRIC8_PIPELINES;
import static io.fabric8.kubernetes.api.pipelines.PipelineConfigurationParseTest.loadTestConfigMap;
import static io.fabric8.kubernetes.api.pipelines.PipelinesTest.assertJobName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 */
public class OpenShiftPipelineTest {
    @Rule
    public OpenShiftServer server = new OpenShiftServer();

    protected KubernetesClient kubernetesClient;


    @Test
    public void testPipelinesFromConfigMap() throws Exception {
        String namespace = "myproject";

        final ConfigMap configMap = loadTestConfigMap();

        server.expect().withPath("/api/v1/namespaces/" + namespace + "/configmaps/" + FABRIC8_PIPELINES).andReturn(200, configMap).once();

        PipelineConfiguration configuration = PipelineConfiguration.loadPipelineConfiguration(getKubernetesClient(), namespace);

        assertJobName(configuration, "foo", "dummy", PipelineKind.CD);
        assertJobName(configuration, "bar", "dummy", PipelineKind.CI);
        assertJobName(configuration, "whatnot", "dummy", PipelineKind.Developer);

        assertJobName(configuration, "random", "master", "git@github.com:fabric8io/random.git", PipelineKind.CD);
        assertJobName(configuration, "random", "master", "https://github.com/fabric8io/random.git", PipelineKind.CD);
        assertJobName(configuration, "random", "not-master", "https://github.com/fabric8io/random.git", PipelineKind.Developer);

        assertJobName(configuration, "random", "master", "https://github.com/bar/foo.git", PipelineKind.Developer);
        assertJobName(configuration, "random", "master", "git@github.com:bar/foo.git", PipelineKind.Developer);

        // lets show we can opt out of CD pipelines for specific builds in an organisation if required
        assertJobName(configuration, "random", "master", "https://github.com/random/whatnot.git", PipelineKind.Developer);
        assertJobName(configuration, "random", "release", "https://github.com/random/whatnot.git", PipelineKind.CD);
        assertJobName(configuration, "random", "whatever", "https://github.com/random/whatnot.git", PipelineKind.CI);
    }

    @Test
    public void testPipelinesFromConfigMapWithCaching() throws Exception {
        String namespace = "myproject";

        final ConfigMap configMap = loadTestConfigMap();

        server.expect().withPath("/api/v1/namespaces/" + namespace + "/configmaps/" + FABRIC8_PIPELINES).andReturn(200, configMap).once();

        Map<String, String> environment = new HashMap<>();
        environment.put(Pipelines.JOB_NAME, "foo");
        environment.put("BRANCH_NAME", "master");

        Pipeline pipeline = Pipelines.getPipeline(getKubernetesClient(), namespace, environment);
        assertEquals("Pipeline kind", pipeline.getKind(), PipelineKind.CD);

        assertEquals("$" + Pipelines.PIPELINE_KIND, "CD", environment.get(Pipelines.PIPELINE_KIND));

        // we should not query the ConfigMap again!
        pipeline = Pipelines.getPipeline(getKubernetesClient(), namespace, environment);
        assertEquals("Pipeline kind", pipeline.getKind(), PipelineKind.CD);

    }

    @Test
    public void testPipelinesWithNoConfigMap() throws Exception {
        String namespace = "myproject";

        PipelineConfiguration configuration = PipelineConfiguration.loadPipelineConfiguration(getKubernetesClient(), namespace);

        assertJobName(configuration, "whatnot", "master", PipelineKind.Developer);
        assertJobName(configuration, "whatnot", "release", PipelineKind.Developer);

        // match CI if branch pattern matches
        assertJobName(configuration, "whatnot", null, PipelineKind.Developer);
        assertJobName(configuration, "whatnot", "PR-123", PipelineKind.CI);
    }


    public KubernetesClient getKubernetesClient() {
        if (kubernetesClient == null) {
            kubernetesClient = server.getKubernetesClient();
        }
        assertNotNull("No KubernetesClient was created by the mock!", kubernetesClient);
        return kubernetesClient;
    }

    public void setKubernetesClient(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

}
