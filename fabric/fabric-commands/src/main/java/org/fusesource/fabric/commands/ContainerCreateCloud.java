/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.commands;

import java.net.URI;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.CreateJCloudsContainerArguments;
import org.fusesource.fabric.api.JCloudsInstanceType;
import org.fusesource.fabric.commands.support.ContainerCreateSupport;

@Command(name = "container-create-cloud", scope = "fabric", description = "Creates one or more new containers on the cloud")
public class ContainerCreateCloud extends ContainerCreateSupport {

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
    @Argument(index = 0, required = true, description = "The name of the container to be created. When creating multiple containers it serves as a prefix")
    protected String name;
    @Argument(index = 1, required = false, description = "The number of containers that should be created")
    protected int number = 1;

    @Override
    protected Object doExecute() throws Exception {
        // validate profiles exists before creating
        doValidateProfiles();

        // validate number is not out of bounds
        if (number < 1 || number > 999) {
            // for cloud we accept 3 digits
            throw new IllegalArgumentException("The number of containers must be between 1 and 999.");
        }

        CreateJCloudsContainerArguments args = new CreateJCloudsContainerArguments();
        args.setEnsembleServer(isEnsembleServer);
        args.setCredential(credential);
        args.setDebugContainer(debugContainer);
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
        if (proxyUri != null) {
            args.setProxyUri(proxyUri);
        } else {
            args.setProxyUri(fabricService.getMavenRepoURI());
        }
        Container[] containers = fabricService.createContainer(args, name, number);
        // and set its profiles after creation
        setProfiles(containers);
        return null;
    }


}
