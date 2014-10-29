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
package io.fabric8.kubernetes.template;

import io.fabric8.utils.Strings;
import io.fabric8.kubernetes.api.model.Env;
import io.fabric8.kubernetes.api.model.Port;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A DTO for specifying the parameters to use to configure the template generation
 */
public class GenerateTemplateDTO {
    protected String name;
    protected String dockerImage;
    protected String containerName;
    protected String template;
    protected Map<String, String> labels;
    protected List<Env> environmentVariables;
    protected List<Port> ports;
    protected Map<String, Object> templateVariables;
    private Integer replicaCount;

    public String getDockerImage() {
        return dockerImage;
    }

    public void setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
    }

    public String getTemplate() {
        if (Strings.isNullOrBlank(template)) {
            template = TemplateGenerator.DEFAULT_TEMPLATE;
        }
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Map<String, Object> getTemplateVariables() {
        if (templateVariables == null) {
            templateVariables = new HashMap<>();
        }
        return templateVariables;
    }

    public void setTemplateVariables(Map<String, Object> templateVariables) {
        this.templateVariables = templateVariables;
    }

    public List<Env> getEnvironmentVariables() {
        if (environmentVariables == null) {
            environmentVariables = new ArrayList<>();
        }
        return environmentVariables;
    }

    public void setEnvironmentVariables(List<Env> environmentVariables) {
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
        if (labels == null) {
            labels = new HashMap<>();
        }
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public List<Port> getPorts() {
        if (ports == null) {
            ports = new ArrayList<>();
        }
        return ports;
    }

    public void setPorts(List<Port> ports) {
        this.ports = ports;
    }
}
