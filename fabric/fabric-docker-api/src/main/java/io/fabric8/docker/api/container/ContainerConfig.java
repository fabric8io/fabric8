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
public class ContainerConfig {
    @Getter
    @Setter
    @JsonProperty("Hostname")
    private String hostname;
    @Getter
    @Setter
    @JsonProperty("Domainname")
    private String domainName;
    @Getter
    @Setter
    @JsonProperty("User")
    private String user;
    @Getter
    @Setter
    @JsonProperty("Memory")
    private long memory;
    @Getter
    @Setter
    @JsonProperty("MemorySwap")
    private long memorySwap;
    @Getter
    @Setter
    @JsonProperty("CpuShares")
    private long cpuShares;
    @Getter
    @Setter
    @JsonProperty("AttachStdin")
    private boolean attachStdin;
    @Getter
    @Setter
    @JsonProperty("AttachStdout")
    private boolean attachStdout;
    @Getter
    @Setter
    @JsonProperty("AttachStderr")
    private boolean attachStderr;
    @Getter
    @Setter
    @JsonProperty("PortSpecs")
    private String portSpecs;
    @Getter
    @Setter
    @JsonProperty("ExposedPorts")
    private Map<String, Object> exposedPorts;
    @Getter
    @Setter
    @JsonProperty("Tty")
    private String tty;
    @Getter
    @Setter
    @JsonProperty("OpenStdin")
    private boolean openStdin;
    @Getter
    @Setter
    @JsonProperty("StdinOnce")
    private boolean stdinOnce;
    @Getter
    @Setter
    @JsonProperty("Env")
    private String evn;
    @Getter
    @Setter
    @JsonProperty("Cmd")
    private String[] cmd;
    @Getter
    @Setter
    @JsonProperty("Dns")
    private String dns;
    @Getter
    @Setter
    @JsonProperty("Image")
    private String image;
    @Getter
    @Setter
    @JsonProperty("Volumes")
    private Map<String, Object> volumes;
    @Getter
    @Setter
    @JsonProperty("VolumesFrom")
    private String volumesFrom;
    @Getter
    @Setter
    @JsonProperty("WorkingDir")
    private String workingDir;
    @Getter
    @Setter
    @JsonProperty("Entrypoint")
    private String entrypoint;
    @Getter
    @Setter
    @JsonProperty("NetworkDisabled")
    private boolean networkDisabled;


}
