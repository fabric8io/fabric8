/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.docker.api.container;

import io.fabric8.docker.api.AbstractDockerDTO;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@EqualsAndHashCode(callSuper=false)
public class ContainerInfo extends AbstractDockerDTO {
    @JsonProperty("ID")
    private String iD;
    private String created;
    private String path;
    private String[] args;
    private ContainerConfig config;
    private State state;
    private String image;
    private NetworkSettings networkSettings;
    private String sysInitPath;
    private String resolvConfPath;
    private String hostnamePath;
    private String hostsPath;
    private String name;
    private String driver;
    private Map<String, String> volumes;
    private Map<String, String> volumesRW;
    private HostConfig hostConfig;
}
