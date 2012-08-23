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

import java.util.*;

/**
 * A container for multiple {@link Collector} instances that just aggregates the collected results from all collectors
 * it contains
 */
public class Collectors<T> implements Collector<T> {

    private final List<Collector<T>> collectors = new LinkedList<Collector<T>>();

    public Collectors() {
        super();
    }

    public Collectors(Collector<T>... collectors) {
        super();
        this.collectors.addAll(Arrays.asList(collectors));
    }

    public void addCollector(Collector<T> collector) {
        collectors.add(collector);
    }

    @Override
    public Collection<T> getCollection() {
        Set<T> result = new HashSet<T>();
        for (Collector<T> collector : collectors) {
            result.addAll(collector.getCollection());
        }
        return result;
    }

    /**
     * Convenience method for adding an existing collection to the collector directly
     *
     * @param items the collection to be added
     */
    public void addCollection(final Collection<T> items) {
        collectors.add(new Collector<T>() {
            @Override
            public Collection<T> getCollection() {
                return items;
            }
        });
    }
}
