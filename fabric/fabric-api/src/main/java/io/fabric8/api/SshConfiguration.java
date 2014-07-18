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
 * Represents the configuration used when the autoscaler creates containers via ssh
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SshConfiguration {
    private List<SshHostConfiguration> hosts = new ArrayList<>();
    private String defaultPath;
    private Integer defaultPort;
    private String defaultUsername;
    private String defaultPassword;
    private List<String> fallbackRepositories;
    private String defaultPassPhrase;
    private String defaultPrivateKeyFile;

    public SshHostConfiguration getHost(String hostName) {
        if (hosts != null) {
            for (SshHostConfiguration host : hosts) {
                if (hostName.equals(host.getHostName())) {
                    return host;
                }
            }
        }
        return null;
    }


    public void addHost(SshHostConfiguration configuration) {
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
    public SshHostConfiguration host(String hostName) {
        SshHostConfiguration answer = getHost(hostName);
        if (answer == null) {
            answer = new SshHostConfiguration(hostName);
            addHost(answer);
        }
        return answer;
    }

    public SshConfiguration defaultPort(Integer defaultPort) {
        setDefaultPort(defaultPort);
        return this;
    }

    public SshConfiguration defaultPath(String defaultPath) {
        setDefaultPath(defaultPath);
        return this;
    }

    public SshConfiguration defaultUsername(final String defaultUsername) {
        this.defaultUsername = defaultUsername;
        return this;
    }

    public SshConfiguration defaultPassword(final String defaultPassword) {
        this.defaultPassword = defaultPassword;
        return this;
    }

    public SshConfiguration defaultPassPhrase(final String defaultPassPhrase) {
        this.defaultPassPhrase = defaultPassPhrase;
        return this;
    }

    public SshConfiguration defaultPrivateKeyFile(final String defaultPrivateKeyFile) {
        this.defaultPrivateKeyFile = defaultPrivateKeyFile;
        return this;
    }

    public SshConfiguration fallbackRepositories(final List<String> fallbackRepositories) {
        this.fallbackRepositories = fallbackRepositories;
        return this;
    }

    public SshConfiguration fallbackRepositories(final String... fallbackRepositories) {
        return fallbackRepositories(Arrays.asList(fallbackRepositories));
    }

    // Properties
    //-------------------------------------------------------------------------

    public List<SshHostConfiguration> getHosts() {
        return hosts;
    }

    public void setHosts(List<SshHostConfiguration> hosts) {
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

    public List<String> getFallbackRepositories() {
        return fallbackRepositories;
    }

    public void setFallbackRepositories(List<String> fallbackRepositories) {
        this.fallbackRepositories = fallbackRepositories;
    }

    public String getDefaultPassPhrase() {
        return defaultPassPhrase;
    }

    public void setDefaultPassPhrase(String defaultPassPhrase) {
        this.defaultPassPhrase = defaultPassPhrase;
    }

    public String getDefaultPrivateKeyFile() {
        return defaultPrivateKeyFile;
    }

    public void setDefaultPrivateKeyFile(String defaultPrivateKeyFile) {
        this.defaultPrivateKeyFile = defaultPrivateKeyFile;
    }

}
