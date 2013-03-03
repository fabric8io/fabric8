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

/**
 * Arguments for creating a new container via SSH
 */
public class CreateSshContainerOptions extends CreateContainerBasicOptions<CreateSshContainerOptions> implements CreateRemoteContainerOptions<CreateSshContainerOptions> {

    private static final long serialVersionUID = -1171578973712670970L;

    public static final String DEFAULT_PRIVATE_KEY_FILE = System.getProperty("user.home") + File.separatorChar + ".ssh" + File.separatorChar + "id_rsa";

    static final int DEFAULT_SSH_RETRIES = 1;
    static final int DEFAULT_SSH_PORT = 22;

    private String username;
    private String password;
    private String host;
    private int port = DEFAULT_SSH_PORT;
    private int sshRetries = DEFAULT_SSH_RETRIES;
    private int retryDelay = 1;
    private String privateKeyFile = DEFAULT_PRIVATE_KEY_FILE;
    private String passPhrase;
    private CreateEnsembleOptions createEnsembleOptions = CreateEnsembleOptions.build();
    private String path = "~/containers/";

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

    public CreateSshContainerOptions port(int port) {
        this.port = port;
        return this;
    }

    public CreateSshContainerOptions path(final String path) {
        this.path = path;
        return this;
    }

    public CreateSshContainerOptions sshRetries(int sshRetries) {
        this.sshRetries = sshRetries;
        return this;
    }

    public CreateSshContainerOptions retryDelay(int retryDelay) {
        this.retryDelay = retryDelay;
        return this;
    }

    public CreateSshContainerOptions privateKeyFile(final String privateKeyFile) {
        this.privateKeyFile = privateKeyFile;
        return this;
    }

    public CreateSshContainerOptions passPhrase(final String passPhrase) {
        this.passPhrase = passPhrase;
        return this;
    }

    public CreateSshContainerOptions createEnsembleOptions(final CreateEnsembleOptions createEnsembleOptions) {
        this.createEnsembleOptions = createEnsembleOptions;
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(int retryDelay) {
        this.retryDelay = retryDelay;
    }

    public String getPrivateKeyFile() {
        //We check for a parameter first as the privateKeyFile has a default value assigned.
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

    public CreateEnsembleOptions getCreateEnsembleOptions() {
        return createEnsembleOptions;
    }

    public void setCreateEnsembleOptions(CreateEnsembleOptions createEnsembleOptions) {
        this.createEnsembleOptions = createEnsembleOptions;
    }

	public CreateSshContainerOptions clone() throws CloneNotSupportedException {
		return (CreateSshContainerOptions) super.clone();
	}
}
