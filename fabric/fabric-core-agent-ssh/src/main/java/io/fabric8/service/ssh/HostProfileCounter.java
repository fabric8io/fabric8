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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a class to help keep track of how many containers there are per host key
 * and how many profiles are on each host
 */
public class HostProfileCounter {
    CountingMap hostContainerCounts = new CountingMap();
    Map<String, CountingMap> hostToProfileCounts = new HashMap<>();

    public CountingMap getHostContainerCounts() {
        return hostContainerCounts;
    }

    public int containerCount(String hostAlias) {
        return hostContainerCounts.count(hostAlias);
    }

    public Map<String, CountingMap> getHostToProfileCounts() {
        return hostToProfileCounts;
    }

    public int incrementContainers(String host) {
        return hostContainerCounts.increment(host);
    }

    public int decrementContainers(String host) {
        return hostContainerCounts.decrement(host);
    }

    public void setContainerCount(String host, int count) {
        hostContainerCounts.setCount(host, count);
    }

    public int incrementProfileCount(String host, String profileId) {
        CountingMap countingMap = profileCounts(host);
        return countingMap.increment(profileId);
    }

    public CountingMap profileCounts(String host) {
        CountingMap countingMap = hostToProfileCounts.get(host);
        if (countingMap == null) {
            countingMap = new CountingMap();
            hostToProfileCounts.put(host, countingMap);
        }
        return countingMap;
    }

    public void incrementProfilesCount(String hostAlias, List<String> profileIds) {
        CountingMap counts = profileCounts(hostAlias);
        counts.incrementAll(profileIds);
    }

    public void decrementProfilesCount(String hostAlias, List<String> profileIds) {
        CountingMap counts = profileCounts(hostAlias);
        counts.decrementAll(profileIds);
    }

    public int profileCount(String hostAlias, String profileId) {
        CountingMap counts = profileCounts(hostAlias);
        return counts.count(profileId);
    }
}
