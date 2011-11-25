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
import org.fusesource.fabric.api.CreateJCloudsAgentArguments;
import org.fusesource.fabric.api.JCloudsInstanceType;
import org.fusesource.fabric.api.Profile;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@Command(name = "agent-create-cloud", scope = "fabric", description = "Creates one or more new agents on the cloud")
public class CreateAgentJclouds extends CreateAgentSupport {

    @Option(name = "--provider", required = true, description = "JClouds provider name")
    private String providerName;
    @Option(name = "--identity", required = true)
    private String identity;
    @Option(name = "--credential", required = true)
    private String credential;
    @Option(name = "--hardware", required = true)
    private String hardwareId;
    @Option(name = "--instanceType", required = true)
    private JCloudsInstanceType instanceType;
    @Option(name = "--image", required = true)
    private String imageId;
    @Option(name = "--location", required = true)
    private String locationId;
    @Option(name = "--user", required = true)
    private String user;
    @Option(name = "--owner")
    private String owner;
    @Option(name = "--group")
    private String group;
    @Option(name = "--proxy-uri")
    private URI proxyUri;

    @Override
    protected Object doExecute() throws Exception {
        CreateJCloudsAgentArguments args = new CreateJCloudsAgentArguments();
        args.setClusterServer(isClusterServer);
        args.setCredential(credential);
        args.setDebugAgent(debugAgent);
        args.setGroup(group);
        args.setHardwareId(hardwareId);
        args.setIdentity(identity);
        args.setImageId(imageId);
        args.setInstanceType(instanceType);
        args.setLocationId(locationId);
        args.setNumber(number);
        args.setOwner(owner);
        args.setProviderName(providerName);
        args.setUser(user);
        fabricService.createAgents(args, name, number);
        return null;
    }


}
