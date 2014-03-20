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

import java.util.List;

public class CreateDockerContainerMetadata extends CreateContainerBasicMetadata<CreateDockerContainerOptions> {
    private final String id;
    private final List<String> warnings;

    public CreateDockerContainerMetadata(String id, List<String> warnings) {
        this.id = id;
        this.warnings = warnings;
    }

    public String getId() {
        return id;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}


