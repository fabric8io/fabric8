/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api.environments;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a single environment
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Environment implements Comparable<Environment> {
    private String name;
    private String namespace;
    private String clusterAPiServer;
    private Integer order;
    private String key;


    @Override
    public String toString() {
        return "Environment{" +
                "name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                '}';
    }

    @Override
    public int compareTo(Environment that) {
        int answer = this.order() - that.order();
        if (answer == 0) {
            answer = this.name.compareTo(that.name);
        }
        return answer;
    }

    public int order() {
        return order != null ? order.intValue() : Integer.MAX_VALUE;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getClusterAPiServer() {
        return clusterAPiServer;
    }

    public void setClusterAPiServer(String clusterAPiServer) {
        this.clusterAPiServer = clusterAPiServer;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
