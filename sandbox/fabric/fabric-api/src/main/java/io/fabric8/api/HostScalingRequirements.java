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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Arrays;
import java.util.List;

/**
 * Base class for host based scaling requirement configurations so we can share code between
 * classes like {@link io.fabric8.api.SshScalingRequirements} and {@link io.fabric8.api.DockerScalingRequirements}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class HostScalingRequirements<T extends HostScalingRequirements> {
    private List<String> hostPatterns;
    private List<String> hostTags;

    @Override
    public String toString() {
        String hostPatternsText = hostPatterns != null && hostPatterns.size() > 0 ? "hostPatterns=" + hostPatterns : "";
        String hostTagsText = hostTags != null && hostTags.size() > 0 ? "hostTags=" + hostTags : "";
        String separator = hostPatternsText.length() > 0 && hostTagsText.length() > 0 ? ", " : "";
        return getClass().getName() + "{" +
                hostPatternsText + separator + hostTagsText +
                '}';
    }

    // Fluent API for configuring the requirements
    //-------------------------------------------------------------------------

    public T hostPatterns(List<String> rootContainerPatterns) {
        setHostPatterns(rootContainerPatterns);
        return (T) this;
    }

    public T hostPatterns(String... rootContainerPatterns) {
        return hostPatterns(Arrays.asList(rootContainerPatterns));
    }

    public T hostTags(final List<String> hostTags) {
        this.hostTags = hostTags;
        return (T) this;
    }

    public T hostTags(String... hostTags) {
        return hostTags(Arrays.asList(hostTags));
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
    }

    public List<String> getHostTags() {
        return hostTags;
    }

    public void setHostTags(List<String> hostTags) {
        this.hostTags = hostTags;
    }
}
