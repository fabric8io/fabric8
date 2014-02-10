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

import java.util.List;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class Info extends AbstractDockerDTO {
    private boolean debug;
    private int containers;
    private int images;
    private String driver;
    private List<List<String>> driverStatus;
    @JsonProperty("NFd")
    private int nFd;
    @JsonProperty("NGoroutines")
    private int nGoroutines;
    @JsonProperty("IPv4Forwarding")
    private boolean iPV4Forwarding;
    @JsonProperty("LXCVersion")
    private String lXCVersion;
    @JsonProperty("NEventsListener")
    private int nEventsListener;
    private String kernelVersion;
    private String indexServerAddress;
    private boolean memoryLimit;
    private boolean swapLimit;
}