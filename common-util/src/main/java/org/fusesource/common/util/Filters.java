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

    /**
     * Returns true if any of the filters matches the given value
     */
    public static <T> boolean matches(T value, List<? extends Filter<T>> filters) {
        if (filters != null) {
            for (Filter<T> filter : filters) {
                if (filter.matches(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Return true if the value matches an include filter if specified and does not match an exclude filter
     */
    public static <T> boolean matches(T value, List<? extends Filter<T>> includeFilters, List<? extends Filter<T>> excludeFilters) {
        if (matches(value, excludeFilters)) {
            return false;
        }
        return includeFilters == null || includeFilters.isEmpty() || Filters.matches(value, includeFilters);
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

    /**
     * Returns a String pattern matching filter using ! for not and * for any characters
     */
    public static Filter<String> createStringFilter(final String text) {
        if (text.startsWith("!")) {
            String remaining = text.substring(1);
            return not(createStringFilter(remaining));
        } else {
            if (text == null || text.length() == 0 || text.startsWith("*")) {
                return trueFilter();
            } else {
                if (text.endsWith("*")) {
                    final String prefix = text.substring(0, text.length() - 1);
                    return new Filter<String>() {
                        public boolean matches(String s) {
                            return s.startsWith(prefix);
                        }

                        @Override
                        public String toString() {
                            return "StartsWith(" + prefix + ")";
                        }
                    };

                } else {
                    return new Filter<String>() {
                        public boolean matches(String s) {
                            return text.equals(s);
                        }

                        @Override
                        public String toString() {
                            return "Equals(" + text + ")";
                        }
                    };
                }
            }
        }
    }
}
