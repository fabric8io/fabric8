/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
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

        final Agent parent = service.getAgent(agentUri.getHost());
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
                String featuresUrls = "mvn:org.fusesource.fabric/fabric-distro/1.1-SNAPSHOT/xml/features";

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
