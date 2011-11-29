/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.commands.support.AgentCreateSupport;

@Command(name = "agent-create", scope = "fabric", description = "Creates one or more new agents")
public class AgentCreate extends AgentCreateSupport {

    @Option(name = "--parent", multiValued = false, required = false, description = "Parent agent ID")
    private String parent;

    @Option(name = "--url", multiValued = false, required = false, description = "The URL")
    private String url;
    @Argument(index = 0, required = true, description = "The name of the agent to be created. When creating multiple agents it serves as a prefix")
    protected String name;
    @Argument(index = 1, required = false, description = "The number of agents that should be created")
    protected int number = 1;

    @Override
    protected Object doExecute() throws Exception {
        if (url == null && parent == null) {
            throw new Exception("Either an url or a parent must be specified");
        }
        if (url == null && parent != null) {
            url = "child://" + parent;
        }
        Agent[] children = fabricService.createAgents(url, name, isClusterServer, debugAgent, number);
        setProfiles(children);
        return null;
    }

}
