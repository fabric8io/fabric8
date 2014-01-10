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

package io.fabric8.docker.api.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.docker.api.container.ContainerConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class ImageInfo {
    @Getter
    @Setter
    @JsonProperty("ID")
    private String id;
    @Getter
    @Setter
    @JsonProperty("Parent")
    private String parent;
    @Getter
    @Setter
    @JsonProperty("Created")
    private String created;
    @Getter
    @Setter
    @JsonProperty("Container")
    private String container;
    @JsonProperty("ContainerConfig")
    private ContainerConfig containerConfig;
    @JsonProperty("Size")
    private long size;
}