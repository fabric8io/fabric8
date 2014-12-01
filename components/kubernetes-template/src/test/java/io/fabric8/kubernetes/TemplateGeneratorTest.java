/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.kubernetes;

import io.fabric8.kubernetes.api.Entity;
import io.fabric8.kubernetes.api.IntOrString;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Config;
import io.fabric8.kubernetes.api.model.ControllerDesiredState;
import io.fabric8.kubernetes.api.model.Env;
import io.fabric8.kubernetes.api.model.ManifestContainer;
import io.fabric8.kubernetes.api.model.PodTemplate;
import io.fabric8.kubernetes.api.model.PodTemplateDesiredState;
import io.fabric8.kubernetes.api.model.Port;
import io.fabric8.kubernetes.api.model.PullPolicy;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;
import io.fabric8.kubernetes.api.model.ServiceSchema;
import io.fabric8.kubernetes.template.CreateAppDTO;
import io.fabric8.kubernetes.template.TemplateGenerator;
import io.fabric8.utils.IOHelpers;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.fabric8.utils.Files.recursiveDelete;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class TemplateGeneratorTest {

    private static final transient Logger LOG = LoggerFactory.getLogger(TemplateGeneratorTest.class);
    protected String imagePullPolicy = "PullIfNotPresent";

    @Test
    public void testGenerateControllerJson() throws Exception {
        CreateAppDTO dto = createAppDto(3);
        File jsonFile = generateJsonFile("controllerOnly", dto);

        List<Entity> entities = generateTemplateAndLoadEntities(jsonFile);
        assertEquals("Entities size", 1, entities.size());

        Entity entity = entities.get(0);
        assertGeneratedReplicationController(entity, dto);
    }

    @Test
    public void testGenerateControllerAndServicesJson() throws Exception {
        String serviceName = "my-service";
        Integer servicePort = 80;
        Integer serviceContainerPort = 8080;
        CreateAppDTO dto = createAppDto(4);
        dto.setServiceName(serviceName);
        dto.setServicePort(servicePort);
        dto.setServiceContainerPort(serviceContainerPort);

        File jsonFile = generateJsonFile("controllerAnd", dto);

        List<Entity> entities = generateTemplateAndLoadEntities(jsonFile);
        assertEquals("Entities size", 2, entities.size());

        Entity serviceEntity = entities.get(0);
        assertThat(serviceEntity).isInstanceOf(ServiceSchema.class);
        ServiceSchema service = (ServiceSchema) serviceEntity;
        assertEquals("serviceName", serviceName, service.getId());
        assertEquals("servicePort", servicePort, service.getPort());
        IntOrString containerPortNameOrNumber = service.getContainerPort();
        assertNotNull("containerPortNameOrNumber", containerPortNameOrNumber);
        assertEquals("serviceContainerPort", serviceContainerPort, containerPortNameOrNumber.getIntValue());
        assertEquals("selector", dto.getLabels(), service.getSelector());

        Entity entity = entities.get(1);
        assertGeneratedReplicationController(entity, dto);
    }

    protected File generateJsonFile(String testName, CreateAppDTO dto) throws IOException {
        String basedir = System.getProperty("basedir", ".");
        File jsonFile = new File(basedir + "/target/templateGenerator/" + testName + ".json").getCanonicalFile();
        recursiveDelete(jsonFile);

        TemplateGenerator generator = new TemplateGenerator(dto);
        generator.generate(jsonFile);
        return jsonFile;
    }

    protected static void assertGeneratedReplicationController(Entity entity, CreateAppDTO dto) {
        assertTrue("First entity should be a ReplicationControllerSchema but was " + entity, entity instanceof ReplicationControllerSchema);
        ReplicationControllerSchema rc = (ReplicationControllerSchema) entity;

        assertThat(rc.getLabels()).isEqualTo(dto.getLabels());
        ControllerDesiredState desiredState = rc.getDesiredState();
        assertThat(desiredState.getReplicas()).isEqualTo(dto.getReplicaCount());
        assertThat(desiredState.getReplicaSelector()).isEqualTo(dto.getLabels());

        List<ManifestContainer> containers = KubernetesHelper.getContainers(rc);
        assertEquals("PodTemplate desired containers", 1, containers.size());

        ManifestContainer manifestContainer = containers.get(0);
        PullPolicy pullPolicy = manifestContainer.getImagePullPolicy();
        assertNotNull(pullPolicy);
        assertEquals("pullPolicy", dto.getImagePullPolicy(), pullPolicy.toString());

        // TODO expose labels on pod template
        //assertThat(desiredState.getPodTemplate()).isEqualTo(labels);
    }

    protected List<Entity> generateTemplateAndLoadEntities(File jsonFile) throws IOException {
        String json = IOHelpers.readFully(jsonFile);
        LOG.info("Generated: " + json);

        Object loadedDTO = KubernetesHelper.loadJson(json);
        LOG.info("Loaded json DTO: " + loadedDTO);


        assertTrue("Loaded DTO should be an ObjectNode but was " + loadedDTO, loadedDTO instanceof Config);
        Config config = (Config) loadedDTO;
        return KubernetesHelper.getEntities(config);
    }

    protected CreateAppDTO createAppDto(int replicaCount) {
        String name = "MyApp";

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

        dto.setImagePullPolicy(imagePullPolicy);

        labels.put("name", name);
        return dto;
    }

}
