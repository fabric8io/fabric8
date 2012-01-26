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
