/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.commands;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.CreateSshAgentArguments;

import java.net.URI;

@Command(name = "agent-create-ssh", scope = "fabric", description = "Creates one or more new agents via SSH")
public class CreateAgentSsh extends CreateAgentSupport {

    @Option(name = "--host", required = true)
    private String host;
    @Option(name = "--path")
    private String path;
    @Option(name = "--user")
    private String user;
    @Option(name = "--password")
    private String password;
    @Option(name = "--port")
    private Integer port;
    @Option(name = "--ssh-retries")
    private Integer sshRetries;
    @Option(name = "--proxy-uri")
    private URI proxyUri;

    @Override
    protected Object doExecute() throws Exception {
        CreateSshAgentArguments args = new CreateSshAgentArguments();
        args.setClusterServer(isClusterServer);
        args.setDebugAgent(debugAgent);
        args.setNumber(number);
        args.setHost(host);
        args.setPath(path);
        args.setPassword(password);
        args.setProxyUri(proxyUri);
        args.setUsername(user);
        if (port != null) {
            args.setPort(port);
        }
        if (sshRetries != null) {
            args.setSshRetries(sshRetries);
        }
        fabricService.createAgents(args, name, number);
        return null;
    }


}
