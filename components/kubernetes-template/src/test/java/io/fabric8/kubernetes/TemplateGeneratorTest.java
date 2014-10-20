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
package io.fabric8.kubernetes;

import io.fabric8.common.util.IOHelpers;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ControllerDesiredState;
import io.fabric8.kubernetes.api.model.Env;
import io.fabric8.kubernetes.api.model.Port;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;
import io.fabric8.kubernetes.template.CreateAppDTO;
import io.fabric8.kubernetes.template.TemplateGenerator;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.fabric8.common.util.Files.recursiveDelete;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

/**
 */
public class TemplateGeneratorTest {
    @Test
    public void testGenerateJson() throws Exception {
        String basedir = System.getProperty("basedir", ".");
        File jsonFile = new File(basedir + "/target/templateGenerator/sample.json").getCanonicalFile();
        recursiveDelete(jsonFile);


        String name = "MyApp";
        int replicaCount = 3;

        CreateAppDTO dto = new CreateAppDTO();
        dto.setName(name);
        dto.setDockerImage("fabric8/hawtio");
        dto.setReplicaCount(replicaCount);

        List<Port> ports = new ArrayList<>();
        Port jolokiaPort = new Port();
        jolokiaPort.setHostPort(10001);
        jolokiaPort.setContainerPort(8778);

        Port brokerPort = new Port();
        brokerPort.setHostPort(6161);
        brokerPort.setContainerPort(6162);

        ports.add(jolokiaPort);
        ports.add(brokerPort);
        dto.setPorts(ports);

        Map<String, String> labels = new HashMap<>();
        labels.put("foo", "bar");
        labels.put("drink", "beer");
        dto.setLabels(labels);

        List<Env> envs = new ArrayList<>();
        Env env1 = new Env();
        env1.setName("CHEESE");
        env1.setValue("EDAM");

        Env env2 = new Env();
        env2.setName("CHEESE");
        env2.setValue("EDAM");

        envs.add(env1);
        envs.add(env2);

        dto.setEnvironmentVariables(envs);

        TemplateGenerator generator = new TemplateGenerator(dto);
        generator.generate(jsonFile);

        labels.put("name", name);

        String json = IOHelpers.readFully(jsonFile);
        System.out.println("Generated: " + json);

        Object loadedDTO = KubernetesHelper.loadJson(json);
        System.out.println("Loaded json DTO: " + loadedDTO);
        assertTrue("Loaded DTO should be a ReplicationControllerSchema but was " + loadedDTO, loadedDTO instanceof ReplicationControllerSchema);
        ReplicationControllerSchema rc = (ReplicationControllerSchema) loadedDTO;

        assertThat(rc.getLabels()).isEqualTo(labels);
        ControllerDesiredState desiredState = rc.getDesiredState();
        assertThat(desiredState.getReplicas()).isEqualTo(replicaCount);
        assertThat(desiredState.getReplicaSelector()).isEqualTo(labels);

        // TODO expose labels on pod template
        //assertThat(desiredState.getPodTemplate()).isEqualTo(labels);
    }


}
