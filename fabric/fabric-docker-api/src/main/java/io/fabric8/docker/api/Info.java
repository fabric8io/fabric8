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
public class Info {
    @JsonProperty("Debug")
    private boolean debug;
    @JsonProperty("Containers")
    private int containers;
    @JsonProperty("Images")
    private int images;
    @JsonProperty("Driver")
    private String driver;
    @JsonProperty("DriverStatus")
    private List<List<String>> driverStatus;
    @JsonProperty("NFd")
    private int nfd;
    @JsonProperty("NGoroutines")
    private int ngoRoutines;
    @JsonProperty("IPv4Forwarding")
    private boolean ipV4Forwarding;
    @JsonProperty("LXCVersion")
    private String lxcVersion;
    @JsonProperty("NEventsListener")
    private int neventsListener;
    @JsonProperty("KernelVersion")
    private String kernelVersion;
    @JsonProperty("IndexServerAddress")
    private String indexServerAddress;
    @JsonProperty("MemoryLimit")
    private boolean memoryLimit;
    @JsonProperty("SwapLimit")
    private boolean swapLimit;

}
