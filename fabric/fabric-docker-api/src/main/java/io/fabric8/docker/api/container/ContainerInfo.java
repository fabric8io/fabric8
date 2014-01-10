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
public class ContainerInfo {
    @Getter
    @Setter
    @JsonProperty("ID")
    private String id;
    @Getter
    @Setter
    @JsonProperty("Created")
    private String created;
    @Getter
    @Setter
    @JsonProperty("Path")
    private String path;
    @Getter
    @Setter
    @JsonProperty("Args")
    private String[] args;
    @Getter
    @Setter
    @JsonProperty("Config")
    private ContainerConfig config;
    @Getter
    @Setter
    @JsonProperty("State")
    private State state;
    @Getter
    @Setter
    @JsonProperty("Image")
    private String image;
    @Getter
    @Setter
    @JsonProperty("NetworkSettings")
    private NetworkSettings networkSettings;
    @Getter
    @Setter
    @JsonProperty("SysInitPath")
    private String sysInitPath;
    @Getter
    @Setter
    @JsonProperty("ResolvConfPath")
    private String resolvConfPath;
    @Getter
    @Setter
    @JsonProperty("HostnamePath")
    private String hostnamePath;
    @Getter
    @Setter
    @JsonProperty("HostsPath")
    private String hostsPath;
    @Getter
    @Setter
    @JsonProperty("Name")
    private String name;
    @Getter
    @Setter
    @JsonProperty("Driver")
    private String driver;
    @Getter
    @Setter
    @JsonProperty("Volumes")
    private Map<String, String> volumes;
    @Getter
    @Setter
    @JsonProperty("VolumesRW")
    private Map<String, String> volumesRW;
    @Getter
    @Setter
    @JsonProperty("HostConfig")
    private HostConfig hostConfig;
}
