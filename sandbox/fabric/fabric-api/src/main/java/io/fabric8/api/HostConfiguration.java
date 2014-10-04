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

import java.util.Arrays;
import java.util.List;

/**
 * Base class for host based configurations so we can share code between
 * classes like {@link io.fabric8.api.SshHostConfiguration} and {@link io.fabric8.api.DockerHostConfiguration}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class HostConfiguration<T extends HostConfiguration> {
    private String hostName;
    private Integer port;
    private String username;
    private String password;
    private Integer maximumContainerCount;
    private List<String> tags;

    protected HostConfiguration() {
    }

    protected HostConfiguration(String hostName) {
        this.hostName = hostName;
    }

    // Fluid API
    //-------------------------------------------------------------------------
    public T hostName(String hostName) {
        setHostName(hostName);
        return (T) this;
    }

    public T port(Integer port) {
        setPort(port);
        return (T) this;
    }

    public T username(final String username) {
        this.username = username;
        return (T) this;
    }

    public T password(final String password) {
        this.password = password;
        return (T) this;
    }

    public T maximumContainerCount(final Integer maximumContainerCount) {
        this.maximumContainerCount = maximumContainerCount;
        return (T) this;
    }

    public T tags(final List<String> tags) {
        this.tags = tags;
        return (T) this;
    }

    public T tags(String... tags) {
        return tags(Arrays.asList(tags));
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getMaximumContainerCount() {
        return maximumContainerCount;
    }

    public void setMaximumContainerCount(Integer maximumContainerCount) {
        this.maximumContainerCount = maximumContainerCount;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
