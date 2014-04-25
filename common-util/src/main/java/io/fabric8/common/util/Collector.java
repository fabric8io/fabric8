package io.fabric8.common.util;

import java.util.Collection;

/**
 * Represents an object capable of collecting stuff (dependencies, bundles, features, ...)
 */
public interface Collector<T> {

    /**
     * Access the collection of items gathered by this collector
     *
     * @return the collection of items
     */
    public Collection<T> getCollection();

}
