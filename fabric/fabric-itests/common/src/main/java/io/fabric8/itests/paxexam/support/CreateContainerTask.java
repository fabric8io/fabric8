/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.itests.paxexam.support;

import io.fabric8.api.Container;
import io.fabric8.api.CreateContainerBasicOptions;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceLocator;
import io.fabric8.api.ServiceProxy;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

public class CreateContainerTask implements Callable<Set<ContainerProxy>> {

    private final ServiceProxy<FabricService> fabricServiceProxy;
    private final CreateContainerBasicOptions options;


    public CreateContainerTask(ServiceProxy<FabricService> fabricServiceProxy, CreateContainerBasicOptions  options) {
        this.fabricServiceProxy = fabricServiceProxy;
        this.options = options;
    }

    @Override
    public Set<ContainerProxy> call() throws Exception {
        Set<ContainerProxy> containers = new HashSet<ContainerProxy>();
        FabricService fabricService = fabricServiceProxy.getService();
        CreateContainerMetadata[] allMetadata = fabricService.createContainers(options);
        if (allMetadata != null && allMetadata.length > 0) {
            for (CreateContainerMetadata metadata : allMetadata) {
                Container container = metadata.getContainer();
                containers.add(ContainerProxy.wrap(container, fabricServiceProxy));
                if (!metadata.isSuccess()) {
                    throw new FabricException("Failed to create container." , metadata.getFailure());
                }
            }
        }

        return containers;
    }
}
