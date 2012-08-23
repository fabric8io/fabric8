/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.common.util;

import java.util.Collection;

/**
 * Represents a filter which ORs together a collection of filters,
 * returning true if any of the filters are true
 */
public class CompositeFilter<T> implements Filter<T> {
    private final Collection<Filter<T>> filters;

    public CompositeFilter(Collection<Filter<T>> filters) {
        this.filters = filters;
    }

    @Override
    public String toString() {
        return "CompsiteFilter" + filters;

    }

    @Override
    public boolean matches(T t) {
        for (Filter<T> filter : filters) {
            if (filter.matches(t)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return filters.isEmpty();
    }
}
