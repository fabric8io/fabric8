/*
 * Copyright (C) 2011 FuseSource, Corp. All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the CDDL license
 * a copy of which has been included with this distribution in the license.txt file.
 */

package org.fusesource.fabric.api;

import java.io.Serializable;
import java.net.URI;

public class BasicCreateAgentArguements implements CreateAgentArguments, Serializable {

    private static final long serialVersionUID = 7806030498786182100L;

    protected boolean clusterServer;
    protected boolean debugAgent;
    protected int number = 1;
    protected URI proxyUri;


    public boolean isClusterServer() {
        return clusterServer;
    }

    public void setClusterServer(boolean clusterServer) {
        this.clusterServer = clusterServer;
    }

    public boolean isDebugAgent() {
        return debugAgent;
    }

    public void setDebugAgent(boolean debugAgent) {
        this.debugAgent = debugAgent;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public URI getProxyUri() {
        return proxyUri;
    }

    public void setProxyUri(URI proxyUri) {
        this.proxyUri = proxyUri;
    }
}
