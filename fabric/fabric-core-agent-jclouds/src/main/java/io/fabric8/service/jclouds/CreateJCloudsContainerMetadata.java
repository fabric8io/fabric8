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

package io.fabric8.service.jclouds;

import io.fabric8.api.CreateContainerBasicMetadata;

import java.util.LinkedHashSet;
import java.util.Set;


public class CreateJCloudsContainerMetadata extends CreateContainerBasicMetadata<CreateJCloudsContainerOptions> {

    static final long serialVersionUID = 858054431079073318L;

    private String nodeId;
    private String hostname;
    private Set<String> publicAddresses = new LinkedHashSet<String>();
    private Set<String> privateAddresses = new LinkedHashSet<String>();
    //Note: The identity/credential properties below refer to the user account and not to the provider account.
    private String identity;
    private String credential;

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
        getContainerConfiguration().put("localhostname", hostname);

    }

    public Set<String> getPublicAddresses() {
        return publicAddresses;
    }

    public void setPublicAddresses(Set<String> publicAddresses) {
        this.publicAddresses = publicAddresses;
        if (publicAddresses != null && !publicAddresses.isEmpty()) {
            getContainerConfiguration().put("publicip", publicAddresses.iterator().next());
        }
    }

    public Set<String> getPrivateAddresses() {
        return privateAddresses;
    }

    public void setPrivateAddresses(Set<String> privateAddresses) {
        this.privateAddresses = privateAddresses;
        if (privateAddresses != null && !privateAddresses.isEmpty()) {
            getContainerConfiguration().put("localip", privateAddresses.iterator().next());
        }
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getContainerName()).append("[")
                .append("public ip=[ ");
        for (String ip : publicAddresses) {
            builder.append(ip).append(" ");
        }
        builder.append("]]");

        return builder.toString();
    }
}
