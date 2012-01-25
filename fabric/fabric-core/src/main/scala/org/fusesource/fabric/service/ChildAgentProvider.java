/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.service;

import java.net.URI;
import org.apache.karaf.admin.management.AdminServiceMBean;
import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.AgentProvider;
import org.fusesource.fabric.api.CreateAgentArguments;

public class ChildAgentProvider implements AgentProvider {

    final FabricServiceImpl service;

    public ChildAgentProvider(FabricServiceImpl service) {
        this.service = service;
    }

    /**
     * Creates an {@link org.fusesource.fabric.api.Agent} with the given name pointing to the specified zooKeeperUrl.
     *
     * @param proxyUri
     * @param agentUri     The uri that contains required information to build the Agent.
     * @param name         The name of the Agent.
     * @param zooKeeperUrl The url of Zoo Keeper.
     * @param debugAgent   Flag used to enable debugging on the new Agent.
     * @param number       The number of Agents to create.
     * @param isClusterServer       Marks if the agent will have the role of the cluster server.
     * @param debugAgent
     */
    public void create(URI proxyUri, final URI agentUri, final String name, final String zooKeeperUrl, final boolean isClusterServer, final boolean debugAgent, final int number) {
        String parentName = FabricServiceImpl.getParentFromURI(agentUri);
        final Agent parent = service.getAgent(parentName);
        AgentTemplate agentTemplate = service.getAgentTemplate(parent);

        //Retrieve the credentials from the URI if available.
        String ui = agentUri.getUserInfo();
        String[] uip = ui != null ? ui.split(":") : null;
        if (uip != null) {
            agentTemplate.setLogin(uip[0]);
            agentTemplate.setPassword(uip[1]);
        }

        agentTemplate.execute(new AgentTemplate.AdminServiceCallback<Object>() {
            public Object doWithAdminService(AdminServiceMBean adminService) throws Exception {
                String javaOpts = zooKeeperUrl != null ? "-Dzookeeper.url=\"" + zooKeeperUrl + "\" -Xmx512M -server" : "";
                if(debugAgent) {
                    javaOpts += DEBUG_AGNET;
                }
                if(isClusterServer) {
                    javaOpts += CLUSTER_SERVER_AGENT;
                }
                String features = "fabric-agent";
                String featuresUrls = "mvn:org.fusesource.fabric/fuse-fabric/1.1-SNAPSHOT/xml/features";

                for (int i = 1; i <= number; i++) {
                    String agentName = name;
                    if (number > 1) {
                        agentName += i;
                    }
                    adminService.createInstance(agentName, 0, 0, 0, null, javaOpts, features, featuresUrls);
                    adminService.startInstance(agentName, null);
                }
                return null;
            }
        });
    }

    /**
     * Creates an {@link org.fusesource.fabric.api.Agent} with the given name pointing to the specified zooKeeperUrl.
     *
     * @param proxyUri
     * @param agentUri      The uri that contains required information to build the Agent.
     * @param name          The name of the Agent.
     * @param zooKeeperUrl  The url of Zoo Keeper.
     * @param debugAgent    Flag used to enable debugging on the new Agent.
     */
    @Override
    public void create(URI proxyUri, URI agentUri, String name, String zooKeeperUrl, boolean isClusterServer, boolean debugAgent) {
        create(proxyUri, agentUri, name, zooKeeperUrl,isClusterServer,debugAgent,1);
    }

    /**
     * Creates an {@link org.fusesource.fabric.api.Agent} with the given name pointing to the specified zooKeeperUrl.
     *
     * @param proxyUri
     * @param agentUri     The uri that contains required information to build the Agent.
     * @param name         The name of the Agent.
     * @param zooKeeperUrl The url of Zoo Keeper.
     */
    @Override
    public void create(URI proxyUri, URI agentUri, String name, String zooKeeperUrl) {
        create(proxyUri, agentUri, name, zooKeeperUrl,false, false);
    }

    @Override
    public boolean create(CreateAgentArguments args, String name, String zooKeeperUrl) throws Exception {
        return false;
    }
}
