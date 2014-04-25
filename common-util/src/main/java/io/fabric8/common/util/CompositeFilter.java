package io.fabric8.common.util;

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
