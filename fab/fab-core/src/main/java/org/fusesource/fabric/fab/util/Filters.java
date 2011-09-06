/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
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
}
