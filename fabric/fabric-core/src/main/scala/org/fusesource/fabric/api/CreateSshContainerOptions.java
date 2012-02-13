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

/**
 * Arguments for creating a new container via SSH
 */
public class CreateSshContainerOptions extends CreateContainerBasicOptions<CreateSshContainerOptions> {

    private static final long serialVersionUID = -1171578973712670970L;

    static final Integer DEFAULT_SSH_RETRIES = 5;
    static final Integer DEFAULT_SSH_PORT = 22;

    private String username;
    private String password;
    private String host;
    private Integer port = DEFAULT_SSH_PORT;
    private String path = "/usr/local/fusesource/container";
    private Integer sshRetries = DEFAULT_SSH_RETRIES;
    private Integer retryDelay = 1;

    public CreateSshContainerOptions() {
        this.providerType = "ssh";
    }

    @Override
    public String toString() {
        return "createSshContainer(" + getUsername() + "@" + getHost() + ":" + getPort() + " " + getPath() + ")";
    }


    public CreateSshContainerOptions username(final String username) {
        this.username = username;
        return this;
    }

    public CreateSshContainerOptions password(final String password) {
        this.password = password;
        return this;
    }

    public CreateSshContainerOptions host(final String host) {
        this.host = host;
        return this;
    }

    public CreateSshContainerOptions port(final Integer port) {
        this.port = port;
        return this;
    }

    public CreateSshContainerOptions path(final String path) {
        this.path = path;
        return this;
    }

    public CreateSshContainerOptions sshRetries(final Integer sshRetries) {
        this.sshRetries = sshRetries;
        return this;
    }

    public CreateSshContainerOptions retryDelay(final Integer retryDelay) {
        this.retryDelay = retryDelay;
        return this;
    }

    public String getUsername() {
        try {
            return username != null && !username.isEmpty() ? username : getProviderURI().getUserInfo().split(":")[0];
        } catch (Exception ex) {
            throw new IllegalStateException("Username should be part of the url or explicitly specified");
        }
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        try {
        return password != null && !password.isEmpty() ? password : getProviderURI().getUserInfo().split(":")[1];
        }catch (Exception ex) {
            throw new IllegalStateException("Password should be part of the url or explicitly specified");
        }
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHost() {
        return host != null && !host.isEmpty() ? host : getProviderURI().getHost();
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port != null ? port :
                (getProxyUri().getPort() != 0 ? getProxyUri().getPort() : DEFAULT_SSH_PORT);
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getSshRetries() {
        return sshRetries != null ? sshRetries : DEFAULT_SSH_RETRIES;
    }

    public void setSshRetries(Integer sshRetries) {
        this.sshRetries = sshRetries;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(Integer retryDelay) {
        this.retryDelay = retryDelay;
    }
}
