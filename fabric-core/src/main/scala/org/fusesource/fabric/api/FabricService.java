/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.api;

import java.net.URI;

public interface FabricService {

    Agent[] getAgents();

    Agent getAgent(String name);

    Agent createAgent(String name);

    Agent createAgent(String url, String name);

    Agent createAgent(String url, String name, boolean debugAgent);

    Agent createAgent(Agent parent, String name);

    Agent createAgent(Agent parent, String name, boolean debugAgent);

    Agent createAgent(CreateAgentArguments args, String name);

    /**
     * Uses the given parent agent to create the new agent (so that locally
     * we don't have to have all the plugins like ssh and jclouds available)
     */
    Agent createAgent(Agent parent, CreateAgentArguments args, String name);

    Version getDefaultVersion();

    void setDefaultVersion( Version version );

    Version[] getVersions();

    Version getVersion(String name);

    Version createVersion(String version);

    Version createVersion(Version parent, String version);

    /**
     * Returns the current maven proxy repository to use to create new agents
     */
    URI getMavenRepoURI();
}
