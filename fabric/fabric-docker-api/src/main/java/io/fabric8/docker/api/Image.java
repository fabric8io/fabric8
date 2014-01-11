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
import lombok.Data;

import java.util.List;

@Data
public class Image {
    @JsonProperty("Id")
    private String id;
    @JsonProperty("RepoTags")
    private List<String> repoTags;
    @JsonProperty("Created")
    private long created;
    @JsonProperty("Size")
    private long size;
    @JsonProperty("VirtualSize")
    private long virtualSize;
    @JsonProperty("ParentId")
    private String parentId;

    // still required?
    @JsonProperty("Repository")
    private String repository;
}
