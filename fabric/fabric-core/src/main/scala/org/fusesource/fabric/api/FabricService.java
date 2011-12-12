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

    static final String DEFAULT_REPO_URI = "http://repo.fusesource.com/nexus/content/groups/public-snapshots/";

    Agent[] getAgents();

    Agent getAgent(String name);

    Agent createAgent(String name);

    Agent createAgent(String url, String name);

    Agent createAgent(String url, String name, boolean isClusterServer, boolean debugAgent);

    /**
     * Creates multiple Agents.
     * Will create a number of Agents equal to the given number.
     * @param url
     * @param name
     * @param isClusterServer
     * @param debugAgent
     * @param number
     * @return
     */
    Agent[] createAgents(String url, String name, boolean isClusterServer, boolean debugAgent, int number);

    Agent createAgent(Agent parent, String name);

    Agent createAgent(Agent parent, String name, boolean debugAgent);

    Agent createAgent(CreateAgentArguments args, String name);

    /**
     * Create multiple agents where the name is used as a prefix
     */
    Agent[] createAgents(CreateAgentArguments args, String name, int number);

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

    Profile[] getProfiles(String version);

    Profile getProfile(String version, String name);

    Profile createProfile(String version, String name);

    void deleteProfile(Profile profile);

    Agent getCurrentAgent();

    String getCurrentAgentName();
}
