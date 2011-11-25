/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.commands;

import java.util.Collections;
import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.Profile;

@Command(name = "agent-create", scope = "fabric", description = "Creates one or more new agents")
public class CreateAgent extends CreateAgentSupport {

    @Option(name = "--parent", multiValued = false, required = false)
    private String parent;

    @Option(name = "--url", multiValued = false, required = false)
    private String url;

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
