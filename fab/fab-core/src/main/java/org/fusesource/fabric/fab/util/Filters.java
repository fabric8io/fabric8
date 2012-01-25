/**
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
package org.fusesource.fabric.fab.util;

import java.util.Arrays;
import java.util.List;

/**
 */
public class Filters {

    public static <T> Filter<T> trueFilter() {
        return new Filter<T>() {
            public boolean matches(T t) {
                return true;
            }

            @Override
            public String toString() {
                return "TrueFilter";
            }
        };
    }

    public static <T> Filter<T> falseFilter() {
        return new Filter<T>() {
            public boolean matches(T t) {
                return false;
            }

            @Override
            public String toString() {
                return "FalseFilter";
            }
        };
    }

    public static <T> Filter<T> compositeFilter(List<Filter<T>> filters) {
        if (filters.size() == 0) {
            return falseFilter();
        } else if (filters.size() == 1) {
            return filters.get(0);
        } else {
            return new CompositeFilter<T>(filters);
        }
    }

    public static <T> Filter<T> or(final Filter<T>... filters) {
        return new Filter<T>() {
            public boolean matches(T t) {
                for (Filter filter : filters) {
                    if (filter != null && filter.matches(t)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public String toString() {
                return "OrFilter" + Arrays.asList(filters);
            }
        };
    }

    public static <T> Filter<T> not(final Filter<T> filter) {
        return new Filter<T>() {
            public boolean matches(T t) {
                return !filter.matches(t);
            }

            @Override
            public String toString() {
                return "Not(" + filter + ")";
            }
        };
    }

    public static <T> boolean isEmpty(Filter<T> filter) {
        boolean empty = false;
        if (filter instanceof CompositeFilter) {
            // lets treat empty filters as not matching anything
            CompositeFilter<T> compositeFilter = (CompositeFilter<T>) filter;
            empty = compositeFilter.isEmpty();
        }
        return empty;
    }
}
