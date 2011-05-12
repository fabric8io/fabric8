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

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.FabricService;

public class AgentCompleter implements Completer {

    protected FabricService fabricService;

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        for (Agent agent : fabricService.getAgents()) {
            delegate.getStrings().add(agent.getId());
        }
        return delegate.complete(buffer, cursor, candidates);
    }

}
