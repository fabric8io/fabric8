/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service;

import org.apache.karaf.admin.management.AdminServiceMBean;
import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.AgentProvider;
import org.fusesource.fabric.api.CreateAgentArguments;

import java.net.URI;

public class ChildAgentProvider implements AgentProvider {

    final FabricServiceImpl service;

    public ChildAgentProvider(FabricServiceImpl service) {
        this.service = service;
    }

    /**
     * Creates an {@link org.fusesource.fabric.api.Agent} with the given name pointing to the specified zooKeeperUrl.
     *
     * @param agentUri     The uri that contains required information to build the Agent.
     * @param name         The name of the Agent.
     * @param zooKeeperUrl The url of Zoo Keeper.
     * @param debugAgent   Flag used to enable debugging on the new Agent.
     */
    public void create(final URI agentUri, final String name, final String zooKeeperUrl, final boolean debugAgent) {
        final Agent parent = service.getAgent(agentUri.getSchemeSpecificPart());
        service.getAgentTemplate(parent).execute(new AgentTemplate.AdminServiceCallback<Object>() {
            public Object doWithAdminService(AdminServiceMBean adminService) throws Exception {
                String javaOpts = zooKeeperUrl != null ? "-Dzookeeper.url=\"" + zooKeeperUrl + "\" -Xmx512M -server" : "";
                if(debugAgent) {
                    javaOpts += AgentProvider.DEBUG_AGNET;
                }
                String features = "fabric-agent";
                String featuresUrls = "mvn:org.fusesource.fabric/fabric-distro/1.1-SNAPSHOT/xml/features";
                adminService.createInstance(name, 0, 0, 0, null, javaOpts, features, featuresUrls);
                adminService.startInstance(name, null);
                return null;
            }
        });
    }

    /**
     * Creates an {@link org.fusesource.fabric.api.Agent} with the given name pointing to the specified zooKeeperUrl.
     *
     * @param agentUri     The uri that contains required information to build the Agent.
     * @param name         The name of the Agent.
     * @param zooKeeperUrl The url of Zoo Keeper.
     */
    @Override
    public void create(URI agentUri, String name, String zooKeeperUrl) {
        create(agentUri,name,zooKeeperUrl);
    }

    @Override
    public boolean create(CreateAgentArguments args, String name, String zooKeeperUrl) throws Exception {
        return false;
    }
}
