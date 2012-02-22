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
import org.fusesource.fabric.api.CreateContainerMetadata;
import org.fusesource.fabric.api.CreateContainerOptions;
import org.fusesource.fabric.api.CreateContainerOptionsBuilder;
import org.fusesource.fabric.commands.support.ContainerCreateSupport;

@Command(name = "container-create", scope = "fabric", description = "Creates one or more new containers")
public class ContainerCreate extends ContainerCreateSupport {

    @Option(name = "--parent", multiValued = false, required = false, description = "Parent container ID")
    protected String parent;
    @Option(name = "--url", multiValued = false, required = false, description = "The URL")
    protected String url;
    @Option(name = "--proxy-uri", description = "Maven proxy URL to use")
    protected URI proxyUri;
    @Argument(index = 0, required = true, description = "The name of the container to be created. When creating multiple containers it serves as a prefix")
    protected String name;
    @Argument(index = 1, required = false, description = "The number of containers that should be created")
    protected int number = 1;

    @Override
    protected Object doExecute() throws Exception {
        // validate input before creating containers
        preCreateContainer(name);

        String type = null;
        if (url == null && parent != null) {
            url = "child://" + parent;
            type = "child";
        } else if (parent == null && url != null) {
            URI uri = new URI(url);
            type = uri.getScheme();
            if ("child".equals(type)) {
                parent = uri.getHost();
            }
        }

        CreateContainerOptions args = CreateContainerOptionsBuilder.type(type)
                .name(name)
                .parent(parent)
                .number(number)
                .debugContainer(debugContainer)
                .ensembleServer(isEnsembleServer)
                .providerUri(url)
                .proxyUri(proxyUri != null ? proxyUri : fabricService.getMavenRepoURI())
                .zookeeperUrl(fabricService.getZookeeperUrl());

        CreateContainerMetadata[] metadatas = fabricService.createContainers(args);
        // display containers
        displayContainers(metadatas);
        // and set its profiles and versions after creation
        postCreateContainers(metadatas);
        return null;
    }

    @Override
    protected void preCreateContainer(String name) {
        super.preCreateContainer(name);

        // validate number is not out of bounds
        if (number < 1 || number > 99) {
            throw new IllegalArgumentException("The number of containers must be between 1 and 99.");
        }

        if (url == null && parent == null) {
            throw new IllegalArgumentException("Either an url or a parent must be specified");
        }
    }
}
