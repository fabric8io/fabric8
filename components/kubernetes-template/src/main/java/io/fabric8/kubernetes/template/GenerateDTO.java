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

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Port;
import io.fabric8.kubernetes.api.model.util.IntOrString;
import io.fabric8.utils.Strings;

import java.util.List;
import java.util.Map;

/**
 * A DTO for specifying the parameters to use to configure the json generation
 */
public class GenerateDTO {
    protected String name;
    protected String dockerImage;
    private String imagePullPolicy;
    protected String containerName;
    protected String template;
    protected Map<String, String> labels;
    protected List<EnvVar> environmentVariables;
    protected List<Port> ports;
    private Integer replicaCount;
    private String serviceName;
    private String replicationControllerName;
    private Integer servicePort;
    private IntOrString serviceContainerPort;

    public String getDockerImage() {
        return dockerImage;
    }

    public void setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
    }


    public List<EnvVar> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(List<EnvVar> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getReplicaCount() {
        if (replicaCount == null) {
            replicaCount = 1;
        }
        return replicaCount;
    }

    public void setReplicaCount(Integer replicaCount) {
        this.replicaCount = replicaCount;
    }

    public String getContainerName() {
        if (Strings.isNullOrBlank(containerName) && Strings.isNotBlank(name)) {
            // lets generate the docker container name if its not present
            containerName = Strings.splitCamelCase(name, "-").toLowerCase();
        }
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public List<Port> getPorts() {
        return ports;
    }

    public void setPorts(List<Port> ports) {
        this.ports = ports;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getReplicationControllerName() {
        return replicationControllerName;
    }

    public void setReplicationControllerName(String replicationControllerName) {
        this.replicationControllerName = replicationControllerName;
    }

    public Integer getServicePort() {
        return servicePort;
    }

    public void setServicePort(Integer servicePort) {
        this.servicePort = servicePort;
    }

    public IntOrString getServiceContainerPort() {
        return serviceContainerPort;
    }

    public void setServiceContainerPort(IntOrString serviceContainerPort) {
        this.serviceContainerPort = serviceContainerPort;
    }

    public String getImagePullPolicy() {
        return imagePullPolicy;
    }

    public void setImagePullPolicy(String imagePullPolicy) {
        this.imagePullPolicy = imagePullPolicy;
    }
}
