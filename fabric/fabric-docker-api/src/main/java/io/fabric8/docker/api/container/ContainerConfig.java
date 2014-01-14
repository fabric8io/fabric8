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

import java.util.List;
import java.util.Map;

@Data
public class ContainerConfig {
    @JsonProperty("Hostname")
    private String hostname;
    @JsonProperty("Domainname")
    private String domainName;
    @JsonProperty("User")
    private String user;
    @JsonProperty("Memory")
    private long memory;
    @JsonProperty("MemorySwap")
    private long memorySwap;
    @JsonProperty("CpuShares")
    private long cpuShares;
    @JsonProperty("AttachStdin")
    private boolean attachStdin;
    @JsonProperty("AttachStdout")
    private boolean attachStdout;
    @JsonProperty("AttachStderr")
    private boolean attachStderr;
    @JsonProperty("PortSpecs")
    private String portSpecs;
    @JsonProperty("ExposedPorts")
    private Map<String, Object> exposedPorts;
    @JsonProperty("Tty")
    private boolean tty;
    @JsonProperty("OpenStdin")
    private boolean openStdin;
    @JsonProperty("StdinOnce")
    private boolean stdinOnce;
    @JsonProperty("Env")
    private List<String> env;
    @JsonProperty("Cmd")
    private String[] cmd;
    @JsonProperty("Dns")
    private String dns;
    @JsonProperty("Image")
    private String image;
    @JsonProperty("Volumes")
    private Map<String, Object> volumes;
    @JsonProperty("VolumesFrom")
    private String volumesFrom;
    @JsonProperty("WorkingDir")
    private String workingDir;
    @JsonProperty("Entrypoint")
    private String entrypoint;
    @JsonProperty("NetworkDisabled")
    private boolean networkDisabled;
}
