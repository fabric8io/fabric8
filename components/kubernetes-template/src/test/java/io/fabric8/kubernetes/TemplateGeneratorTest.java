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
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.Config;
import io.fabric8.kubernetes.api.model.ReplicationControllerState;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PodTemplate;
import io.fabric8.kubernetes.api.model.PodState;
import io.fabric8.kubernetes.api.model.Port;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.util.IntOrString;
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

import static io.fabric8.kubernetes.api.KubernetesHelper.getId;
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

        List<Object> entities = generateTemplateAndLoadEntities(jsonFile);
        assertEquals("Entities size", 1, entities.size());

        Object entity = entities.get(0);
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

        List<Object> entities = generateTemplateAndLoadEntities(jsonFile);
        assertEquals("Entities size", 2, entities.size());

        Object serviceEntity = entities.get(0);
        assertThat(serviceEntity).isInstanceOf(Service.class);
        Service service = (Service) serviceEntity;
        assertEquals("serviceName", serviceName, getId(service));
        ServiceSpec spec = service.getSpec();
        assertEquals("servicePort", servicePort, spec.getPort());
        IntOrString containerPortNameOrNumber = spec.getContainerPort();
        assertNotNull("containerPortNameOrNumber", containerPortNameOrNumber);
        assertEquals("serviceContainerPort", serviceContainerPort, containerPortNameOrNumber.getIntVal());
        assertEquals("selector", dto.getLabels(), spec.getSelector());

        Object entity = entities.get(1);
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

    protected static void assertGeneratedReplicationController(Object entity, CreateAppDTO dto) {
        assertTrue("First entity should be a ReplicationController but was " + entity, entity instanceof ReplicationController);
        ReplicationController rc = (ReplicationController) entity;

        assertThat(rc.getLabels()).isEqualTo(dto.getLabels());
        ReplicationControllerState desiredState = rc.getDesiredState();
        assertThat(desiredState.getReplicas()).isEqualTo(dto.getReplicaCount());
        assertThat(desiredState.getReplicaSelector()).isEqualTo(dto.getLabels());

        List<Container> containers = KubernetesHelper.getContainers(rc);
        assertEquals("PodTemplate desired containers", 1, containers.size());

        Container manifestContainer = containers.get(0);
        String pullPolicy = manifestContainer.getImagePullPolicy();
        assertNotNull(pullPolicy);
        assertEquals("pullPolicy", dto.getImagePullPolicy(), pullPolicy);

        // TODO expose labels on pod template
        //assertThat(desiredState.getPodTemplate()).isEqualTo(labels);
    }

    protected List<Object> generateTemplateAndLoadEntities(File jsonFile) throws IOException {
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

        List<EnvVar> envs = new ArrayList<>();
        EnvVar env1 = new EnvVar();
        env1.setName("CHEESE");
        env1.setValue("EDAM");

        EnvVar env2 = new EnvVar();
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
