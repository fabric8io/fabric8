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

import io.fabric8.kubernetes.api.model.Pod;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ListAssert;
import org.assertj.core.internal.Iterables;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Provides helper methods for navigating a list property in a generated assertion class
 *
 * TODO replace with the same class from assertj-core when this issue is fixed and released:
 * https://github.com/joel-costigliola/assertj-core/issues/641
 */
public class NavigationListAssert<T, EA extends AbstractAssert> extends ListAssert<T> {
    private final AssertFactory<T, EA> assertFactory;

    public NavigationListAssert(List<? extends T> actual, AssertFactory<T, EA> assertFactory) {
        super(actual);
        this.assertFactory = assertFactory;
    }

    /**
     * Navigates to the first element in the list if the list is not empty
     *
     * @return the assertion on the first element
     */
    public EA first() {
        isNotEmpty();
        return toAssert(actual.get(0), Assertions.joinDescription(this, "first()"));
    }

    /**
     * Navigates to the last element in the list if the list is not empty
     *
     * @return the assertion on the last element
     */
    public EA last() {
        isNotEmpty();
        return toAssert(actual.get(actual.size() - 1), Assertions.joinDescription(this, "last()"));
    }

    /**
     * Navigates to the element at the given index if the index is within the range of the list
     *
     * @return the assertion on the given element
     */
    public EA item(int index) {
        isNotEmpty();
        assertThat(index).describedAs(Assertions.joinDescription(this, "index")).isGreaterThanOrEqualTo(0).isLessThan(actual.size());
        return toAssert(actual.get(index), Assertions.joinDescription(this, "index(" + index + ")"));
    }

    protected EA toAssert(T value, String description) {
        return (EA) assertFactory.createAssert(value).describedAs(description);
    }
}
