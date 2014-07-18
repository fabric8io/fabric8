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
package io.fabric8.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents the configuration used when the autoscaler creates containers via docker
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DockerConfiguration {
    private List<DockerHostConfiguration> hosts = new ArrayList<>();

    public DockerHostConfiguration getHost(String hostName) {
        if (hosts != null) {
            for (DockerHostConfiguration host : hosts) {
                if (hostName.equals(host.getHostName())) {
                    return host;
                }
            }
        }
        return null;
    }


    public void addHost(DockerHostConfiguration configuration) {
        if (hosts == null) {
            hosts = new ArrayList<>();
        }
        hosts.add(configuration);
    }

    // Fluid API to make configuration easier
    //-------------------------------------------------------------------------

    /**
     * Returns the host configuration for the given host name; lazily creating a new one if one does not exist yet
     */
    public DockerHostConfiguration host(String hostName) {
        DockerHostConfiguration answer = getHost(hostName);
        if (answer == null) {
            answer = new DockerHostConfiguration(hostName);
            addHost(answer);
        }
        return answer;
    }

    // Properties
    //-------------------------------------------------------------------------

    public List<DockerHostConfiguration> getHosts() {
        return hosts;
    }

    public void setHosts(List<DockerHostConfiguration> hosts) {
        this.hosts = hosts;
    }
}
