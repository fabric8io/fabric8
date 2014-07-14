/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.api;

import java.util.Arrays;
import java.util.List;

/**
 * Defines the scaling requirements for ssh containers
 */
public class SshScalingRequirements {
    private List<String> hostPatterns;

    @Override
    public String toString() {
        return "SshScalingRequirements{" +
                "hostPatterns=" + hostPatterns +
                '}';
    }

    // Fluent API for configuring the requirements
    //-------------------------------------------------------------------------

    public SshScalingRequirements hostPatterns(List<String> rootContainerPatterns) {
        setHostPatterns(rootContainerPatterns);
        return this;
    }

    public SshScalingRequirements hostPatterns(String... rootContainerPatterns) {
        return hostPatterns(Arrays.asList(rootContainerPatterns));
    }


    // Properties
    //-------------------------------------------------------------------------

    /**
     * Returns a list of patterns to match host names or aliases used for creating ssh containers
     */
    public List<String> getHostPatterns() {
        return hostPatterns;
    }

    public void setHostPatterns(List<String> hostPatterns) {
        this.hostPatterns = hostPatterns;
    }}
