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

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.fusesource.fabric.api.CreateContainerMetadata;
import org.fusesource.fabric.api.CreateContainerOptions;
import org.fusesource.fabric.api.CreateContainerOptionsBuilder;
import org.fusesource.fabric.boot.commands.support.ContainerCreateSupport;

@Command(name = "container-create-child", scope = "fabric", description = "Creates one or more child containers", detailedDescription = "classpath:containerCreateChild.txt")
public class ContainerCreateChild extends ContainerCreateSupport {

    @Argument(index = 0, required = true, description = "Parent containers ID")
    protected String parent;
    @Argument(index = 1, required = true, description = "The name of the containers to be created. When creating multiple containers it serves as a prefix")
    protected String name;
    @Argument(index = 2, required = false, description = "The number of containers that should be created")
    protected int number = 1;

    @Override
    protected Object doExecute() throws Exception {
        // validate input before creating containers
        preCreateContainer(name);
        
        // okay create child container
        String url = "child://" + parent;
        CreateContainerOptions options = CreateContainerOptionsBuilder.child()
                .name(name)
                .parent(parent)
                .providerUri(url)
                .resolver(resolver)
                .ensembleServer(isEnsembleServer)
                .number(number)
                .zookeeperUrl(fabricService.getZookeeperUrl())
                .zookeeperPassword(fabricService.getZookeeperPassword())
                .jvmOpts(jvmOpts);

        CreateContainerMetadata[] metadatas = fabricService.createContainers(options);
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
        if (isEnsembleServer && number > 1) {
            throw new IllegalArgumentException("Can not create a new ZooKeeper ensemble on multiple containers.  Create the containers first and then use the fabric:create command instead.");
        }
    }
}
