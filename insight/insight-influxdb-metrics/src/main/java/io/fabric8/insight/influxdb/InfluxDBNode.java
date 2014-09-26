/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.insight.influxdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.groups.NodeState;

import java.util.Arrays;

public class InfluxDBNode extends NodeState {

    @JsonProperty
    String[] services;

    @JsonProperty
    String url;

    public InfluxDBNode() {
    }

    public InfluxDBNode(String id, String container) {
        super(id, container);
    }

    public String[] getServices() {
        return services;
    }

    public void setServices(String[] services) {
        this.services = services;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "InfluxDBNode{" +
                "id=" + id + "," +
                "container=" + container + "," +
                "services=" + Arrays.toString(services) +
                '}';
    }
}