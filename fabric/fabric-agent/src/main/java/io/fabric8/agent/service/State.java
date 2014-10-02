/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.agent.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import io.fabric8.agent.internal.MapUtils;

public class State {

    public final AtomicBoolean bootDone = new AtomicBoolean();
    public final Set<String> repositories = new TreeSet<>();
    public final Map<String, Set<String>> requirements = new HashMap<>();
    public final Map<String, Set<String>> installedFeatures = new HashMap<>();
    public final Map<String, Map<String, String>> stateFeatures = new HashMap<>();
    public final Map<String, Set<Long>> managedBundles = new HashMap<>();
    public final Map<Long, Long> bundleChecksums = new HashMap<>();

    public State copy() {
        State state = new State();
        copy(this, state, false);
        return state;
    }

    public void replace(State state) {
        copy(state, this, true);
    }

    private static void copy(State from, State to, boolean clear) {
        if (clear) {
            to.repositories.clear();
            to.requirements.clear();
            to.installedFeatures.clear();
            to.stateFeatures.clear();
            to.managedBundles.clear();
            to.bundleChecksums.clear();
        }
        to.bootDone.set(from.bootDone.get());
        MapUtils.copy(from.repositories, to.repositories);
        MapUtils.copy(from.requirements, to.requirements);
        MapUtils.copy(from.installedFeatures, to.installedFeatures);
        MapUtils.copy(from.stateFeatures, to.stateFeatures);
        MapUtils.copy(from.managedBundles, to.managedBundles);
        MapUtils.copy(from.bundleChecksums, to.bundleChecksums);
    }

}
