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

import java.net.URI;

public class ChildAgentProvider implements AgentProvider {

    final FabricServiceImpl service;

    public ChildAgentProvider(FabricServiceImpl service) {
        this.service = service;
    }

    @Override
    public void create(final URI agentUri, final String name, final String zooKeeperUrl) {
        final Agent parent = service.getAgent(agentUri.getSchemeSpecificPart());
        service.getAgentTemplate(parent).execute(new AgentTemplate.AdminServiceCallback<Object>() {
            public Object doWithAdminService(AdminServiceMBean adminService) throws Exception {
                String javaOpts = zooKeeperUrl != null ? "-Dzookeeper.url=\"" + zooKeeperUrl + "\" -Xmx512M -server" : "";
                String features = "fabric-agent";
                String featuresUrls = "mvn:org.fusesource.fabric/fabric-distro/1.1-SNAPSHOT/xml/features";
                adminService.createInstance(name, 0, 0, 0, null, javaOpts, features, featuresUrls);
                adminService.startInstance(name, null);
                return null;
            }
        });
    }
}
