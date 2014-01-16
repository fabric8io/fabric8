/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
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

package io.fabric8.docker.api;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.fabric8.docker.api.container.Port;
import io.fabric8.docker.api.support.DockerPropertyNamingStrategy;
import lombok.Data;

import java.util.List;

@Data
@JsonNaming(DockerPropertyNamingStrategy.class)
public class Container {
    private String id;
    private String image;
    private String command;
    private long created;
    private String status;
    private List<Port> ports;
    private long sizeRw;
    private long sizeRootFs;
    private List<String> names;
}
