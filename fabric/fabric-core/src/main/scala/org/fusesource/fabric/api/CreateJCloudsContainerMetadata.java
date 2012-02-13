/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
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

package org.fusesource.fabric.api;

import java.util.LinkedHashSet;
import java.util.Set;

public class CreateJCloudsContainerMetadata extends CreateContainerBasicMetadata {

    private String nodeId;
    private String hostname;
    private Set<String> publicAddresses = new LinkedHashSet<String>();

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Set<String> getPublicAddresses() {
        return publicAddresses;
    }

    public void setPublicAddresses(Set<String> publicAddresses) {
        this.publicAddresses = publicAddresses;
    }
}
