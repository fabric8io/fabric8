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

package io.fabric8.partition;

import org.codehaus.jackson.annotate.JsonProperty;
import io.fabric8.groups.NodeState;

import java.net.URL;

public class WorkerNode extends NodeState {

    @JsonProperty
    String[] items;

    @JsonProperty
    String[] services;

    public WorkerNode() {
    }

    public WorkerNode(String id) {
        super(id);
    }

    public String[] getItems() {
        return items;
    }

    public void setItems(String[] items) {
        this.items = items;
    }

    public String[] getServices() {
        return services;
    }

    public void setServices(String[] services) {
        this.services = services;
    }

    @Override
    public String toString() {
        return "WorkerNode{" +
                "id='" + id + '\'' +
                ", container='" + container + '\'' +
                ", services='" + services + '\'' +
                ", items='" + items + '\'' +
                '}';
    }
}
