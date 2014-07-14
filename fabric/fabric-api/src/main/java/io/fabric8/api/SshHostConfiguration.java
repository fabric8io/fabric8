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

/**
 * Represents the configuration for a given host
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SshHostConfiguration {
    private String hostName;
    private String path;
    private Integer port;
    private String username;
    private String password;

    public SshHostConfiguration() {
    }

    /**
     * Defaults the host name value to the given host alias
     */
    public SshHostConfiguration(String hostAlias) {
        this.hostName = hostAlias;
    }

    // Fluid API
    //-------------------------------------------------------------------------
    public SshHostConfiguration hostName(String hostName) {
        setHostName(hostName);
        return this;
    }

    public SshHostConfiguration path(String path) {
        setPath(path);
        return this;
    }

    public SshHostConfiguration port(Integer port) {
        setPort(port);
        return this;
    }

    public SshHostConfiguration username(final String username) {
        this.username = username;
        return this;
    }

    public SshHostConfiguration password(final String password) {
        this.password = password;
        return this;
    }

    // Properties
    //-------------------------------------------------------------------------

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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
}
