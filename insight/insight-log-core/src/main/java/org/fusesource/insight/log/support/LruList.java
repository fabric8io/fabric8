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
package org.fusesource.insight.log.support;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * A simple LRU list that stores a fixed size
 */
public class LruList<T> {
    private final Class<T> klass;
    private final int size;
    private T[] elements;
    private transient int start = 0;
    private transient int end = 0;
    private transient boolean full = false;
    private final int maxElements;

    public LruList(Class<T> klass, int size) {
        this.klass = klass;
        this.size = size;
        if (size <= 0) {
            throw new IllegalArgumentException("The size must be greater than 0");
        }
        elements = createArray(size);
        maxElements = elements.length;
    }

    public synchronized int size() {
        int size = 0;
        if (end < start) {
            size = maxElements - start + end;
        } else if (end == start) {
            size = (full ? maxElements : 0);
        } else {
            size = end - start;
        }
        return size;
    }

    public synchronized void clear() {
        start = 0;
        end = 0;
        elements = createArray(size);
    }

    public synchronized void add(T element) {
        if (null == element) {
             throw new NullPointerException("Attempted to add null object to buffer");
        }
        if (size() == maxElements) {
            Object e = elements[start];
            if (null != e) {
                elements[start++] = null;
                if (start >= maxElements) {
                    start = 0;
                }
                full = false;
            }
        }
        elements[end++] = element;
        if (end >= maxElements) {
            end = 0;
        }
        if (end == start) {
            full = true;
        }
    }

    public synchronized Iterable<T> getElements() {
        return getElements(size());
    }

    public synchronized Iterable<T> getElements(int nb) {
        int s = size();
        nb = Math.min(Math.max(0, nb), s);
        T[] e = createArray(nb);
        for (int i = 0; i < nb; i++) {
            e[i] = elements[(i + s - nb + start) % maxElements];
        }
        return Arrays.asList(e);
    }

    private T[] createArray(int size) {
        return (T[]) Array.newInstance(klass, size);
    }

}
