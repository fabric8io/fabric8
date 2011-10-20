/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api;

import java.io.Serializable;

/**
 * Arguments for creating a new agent via SSH
 */
public class CreateSshAgentArguments implements CreateAgentArguments, Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String host;
    private int port = 22;
    private String path = "/usr/local/fusesource/agent";
    private int sshRetries = 6;
    private int retryDelay = 1;
    private boolean debugAgent;

    @Override
    public String toString() {
        return "createSshAgent(" + username + "@" + host + ":" + port + " " + path + ")";
    }

    public boolean isDebugAgent() {
        return debugAgent;
    }

    public void setDebugAgent(boolean debugAgent) {
        this.debugAgent = debugAgent;
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
