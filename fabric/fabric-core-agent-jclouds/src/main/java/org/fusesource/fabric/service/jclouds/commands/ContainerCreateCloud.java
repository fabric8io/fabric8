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
package org.fusesource.fabric.service.jclouds.commands;

import java.net.URI;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.*;
import org.fusesource.fabric.boot.commands.support.ContainerCreateSupport;

@Command(name = "container-create-cloud", scope = "fabric", description = "Creates one or more new containers on the cloud")
public class ContainerCreateCloud extends ContainerCreateSupport {

    static final String DISPLAY_FORMAT = "%22s %-9s %-30s %-30s";
    static final String[] OUTPUT_HEADERS = {"[id]", "[status]", "[container]", "[public addresses]"};

    @Option(name = "--provider", required = true, description = "The cloud provider name")
    private String providerName;
    @Option(name = "--os-family", multiValued = false, required = false, description = "OS Family")
    private String osFamily = "ubuntu";
    @Option(name = "--os-version", multiValued = false, required = false, description = "OS Version")
    private String osVersion = "11.";
    @Option(name = "--identity", required = false, description = "The cloud identity to use")
    private String identity;
    @Option(name = "--credential", required = false, description = "Credential to login to the cloud")
    private String credential;
    @Option(name = "--hardware", required = false, description = "Which hardware kind to use")
    private String hardwareId;
    @Option(name = "--instanceType", required = false, description = "Which kind of instance is required")
    private JCloudsInstanceType instanceType = JCloudsInstanceType.Smallest;
    @Option(name = "--image", required = false, description = "The image ID to use for the new node(s)")
    private String imageId;
    @Option(name = "--location", required = false, description = "The location to use to create the new node(s)")
    private String locationId;
    @Option(name = "--user", required = false, description = "User account to use on the new node(s)")
    private String user;
    @Option(name = "--public-key-file", required = false, description = "Path to the public key file to use for authenticating to the container")
    private String publicKeyFile;
    @Option(name = "--owner", description = "Optional owner of images; only really used for EC2 and deprecated going forward")
    private String owner;
    @Option(name = "--group", description = "Group tag to use on the new node(s)")
    private String group = "fabric";
    @Option(name = "--proxy-uri", description = "Maven proxy URL to use")
    private URI proxyUri;
    @Argument(index = 0, required = true, description = "The name of the container to be created. When creating multiple containers it serves as a prefix")
    protected String name;
    @Argument(index = 1, required = false, description = "The number of containers that should be created")
    protected int number = 1;

    @Override
    protected Object doExecute() throws Exception {
        // validate input before creating containers
        preCreateContainer(name);

        CreateContainerOptions args = CreateContainerOptionsBuilder.jclouds()
        .name(name)
        .resolver(resolver)
        .ensembleServer(isEnsembleServer)
        .credential(credential)
        .group(group)
        .hardwareId(hardwareId)
        .identity(identity)
        .osFamily(osFamily)
        .osVersion(osVersion)
        .imageId(imageId)
        .instanceType(instanceType)
        .locationId(locationId)
        .number(number)
        .owner(owner)
        .publicKeyFile(publicKeyFile)
        .providerName(providerName)
        .user(user)
        .proxyUri(proxyUri != null ? proxyUri : fabricService.getMavenRepoURI())
        .zookeeperUrl(fabricService.getZookeeperUrl())
        .jvmOpts(jvmOpts);

        CreateContainerMetadata[] metadatas = fabricService.createContainers(args);
        // display containers
        displayContainers(metadatas);
        // and set its profiles and versions after creation
        postCreateContainers(metadatas);
        return null;
    }

    protected void displayContainers(CreateContainerMetadata[] metadatas) {
        System.out.println(String.format(DISPLAY_FORMAT,OUTPUT_HEADERS));
        if (metadatas != null && metadatas.length > 0) {
            for (CreateContainerMetadata ccm : metadatas) {
                CreateJCloudsContainerMetadata metadata = (CreateJCloudsContainerMetadata) ccm;
                String status = "success";
                if (ccm.getFailure() != null) {
                    status = "failed";
                }
                System.out.println(String.format(DISPLAY_FORMAT, metadata.getNodeId(), status, metadata.getContainerName(), metadata.getPublicAddresses()));
            }
        }
    }

    @Override
    protected void preCreateContainer(String name) {
        super.preCreateContainer(name);
        // validate number is not out of bounds
        if (number < 1 || number > 999) {
            // for cloud we accept 3 digits
            throw new IllegalArgumentException("The number of containers must be between 1 and 999.");
        }

        if (osFamily == null && imageId == null) {
            System.out.println("Using Ubuntu 11.04");
            osFamily = "ubuntu";
            osVersion = "11.04";
        }

        if (isEnsembleServer && number > 1) {
            throw new IllegalArgumentException("Can not create a new ZooKeeper ensemble on multiple containers.  Create the containers and then use the fabric:create command instead.");
        }
    }
}
