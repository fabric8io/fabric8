/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.assertions;

import io.fabric8.kubernetes.api.model.HasMetadata;
import org.assertj.core.api.Condition;
import org.assertj.core.api.IntegerAssert;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.filter.Filters;
import org.assertj.core.util.Lists;

import java.util.List;

/**
 */
public abstract class HasMetadatasAssert<R extends HasMetadata, AI extends HasMetadatasAssert> extends ListAssert<R> {
    public HasMetadatasAssert(List<R> actual) {
        super(actual);
    }

    protected abstract AI createListAssert(List<R> list);

    protected AI assertThat(Iterable<R> result) {
        List<R> list = Lists.newArrayList(result);
        return createListAssert(list);
    }

    public AI filter(Condition<R> condition) {
        return assertThat((Iterable<R>) Filters.filter(actual).having(condition).get());
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
    public List<R> get() {
        return (List<R>) actual;
    }

    /**
     * Asserts that this collection has at least one element and returns the first one
     */
    public R first() {
        assertSize().isGreaterThan(0);
        return get().get(0);
    }

    /**
     * Asserts that this collection has at least one element and returns the last one
     */
    public R last() {
        assertSize().isGreaterThan(0);
        List<R> list = get();
        return list.get(list.size() - 1);
    }

    /**
     * Asserts that this collection has a resource with the given name and return it
     *
     * @return returns the matching resource
     */
    public R hasName(String name) {
        return (R) filterName(name).first();
    }

    /**
     * Filters the resources by name
     */
    public AI filterName(String name) {
        return filter((Condition<R>) Conditions.hasName(name));
    }

    /**
     * Filters the resources using the given label key and value
     */
    public AI filterLabel(String key, String value) {
        return filter((Condition<R>) Conditions.hasLabel(key, value));
    }

    /**
     * Filters the resources using the given namespace
     */
    public AI filterNamespace(String namespace) {
        return filter((Condition<R>) Conditions.hasNamespace(namespace));
    }
}
