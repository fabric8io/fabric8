/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.commands;

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.fusesource.fabric.api.ZooKeeperClusterService;

@Command(name = "zk-quorum", scope = "fabric", description = "Create a ZooKeeper cluster", detailedDescription = "classpath:zk-cluster.txt")
public class ZkCluster extends OsgiCommandSupport {

    private ZooKeeperClusterService service;

    @Option(name = "--add", description = "Add agents to the cluster")
    private boolean add;

    @Option(name = "--remove", description = "Remove agents from the cluster")
    private boolean remove;

    @Argument(required = false, multiValued = true, description = "List of agents")
    private List<String> agents;

    public ZooKeeperClusterService getService() {
        return service;
    }

    public void setService(ZooKeeperClusterService service) {
        this.service = service;
    }

    @Override
    protected Object doExecute() throws Exception {
        if (agents == null || agents.isEmpty()) {
            List<String> cluster = service.getClusterAgents();
            if (cluster == null || cluster.isEmpty()) {
                throw new IllegalStateException("No ZooKeeper server set up.  Use \"fabric:zk-cluster " + System.getProperty("karaf.name") + "\" to set up one.");

            } else {
                System.out.println("ZooKeeper agents: ");
                for (String agent : service.getClusterAgents()) {
                    System.out.println("  " + agent);
                }
            }
        } else if (add) {
            service.addToCluster(agents);
        } else if (remove) {
            service.removeFromCluster(agents);
        } else {
            service.createCluster(agents);
        }
        return null;
    }

}
