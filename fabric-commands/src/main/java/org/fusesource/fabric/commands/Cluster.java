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

@Command(name = "cluster", scope = "fabric", description = "Create or modify a ZooKeeper cluster", detailedDescription = "classpath:cluster.txt")
public class Cluster extends OsgiCommandSupport {

    private ZooKeeperClusterService service;

    @Option(name = "--add", description = "Add agents to the cluster")
    private boolean add;

    @Option(name = "--remove", description = "Remove agents from the cluster")
    private boolean remove;

    @Option(name = "--clean", description = "Clean local zookeeper cluster and configurations")
    private boolean clean;

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
        if (clean) {
            if (add || remove) {
               throw new IllegalArgumentException("Invalid syntax.  zk-cluster --clean should be used without add or remove options");
            }
            service.clean();

            // TODO we may have a timing issue here so we may want to wait a little bit
            if (agents != null && !agents.isEmpty()) {
                service.createCluster(agents);
            }
        } else if (agents == null || agents.isEmpty()) {
            List<String> cluster = service.getClusterAgents();
            if (cluster == null || cluster.isEmpty()) {
                throw new IllegalStateException("No ZooKeeper server set up.  Use \"fabric:cluster " + System.getProperty("karaf.name") + "\" to set up one.");

            } else {
                System.out.println("ZooKeeper cluster url:");
                System.out.println("  " + service.getZooKeeperUrl());
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
