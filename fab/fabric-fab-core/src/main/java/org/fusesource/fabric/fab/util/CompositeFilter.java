/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.util;

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
    public boolean matches(T t) {
        for (Filter<T> filter : filters) {
            if (filter.matches(t)) {
                return true;
            }
        }
        return false;
    }
}
