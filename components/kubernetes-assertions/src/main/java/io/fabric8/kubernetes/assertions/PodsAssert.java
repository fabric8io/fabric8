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
package io.fabric8.kubernetes.assertions;

import io.fabric8.kubernetes.api.model.Pod;
import org.assertj.core.api.Condition;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.filter.Filters;
import org.assertj.core.util.Lists;

import java.util.List;

import static io.fabric8.kubernetes.assertions.Conditions.podLabel;

/**
 * Adds some extra assertion operations
 */
public class PodsAssert extends ListAssert<Pod> {

    public PodsAssert(List<Pod> actual) {
        super(actual);
    }

    public PodsAssert filter(Condition<Pod> condition) {
        return assertThat(Filters.filter(actual).having(condition).get());
    }

    /**
     * Returns the underlying actual value
     */
    public List<Pod> get() {
        return actual;
    }

    /**
     * Filters the pods using the given label key and value
     */
    public PodsAssert filterLabel(String key, String value) {
        return filter(podLabel(key, value));
    }

    /**
     * Returns the filtered list of pods which have running status
     */
    public PodsAssert runningStatus() {
        return filter(Conditions.runningStatus());
    }

    /**
     * Returns the filtered list of pods which have waiting status
     */
    public PodsAssert waitingStatus() {
        return filter(Conditions.waitingStatus());
    }

    /**
     * Returns the filtered list of pods which have error status
     */
    public PodsAssert errorStatus() {
        return filter(Conditions.errorStatus());
    }

    protected static PodsAssert assertThat(Iterable<Pod> result) {
        List<Pod> list = Lists.newArrayList(result);
        return new PodsAssert(list);
    }
}
