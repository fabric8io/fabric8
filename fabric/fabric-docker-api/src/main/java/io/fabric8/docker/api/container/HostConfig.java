/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.docker.api.container;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@ToString
public class HostConfig {
    @Getter
    @Setter
    @JsonProperty("Binds")
    private String[] binds;
    @Getter
    @Setter
    @JsonProperty("ContainerIDFile")
    private String containerIDFile;
    @Getter
    @Setter
    @JsonProperty("LxcConf")
    private Map<String, String> lxcConf;
    @Getter
    @Setter
    @JsonProperty("Privileged")
    private boolean privileged;
    @Getter
    @Setter
    @JsonProperty("PortBindings")
    private Map<String, String> PortBindings;
    @Getter
    @Setter
    @JsonProperty("Links")
    private Map<String, String> Links;
    @Getter
    @Setter
    @JsonProperty("PublishAllPorts")
    private boolean publishAllPorts;
}
