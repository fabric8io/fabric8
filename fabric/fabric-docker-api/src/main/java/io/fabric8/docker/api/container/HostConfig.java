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

import io.fabric8.docker.api.AbstractDockerDTO;

import java.util.Map;

import lombok.Data;

@Data
public class HostConfig extends AbstractDockerDTO {
    private String[] binds;
    private String containerIDFile;
    private Map<String, String> lxcConf;
    private boolean privileged;
    private Map<String, String> PortBindings;
    private Map<String, String> Links;
    private boolean publishAllPorts;
}