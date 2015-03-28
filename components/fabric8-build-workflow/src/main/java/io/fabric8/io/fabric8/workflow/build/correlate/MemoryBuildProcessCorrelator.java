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
package io.fabric8.io.fabric8.workflow.build.correlate;

import io.fabric8.io.fabric8.workflow.build.BuildCorrelationKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple in memory implementation for testing.
 */
public class MemoryBuildProcessCorrelator implements BuildProcessCorrelator {
    private Map<BuildCorrelationKey, Long> map = new ConcurrentHashMap<>();

    @Override
    public void putBuildProcessInstanceId(BuildCorrelationKey buildKey, long processInstanceId) {
        map.put(buildKey, processInstanceId);
    }

    @Override
    public List<Long> findProcessInstancesForBuild(BuildCorrelationKey buildKey) {
        List<Long> answer = new ArrayList<>();
        Long id = map.get(buildKey);
        if (id != null) {
            answer.add(id);
        }
        return answer;
    }
}
