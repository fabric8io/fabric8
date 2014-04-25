package io.fabric8.common.util;

/**
 * Represents a filter or predicate
 */
public interface Filter<T> {
    public boolean matches(T t);
}
