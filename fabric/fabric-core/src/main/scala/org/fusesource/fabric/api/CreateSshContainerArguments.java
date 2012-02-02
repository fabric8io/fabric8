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

import java.io.Serializable;

/**
 * Arguments for creating a new container via SSH
 */
public class CreateSshContainerArguments extends BasicCreateContainerArguements implements CreateContainerArguments, Serializable {

    private static final long serialVersionUID = -1171578973712670970L;

    private String username;
    private String password;
    private String host;
    private int port = 22;
    private String path = "/usr/local/fusesource/container";
    private int sshRetries = 6;
    private int retryDelay = 1;

    @Override
    public String toString() {
        return "createSshContainer(" + username + "@" + host + ":" + port + " " + path + ")";
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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
}
