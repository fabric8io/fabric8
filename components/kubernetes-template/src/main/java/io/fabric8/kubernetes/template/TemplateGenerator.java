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
package io.fabric8.kubernetes.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.utils.Files;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import static io.fabric8.kubernetes.api.KubernetesHelper.validateKubernetesId;

/**
 */
public class TemplateGenerator {

    private final GenerateDTO config;

    public TemplateGenerator(GenerateDTO config) {
        this.config = config;
    }
    
    public void generate(File kubernetesJson) throws IllegalArgumentException {
        String dockerImage = config.getDockerImage();
        String name = config.getName();
        Map<String, String> labels = config.getLabels();
        // replication controllers
        String replicationControllerName = validateKubernetesId(config.getReplicationControllerName(), "replicationControllerName");

        // service
        String serviceName = config.getServiceName();
        if (Strings.isNotBlank(serviceName)) {
            serviceName = validateKubernetesId(serviceName, "serviceName");
        }
        if (Strings.notEmpty(serviceName)) {
            if (Objects.equal(serviceName, replicationControllerName)) {
                throw new IllegalArgumentException("replicationControllerName and serviceName are the same! (" + serviceName + ")");
            }
        }

        KubernetesListBuilder builder = new KubernetesListBuilder()
                .withKind("Config")
                .withId(name)
                .addNewReplicationController()
                    .withKind("ReplicationController")
                    .withId(replicationControllerName)
                    .withNewDesiredState()
                    .withReplicas(config.getReplicaCount())
                    .withReplicaSelector(config.getLabels())
                    .withNewPodTemplate()
                        .withNewDesiredState()
                        .withNewManifest()
                        .addNewContainer()
                            .withName(config.getContainerName())
                            .withImage(dockerImage)
                            .withImagePullPolicy(config.getImagePullPolicy())
                            .withEnv(config.getEnvironmentVariables())
                        .endContainer()
                        .endManifest()
                        .endDesiredState()
                        .endPodTemplate()
                    .endDesiredState()
                    .withLabels(labels)
                .endReplicationController();

        if (serviceName != null) {
            builder = builder.addNewService()
                    .withId(serviceName)
                    .withKind("Service")
                    .withContainerPort(config.getServiceContainerPort())
                    .withPort(config.getServicePort())
                    .withSelector(labels)
                    .endService();
        }
        KubernetesList kubernetesList = builder.build();

        try {
            ObjectMapper mapper = new ObjectMapper();
            String generated = mapper.writeValueAsString(kubernetesList);
            Files.writeToFile(kubernetesJson, generated, Charset.defaultCharset());
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to generate Kubernetes JSON.", e);
        }
    }
}
