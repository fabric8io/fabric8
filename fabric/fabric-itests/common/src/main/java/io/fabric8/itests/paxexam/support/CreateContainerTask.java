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
package io.fabric8.itests.paxexam.support;

import io.fabric8.api.Container;
import io.fabric8.api.CreateContainerBasicOptions;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricService;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

public class CreateContainerTask implements Callable<Set<Container>> {

    private final FabricService fabricService;
    private final CreateContainerBasicOptions.Builder optionsBuilder;


    public CreateContainerTask(FabricService fabricService, CreateContainerBasicOptions.Builder optionsBuilder) {
        this.fabricService = fabricService;
        this.optionsBuilder = optionsBuilder;
    }

    @Override
    public Set<Container> call() throws Exception {
        Set<Container> containers = new HashSet<Container>();
        CreateContainerMetadata[] allMetadata = fabricService.createContainers(optionsBuilder.build());
        if (allMetadata != null && allMetadata.length > 0) {
            for (CreateContainerMetadata metadata : allMetadata) {
                Container container = metadata.getContainer();
                containers.add(container);
                if (!metadata.isSuccess()) {
                    throw new FabricException("Failed to create container." , metadata.getFailure());
                }
            }
        }

        return containers;
    }
}
