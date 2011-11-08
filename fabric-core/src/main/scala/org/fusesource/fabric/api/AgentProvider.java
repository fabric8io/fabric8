/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api;

import java.net.URI;

/**
 * A Factory that creates {@link Agent}.
 */
public interface AgentProvider {

    static final String DEBUG_AGNET=" -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005";
    static final String CLUSTER_SERVER_AGENT=" -D"+ZooKeeperClusterService.CLUSTER_AUTOSTART_PROPERTY+"=true";
    static final String PROTOCOL = "fabric.agent.protocol";

    /**
     * Creates an {@link Agent} with the given name pointing to the specified zooKeeperUrl.
     * @param proxyUri The uri of the maven proxy to use.
     * @param agentUri The uri that contains required information to build the Agent.
     * @param name The name of the Agent.
     * @param zooKeeperUrl The url of Zoo Keeper.
     */
    void create(URI proxyUri, URI agentUri, String name, String zooKeeperUrl);

    /**
     * Creates an {@link Agent} with the given name pointing to the specified zooKeeperUrl.
     * @param proxyUri The uri of the maven proxy to use.
     * @param agentUri The uri that contains required information to build the Agent.
     * @param name The name of the Agent.
     * @param zooKeeperUrl The url of Zoo Keeper.
     * @param isClusterServer Marks that the Agent will have the role of cluster server.
     * @param debugAgent Flag used to enable debugging on the new Agent.
     */
    void create(URI proxyUri, URI agentUri, String name, String zooKeeperUrl, boolean isClusterServer, boolean debugAgent);

    /**
     * Creates an {@link Agent} with the given name pointing to the specified zooKeeperUrl.
     * @param proxyUri The uri of the maven proxy to use.
     * @param agentUri The uri that contains required information to build the Agent.
     * @param name The name of the Agent.
     * @param zooKeeperUrl The url of Zoo Keeper.
     * @param server Marks that the Agent will have the role of cluster server.
     * @param debugAgent Flag used to enable debugging on the new Agent.
     * @param number The number of Agents to create.
     */
    void create(URI proxyUri, URI agentUri, String name, String zooKeeperUrl, boolean isClusterServer, boolean debugAgent, int number);

    /**
     * Creates an agent using a set of arguments
     */
    boolean create(CreateAgentArguments args, String name, String zooKeeperUrl) throws Exception;
}
