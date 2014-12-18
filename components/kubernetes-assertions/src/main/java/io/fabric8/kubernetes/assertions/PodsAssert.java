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

import io.fabric8.kubernetes.api.model.PodSchema;
import org.assertj.core.api.ListAssert;
import org.assertj.core.util.Lists;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.filter.Filters.filter;

/**
 * Adds some extra assertion operations
 */
public class PodsAssert extends ListAssert<PodSchema> {

    public PodsAssert(List<PodSchema> actual) {
        super(actual);
    }

    /**
     * Returns the filtered list of pods which have running status
     */
    public PodsAssert runningStatus() {
        return assertThat(filter(actual).having(Conditions.runningStatus()).get());
    }

    /**
     * Returns the filtered list of pods which have waiting status
     */
    public PodsAssert waitingStatus() {
        return assertThat(filter(actual).having(Conditions.waitingStatus()).get());
    }

    /**
     * Returns the filtered list of pods which have error status
     */
    public PodsAssert errorStatus() {
        return assertThat(filter(actual).having(Conditions.errorStatus()).get());
    }

    protected static PodsAssert assertThat(Iterable<PodSchema> result) {
        List<PodSchema> list =  Lists.newArrayList(result);
        return new PodsAssert(list);
    }
}
