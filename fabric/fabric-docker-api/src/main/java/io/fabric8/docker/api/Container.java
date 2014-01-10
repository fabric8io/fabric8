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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.docker.api.container.Port;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@ToString
public class Container {
    @Getter
    @Setter
    @JsonProperty("Id")
    private String id;
    @Getter
    @Setter
    @JsonProperty("Image")
    private String image;
    @Getter
    @Setter
    @JsonProperty("Command")
    private String command;
    @Getter
    @Setter
    @JsonProperty("Created")
    private long created;
    @Getter
    @Setter
    @JsonProperty("Status")
    private String status;
    @Getter
    @Setter
    @JsonProperty("Ports")
    private List<Port> ports;
    @Getter
    @Setter
    @JsonProperty("SizeRw")
    private long sizeRw;
    @Getter
    @Setter
    @JsonProperty("SizeRootFs")
    private long sizeRootFs;
    @Getter
    @Setter
    @JsonProperty("Names")
    private List<String> names;
}
