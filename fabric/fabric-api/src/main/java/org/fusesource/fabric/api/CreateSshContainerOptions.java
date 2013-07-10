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
package org.fusesource.fabric.api;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Arguments for creating a new container via SSH
 */
public class CreateSshContainerOptions extends CreateContainerBasicOptions<CreateSshContainerOptions> implements CreateRemoteContainerOptions {

    private static final long serialVersionUID = -1171578973712670970L;

    public static final String DEFAULT_PRIVATE_KEY_FILE = System.getProperty("user.home") + File.separatorChar + ".ssh" + File.separatorChar + "id_rsa";

    static final int DEFAULT_SSH_RETRIES = 1;
    static final int DEFAULT_SSH_PORT = 22;

    public static class Builder extends CreateContainerBasicOptions.Builder<Builder> {

        private String username;
        private String password;
        private String host;
        private int port = DEFAULT_SSH_PORT;
        private int sshRetries = DEFAULT_SSH_RETRIES;
        private int retryDelay = 1;
        private String privateKeyFile = DEFAULT_PRIVATE_KEY_FILE;
        private String passPhrase;
        private String path = "~/containers/";


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

        public CreateSshContainerOptions build() {
            return new CreateSshContainerOptions(bindAddress, resolver, globalResolver, manualIp, minimumPort,
                    maximumPort, profiles, zooKeeperServerPort, zookeeperPassword, agentEnabled, autoImportEnabled,
                    importPath, users, name, parent, "ssh", ensembleServer, preferredAddress, systemProperties,
                    number, proxyUri, zookeeperUrl, jvmOpts, version, username, password, host, port, sshRetries, retryDelay, privateKeyFile, passPhrase, path);
        }
    }


    private final String username;
    private final String password;
    private final String host;
    private final int port;
    private final int sshRetries;
    private final int retryDelay;
    private final String privateKeyFile;
    private final String passPhrase;
    private final String path;

    public CreateSshContainerOptions(String bindAddress, String resolver, String globalResolver, String manualIp, int minimumPort, int maximumPort, Set<String> profiles, int getZooKeeperServerPort, String zookeeperPassword, boolean agentEnabled, boolean autoImportEnabled, String importPath, Map<String, String> users, String name, String parent, String providerType, boolean ensembleServer, String preferredAddress, Map<String, Properties> systemProperties, int number, URI proxyUri, String zookeeperUrl, String jvmOpts, String version, String username, String password, String host, int port, int sshRetries, int retryDelay, String privateKeyFile, String passPhrase, String path) {
        super(bindAddress, resolver, globalResolver, manualIp, minimumPort, maximumPort, profiles, getZooKeeperServerPort, zookeeperPassword, agentEnabled, autoImportEnabled, importPath, users, name, parent, providerType, ensembleServer, preferredAddress, systemProperties, number, proxyUri, zookeeperUrl, jvmOpts, version);
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
        this.sshRetries = sshRetries;
        this.retryDelay = retryDelay;
        this.privateKeyFile = privateKeyFile;
        this.passPhrase = passPhrase;
        this.path = path;
    }

    @Override
    public CreateContainerOptions updateCredentials(String newUser, String newPassword) {
        return new CreateSshContainerOptions(bindAddress, resolver, globalResolver, manualIp, minimumPort,
                maximumPort, profiles, zooKeeperServerPort, zookeeperPassword, agentEnabled, autoImportEnabled,
                importPath, users, name, parent, "ssh", ensembleServer, preferredAddress, systemProperties,
                number, proxyUri, zookeeperUrl, jvmOpts, version, newUser != null ? newUser : username,
                newPassword != null ? newPassword : password, host, port, sshRetries, retryDelay, privateKeyFile, passPhrase, path);
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

    public String getPrivateKeyFile() {
        //We check for a parameter first as the privateKeyFile has a default value assigned.
        return privateKeyFile;
    }

    public String getPassPhrase() {
        return passPhrase;
    }

    public CreateSshContainerOptions clone() throws CloneNotSupportedException {
        return (CreateSshContainerOptions) super.clone();
    }

    @Override
    public String toString() {
        return "createSshContainer(" + getUsername() + "@" + getHost() + ":" + getPort() + " " + getPath() + ")";
    }
}
