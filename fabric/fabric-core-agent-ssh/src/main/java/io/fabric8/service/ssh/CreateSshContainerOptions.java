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
package io.fabric8.service.ssh;

import io.fabric8.api.CreateContainerBasicOptions;
import io.fabric8.api.CreateContainerOptions;
import io.fabric8.api.CreateRemoteContainerOptions;
import io.fabric8.api.jcip.NotThreadSafe;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Arguments for creating a new container via SSH
 */
@NotThreadSafe // because base class isn't
public class CreateSshContainerOptions extends CreateContainerBasicOptions<CreateSshContainerOptions> implements CreateRemoteContainerOptions {

    private static final long serialVersionUID = -1171578973712670970L;

    public static final String DEFAULT_PRIVATE_KEY_FILE = System.getProperty("user.home") + File.separatorChar + ".ssh" + File.separatorChar + "id_rsa";

    static final int DEFAULT_SSH_RETRIES = 1;
    static final int DEFAULT_SSH_PORT = 22;

    @JsonProperty
    private final String username;
    @JsonProperty
    private final String password;
    @JsonProperty
    private final String host;
    @JsonProperty
    private final int port;
    @JsonProperty
    private final int sshRetries;
    @JsonProperty
    private final int retryDelay;
    @JsonProperty
    private final String privateKeyFile;
    @JsonProperty
    private final String passPhrase;
    @JsonProperty
    private final String path;

    @JsonProperty
    private final Map<String, String> environmentalVariables; // keep imutable

    CreateSshContainerOptions(String bindAddress, String resolver, String globalResolver, String manualIp, int minimumPort, int maximumPort, Set<String> profiles, String version, Map<String, String> dataStoreProperties, int zooKeeperServerPort, int zooKeeperServerConnectionPort, String zookeeperPassword, boolean ensembleStart, boolean agentEnabled, boolean autoImportEnabled, String importPath, Map<String, String> users, String name, String parent, String providerType, boolean ensembleServer, String preferredAddress, Map<String, Properties> systemProperties, int number, URI proxyUri, String zookeeperUrl, String jvmOpts, boolean adminAccess, boolean clean, String username, String password, String host, int port, int sshRetries, int retryDelay, String privateKeyFile, String passPhrase, String path, Map<String, String> environmentalVariables) {
        super(bindAddress, resolver, globalResolver, manualIp, minimumPort, maximumPort, profiles, version, dataStoreProperties, zooKeeperServerPort, zooKeeperServerConnectionPort, zookeeperPassword, ensembleStart, agentEnabled, false, 0, autoImportEnabled, importPath, users, name, parent, providerType, ensembleServer, preferredAddress, systemProperties, number, proxyUri, zookeeperUrl, jvmOpts, adminAccess, clean);
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
        this.sshRetries = sshRetries;
        this.retryDelay = retryDelay;
        this.privateKeyFile = privateKeyFile;
        this.passPhrase = passPhrase;
        this.path = path;
        this.environmentalVariables = Collections.unmodifiableMap(new HashMap<String, String>(environmentalVariables));
    }

    @Override
    public CreateContainerOptions updateCredentials(String newUser, String newPassword) {
        return new CreateSshContainerOptions(getBindAddress(), getResolver(), getGlobalResolver(), getManualIp(), getMinimumPort(),
                getMaximumPort(), getProfiles(), getVersion(), getDataStoreProperties(), getZooKeeperServerPort(), getZooKeeperServerConnectionPort(), getZookeeperPassword(), isEnsembleStart(), isAgentEnabled(), isAutoImportEnabled(),
                getImportPath(), getUsers(), getName(), getParent(), "ssh", isEnsembleServer(), getPreferredAddress(), getSystemProperties(),
                getNumber(), getProxyUri(), getZookeeperUrl(), getJvmOpts(), isAdminAccess(), isClean(),
                newUser != null ? newUser : username, newPassword != null ? newPassword : password, host, port, sshRetries, retryDelay, privateKeyFile, passPhrase, path, environmentalVariables);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }


    public int getPort() {
        return port;
    }

    public int getSshRetries() {
        return sshRetries;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getEnvironmentalVariables() {
        return environmentalVariables;
    }

    public String getPrivateKeyFile() {
        //We check for a parameter first as the privateKeyFile has a default value assigned.
        return privateKeyFile;
    }

    public String getPassPhrase() {
        return passPhrase;
    }

    @Override
    public String getHostNameContext() {
        return "none";
    }

    public CreateSshContainerOptions clone() throws CloneNotSupportedException {
        return (CreateSshContainerOptions) super.clone();
    }

    @Override
    public String toString() {
        return "createSshContainer(" + getUsername() + "@" + getHost() + ":" + getPort() + " " + getPath() + ")";
    }

    public static class Builder extends CreateContainerBasicOptions.Builder<Builder> {

        @JsonProperty
        private String username;
        @JsonProperty
        private String password;
        @JsonProperty
        private String host;
        @JsonProperty
        private int port = DEFAULT_SSH_PORT;
        @JsonProperty
        private int sshRetries = DEFAULT_SSH_RETRIES;
        @JsonProperty
        private int retryDelay = 1;
        @JsonProperty
        private String privateKeyFile = DEFAULT_PRIVATE_KEY_FILE;
        @JsonProperty
        private String passPhrase;
        @JsonProperty
        private String path = "~/containers/";
        @JsonProperty
        private Map<String, String> environmentalVariables = new HashMap<String, String>();


        public Builder username(final String username) {
            this.username = username;
            return this;
        }

        public Builder password(final String password) {
            this.password = password;
            return this;
        }

        public Builder host(final String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder path(final String path) {
            this.path = path;
            return this;
        }

        public Builder environmentalVariables(Map<String, String> environmentalVariables) {
            this.environmentalVariables = environmentalVariables;
            return this;
        }

        public Builder environmentalVariable(String key, String value) {
            this.environmentalVariables.put(key, value);
            return this;
        }

        public Builder environmentalVariable(String entry) {
            if (entry.contains("=")) {
                String key = entry.substring(0, entry.indexOf("="));
                String value = entry.substring(entry.indexOf("=") + 1);
                environmentalVariable(key, value);
            }
            return this;
        }

        public Builder environmentalVariable(List<String> entries) {
            if (entries != null) {
                for (String entry : entries) {
                    environmentalVariable(entry);
                }
            }
            return this;
        }

        public Builder sshRetries(int sshRetries) {
            this.sshRetries = sshRetries;
            return this;
        }

        public Builder retryDelay(int retryDelay) {
            this.retryDelay = retryDelay;
            return this;
        }

        public Builder privateKeyFile(final String privateKeyFile) {
            this.privateKeyFile = privateKeyFile;
            return this;
        }

        public Builder passPhrase(final String passPhrase) {
            this.passPhrase = passPhrase;
            return this;
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

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getSshRetries() {
            return sshRetries;
        }

        public void setSshRetries(int sshRetries) {
            this.sshRetries = sshRetries;
        }

        public int getRetryDelay() {
            return retryDelay;
        }

        public void setRetryDelay(int retryDelay) {
            this.retryDelay = retryDelay;
        }

        public String getPrivateKeyFile() {
            return privateKeyFile;
        }

        public void setPrivateKeyFile(String privateKeyFile) {
            this.privateKeyFile = privateKeyFile;
        }

        public String getPassPhrase() {
            return passPhrase;
        }

        public void setPassPhrase(String passPhrase) {
            this.passPhrase = passPhrase;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public void setEnvironmentalVariables(Map<String, String> environmentalVariables) {
            this.environmentalVariables = environmentalVariables;
        }


        public CreateSshContainerOptions build() {
            return new CreateSshContainerOptions(getBindAddress(), getResolver(), getGlobalResolver(), getManualIp(), getMinimumPort(),
                    getMaximumPort(), getProfiles(), getVersion(), getDataStoreProperties(), getZooKeeperServerPort(), getZooKeeperServerConnectionPort(), getZookeeperPassword(), isEnsembleStart(), isAgentEnabled(), isAutoImportEnabled(),
                    getImportPath(), getUsers(), getName(), getParent(), "ssh", isEnsembleServer(), getPreferredAddress(), getSystemProperties(),
                    getNumber(), getProxyUri(), getZookeeperUrl(), getJvmOpts(), isAdminAccess(), false, username, password, host, port, sshRetries, retryDelay, privateKeyFile, passPhrase, path, environmentalVariables);
        }
    }
}
