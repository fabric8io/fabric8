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
import lombok.Data;

import java.util.Map;

@Data
public class ContainerInfo {
    @JsonProperty("ID")
    private String id;
    @JsonProperty("Created")
    private String created;
    @JsonProperty("Path")
    private String path;
    @JsonProperty("Args")
    private String[] args;
    @JsonProperty("Config")
    private ContainerConfig config;
    @JsonProperty("State")
    private State state;
    @JsonProperty("Image")
    private String image;
    @JsonProperty("NetworkSettings")
    private NetworkSettings networkSettings;
    @JsonProperty("SysInitPath")
    private String sysInitPath;
    @JsonProperty("ResolvConfPath")
    private String resolvConfPath;
    @JsonProperty("HostnamePath")
    private String hostnamePath;
    @JsonProperty("HostsPath")
    private String hostsPath;
    @JsonProperty("Name")
    private String name;
    @JsonProperty("Driver")
    private String driver;
    @JsonProperty("Volumes")
    private Map<String, String> volumes;
    @JsonProperty("VolumesRW")
    private Map<String, String> volumesRW;
    @JsonProperty("HostConfig")
    private HostConfig hostConfig;
}
