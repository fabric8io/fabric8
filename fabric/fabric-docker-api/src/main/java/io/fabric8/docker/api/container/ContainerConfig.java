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
package io.fabric8.docker.api.container;

import io.fabric8.docker.api.AbstractDockerDTO;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class ContainerConfig extends AbstractDockerDTO {
    private String hostname = "";
    private String domainname = "";
    private String user = "";
    private long memory;
    private long memorySwap;
    private long cpuShares;
    private boolean attachStdin;
    private boolean attachStdout;
    private boolean attachStderr;
    private String portSpecs = "";
    private Map<String, Object> exposedPorts;
    private boolean tty;
    private boolean openStdin;
    private boolean stdinOnce;
    private List<String> env;
    private String[] cmd;
    private String dns = "";
    private String image = "";
    private Map<String, Object> volumes;
    private String volumesFrom = "";
    private String workingDir = "";
    private String entrypoint = "";
    private boolean networkDisabled;
}
