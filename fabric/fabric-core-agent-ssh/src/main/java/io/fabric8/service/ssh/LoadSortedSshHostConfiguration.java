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
package io.fabric8.service.ssh;

import io.fabric8.api.SshHostConfiguration;

/**
 * Represents a sorted ordering over {@link SshHostConfiguration} objects
 * using the usage values of the underlying container to find the least loaded container
 */
public class LoadSortedSshHostConfiguration implements Comparable<LoadSortedSshHostConfiguration> {
    private final String hostAlias;
    private final SshHostConfiguration hostConfiguration;
    private final String profile;
    private final HostProfileCounter hostProfileCounter;
    private final int index;
    private final int containerCount;
    private final int profileCount;

    public LoadSortedSshHostConfiguration(String hostAlias, SshHostConfiguration hostConfiguration, String profile, HostProfileCounter hostProfileCounter, int index) {
        this.hostAlias = hostAlias;
        this.hostConfiguration = hostConfiguration;
        this.profile = profile;
        this.hostProfileCounter = hostProfileCounter;
        this.index = index;
        this.containerCount = hostProfileCounter.containerCount(hostAlias);
        this.profileCount = hostProfileCounter.profileCount(hostAlias, profile);
    }

    public SshHostConfiguration getHostConfiguration() {
        return hostConfiguration;
    }

    public HostProfileCounter getHostProfileCounter() {
        return hostProfileCounter;
    }

    public int getIndex() {
        return index;
    }

    public String getHostAlias() {
        return hostAlias;
    }

    public String getProfile() {
        return profile;
    }

    public int getContainerCount() {
        return containerCount;
    }

    public int getProfileCount() {
        return profileCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LoadSortedSshHostConfiguration that = (LoadSortedSshHostConfiguration) o;

        if (index != that.index) return false;
        if (hostAlias != null ? !hostAlias.equals(that.hostAlias) : that.hostAlias != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = hostAlias != null ? hostAlias.hashCode() : 0;
        result = 31 * result + index;
        return result;
    }

    @Override
    public int compareTo(LoadSortedSshHostConfiguration that) {
        int answer = this.containerCount - that.containerCount;
        if (answer == 0) {
            answer = this.profileCount - that.profileCount;
            if (answer == 0) {
                answer = this.index - that.index;
            }
        }
        return answer;
    }
}
