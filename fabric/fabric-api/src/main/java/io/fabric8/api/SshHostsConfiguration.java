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

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the host configurations used when the autoscaler creates containers via ssh
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SshHostsConfiguration {
    private Map<String,SshHostConfiguration> hosts = new HashMap<String,SshHostConfiguration>();
    private String defaultPath;
    private Integer defaultPort;
    private String defaultUsername;
    private String defaultPassword;

    public Map<String, SshHostConfiguration> getHosts() {
        return hosts;
    }


    // Fluid API to make configuration easier
    //-------------------------------------------------------------------------
    public SshHostsConfiguration defaultPort(Integer defaultPort) {
        setDefaultPort(defaultPort);
        return this;
    }

    public SshHostsConfiguration defaultPath(String defaultPath) {
        setDefaultPath(defaultPath);
        return this;
    }

    public SshHostsConfiguration defaultUsername(final String defaultUsername) {
        this.defaultUsername = defaultUsername;
        return this;
    }

    public SshHostsConfiguration defaultPassword(final String defaultPassword) {
        this.defaultPassword = defaultPassword;
        return this;
    }

    // Properties
    //-------------------------------------------------------------------------

    public void setHosts(Map<String, SshHostConfiguration> hosts) {
        this.hosts = hosts;
    }

    public String getDefaultPath() {
        return defaultPath;
    }

    public void setDefaultPath(String defaultPath) {
        this.defaultPath = defaultPath;
    }

    public Integer getDefaultPort() {
        return defaultPort;
    }

    public void setDefaultPort(Integer defaultPort) {
        this.defaultPort = defaultPort;
    }

    public String getDefaultUsername() {
        return defaultUsername;
    }

    public void setDefaultUsername(String defaultUsername) {
        this.defaultUsername = defaultUsername;
    }

    public String getDefaultPassword() {
        return defaultPassword;
    }

    public void setDefaultPassword(String defaultPassword) {
        this.defaultPassword = defaultPassword;
    }
}
