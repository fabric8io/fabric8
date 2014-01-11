/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.fabric8.docker.provider;

import io.fabric8.api.CreateContainerBasicMetadata;
import io.fabric8.docker.api.container.ContainerConfig;
import io.fabric8.docker.api.container.ContainerCreateStatus;

public class CreateDockerContainerMetadata extends CreateContainerBasicMetadata<CreateDockerContainerOptions> {
    private final String id;
    private final String[] warnings;

    public static CreateDockerContainerMetadata newInstance(ContainerConfig containerConfig, ContainerCreateStatus status) {
        return new CreateDockerContainerMetadata(status.getId(), status.getWarnings());
    }

    public CreateDockerContainerMetadata(String id, String[] warnings) {
        this.id = id;
        this.warnings = warnings;
    }

    public String getId() {
        return id;
    }

    public String[] getWarnings() {
        return warnings;
    }
}


