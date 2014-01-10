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
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@ToString
public class Info {
    @Getter
    @Setter
    @JsonProperty("Debug")
    private boolean debug;
    @Getter
    @Setter
    @JsonProperty("Containers")
    private int containers;
    @Getter
    @Setter
    @JsonProperty("Images")
    private int images;
    @Getter
    @Setter
    @JsonProperty("Driver")
    private String driver;
    @Getter
    @Setter
    @JsonProperty("DriverStatus")
    private List<List<String>> driverStatus;
    @Getter
    @Setter
    @JsonProperty("NFd")
    private int nfd;
    @Getter
    @Setter
    @JsonProperty("NGoroutines")
    private int ngoRoutines;
    @Getter
    @Setter
    @JsonProperty("IPv4Forwarding")
    private boolean ipV4Forwarding;
    @Getter
    @Setter
    @JsonProperty("LXCVersion")
    private String lxcVersion;
    @Getter
    @Setter
    @JsonProperty("NEventsListener")
    private int neventsListener;
    @Getter
    @Setter
    @JsonProperty("KernelVersion")
    private String kernelVersion;
    @Getter
    @Setter
    @JsonProperty("IndexServerAddress")
    private String indexServerAddress;
    @Getter
    @Setter
    @JsonProperty("MemoryLimit")
    private boolean memoryLimit;
    @Getter
    @Setter
    @JsonProperty("SwapLimit")
    private boolean swapLimit;

}
