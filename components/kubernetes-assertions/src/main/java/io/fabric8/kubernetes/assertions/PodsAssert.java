/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.kubernetes.assertions;

import io.fabric8.kubernetes.api.model.Pod;
import org.assertj.core.api.Condition;
import org.assertj.core.api.IntegerAssert;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectEnumerableAssert;
import org.assertj.core.api.filter.Filters;
import org.assertj.core.util.Lists;

import java.util.List;

import static io.fabric8.kubernetes.assertions.Conditions.podLabel;
import static io.fabric8.kubernetes.assertions.Conditions.podNamespace;

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
     * Returns an assertion on the size of the list
     */
    public IntegerAssert assertSize() {
        return (IntegerAssert) org.assertj.core.api.Assertions.assertThat(get().size()).as("size");
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
     * Filters the pods using the given namespace
     */
    public PodsAssert filterNamespace(String namespace) {
        return filter(podNamespace(namespace));
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
