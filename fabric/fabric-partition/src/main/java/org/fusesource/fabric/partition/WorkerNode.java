/*
 * Copyright 2010 Red Hat, Inc.
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

package org.fusesource.fabric.partition;

import org.codehaus.jackson.annotate.JsonProperty;
import org.fusesource.fabric.groups.NodeState;

public class WorkerNode implements NodeState {

    @JsonProperty
    String id;

    @JsonProperty
    String container;

    @JsonProperty
    String[] services;

    @JsonProperty
    String[] partitions;

    @JsonProperty
    String url;

    /**
     * The id of the cluster node.  There can be multiple node with this ID,
     * but only the first node in the cluster will be the master for for it.
     */
    @Override
    public String id() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String agent) {
        this.container = agent;
    }

    public String[] getServices() {
        return services;
    }

    public void setServices(String[] services) {
        this.services = services;
    }

    public String[] getPartitions() {
        return partitions;
    }

    public void setPartitions(String[] partitions) {
        this.partitions = partitions;
    }

    @Override
    public String toString() {
        return "MemberNode{" +
                "id='" + id + '\'' +
                ", url='" + url + '\'' +
                ", container='" + container + '\'' +
                ", partitions='" + partitions + '\'' +
                '}';
    }
}
