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
    @Option(name = "--identity", required = true, description = "The cloud identity to use")
    private String identity;
    @Option(name = "--credential", required = true, description = "Credential to login to the cloud")
    private String credential;
    @Option(name = "--hardware", required = true, description = "Which hardware kind to use")
    private String hardwareId;
    @Option(name = "--instanceType", required = true, description = "Which kind of instance is required")
    private JCloudsInstanceType instanceType;
    @Option(name = "--image", required = true, description = "The image ID to use for the new node(s)")
    private String imageId;
    @Option(name = "--location", required = true, description = "The location to use to create the new node(s)")
    private String locationId;
    @Option(name = "--user", required = true, description = "User account to run on the new node(s)")
    private String user;
    @Option(name = "--owner", description = "Optional owner of images; only really used for EC2 and deprecated going forward")
    private String owner;
    @Option(name = "--group", description = "Group tag to use on the new node(s)")
    private String group;
    @Option(name = "--proxy-uri", description = "Maven proxy URL to use")
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
