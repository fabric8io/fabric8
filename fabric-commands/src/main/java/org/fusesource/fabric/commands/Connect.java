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

import java.util.List;

@Command(name = "connect", scope = "fabric", description = "Connect to an existing agent")
public class Connect extends FabricCommand {

    @Option(name="-u", aliases={"--username"}, description="Remote user name (Default: admin)", required = false, multiValued = false)
    private String username = "admin";

    @Option(name="-p", aliases={"--password"}, description="Remote user password (Default: admin)", required = false, multiValued = false)
    private String password = "admin";

    @Argument(index = 0, name="agent", description="The agent name", required = true, multiValued = false)
    private String agent = null;

    @Argument(index = 1, name = "command", description = "Optional command to execute", required = false, multiValued = true)
    private List<String> command;

    protected Object doExecute() throws Exception {
        String cmdStr = "";
        if (command != null) {
            StringBuilder sb = new StringBuilder();
            for (String cmd : command) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(cmd);
            }
            cmdStr = "'" + sb.toString().replaceAll("'", "\\'") + "'";
        }

        Agent a = fabricService.getAgent(agent);
        if (a == null) {
            throw new IllegalArgumentException("Agent " + agent + " does not exist.");
        }
        String[] ssh = a.getSshUrl().split(":");
        session.execute("ssh -l " + username + " -P " + password + " -p " + ssh[1] + " " + ssh[0] + " " + cmdStr);
        return null;
    }

}
