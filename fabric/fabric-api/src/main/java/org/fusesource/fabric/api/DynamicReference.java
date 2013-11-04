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
package org.fusesource.fabric.api;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An object that is intended to hold a reference to a dynamic object (e.g. OSGi services).
 * The DynamicReference will return the object if available or wait for it for a configurable amount of time.
 * Usually solutions like SCR tackle the dynamic nature of references, but nothing prevents the user of obtaining
 * a reference to a deactivated component with unbound references.
 */
public final class DynamicReference<T> implements Callable<T> {

    private static final long DEFAULT_TIMEOUT = 5000;
    private static final String DEFAULT_NAME = "dynamic reference";

    private static final String TIMEOUT_MESSAGE_FORMAT = "Gave up waiting for: %s";
    private static final String INTERRUPTED_MESSAGE_FORMAT = "Interrupted while waiting for: %s";
    private static final String UNBOUND_MESSAGE_FORMAT = "Unbound while waiting for: %s";

    private final AtomicInteger revisionIndex = new AtomicInteger();
    private final Map<Integer, ValueRevision> revisionMap = new HashMap<Integer, ValueRevision>();
    private final long timeout;
    private final TimeUnit unit;
    private final String name;

    public DynamicReference() {
        this(DEFAULT_NAME);
    }

    public DynamicReference(String name) {
        this(name, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public DynamicReference(String name, long timeout, TimeUnit unit) {
        this.name = name;
        this.unit = unit;
        this.timeout = timeout;
        this.revisionMap.put(revisionIndex.incrementAndGet(), new ValueRevision());
    }

    @Override
    public T call() throws Exception {
        return currentRevision().get(timeout, unit);
    }

    /**
     * Get the reference or wait until it becomes available.
     * @return Returns the reference or throws a {@link DynamicReferenceException}.
     */
    public T get() {
        return currentRevision().get(timeout, unit);
    }

    /**
     * Returns the value if present.
     * @return  The value or Null.
     */
    public T getIfPresent() {
        return currentRevision().getIfPresent();
    }

    /**
     * Binds the reference.
     */
    public void bind(T value) {
        currentRevision().bind(value);
    }

    /**
     * Unbinds the reference.
     */
    public void unbind() {
        synchronized (revisionMap) {

            // Unbind the current revision
            currentRevision().unbind();

            // Remove old unused revisions
            Iterator<ValueRevision> itrev = revisionMap.values().iterator();
            while(itrev.hasNext()) {
                ValueRevision auxrev = itrev.next();
                if (auxrev.usageCount.get() == 0) {
                    itrev.remove();
                }
            }

            // Create a new revision
            revisionMap.put(revisionIndex.incrementAndGet(), new ValueRevision());
        }
    }

    ValueRevision currentRevision() {
        synchronized (revisionMap) {
            return revisionMap.get(revisionIndex.get());
        }
    }

    class ValueRevision {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<T> ref = new AtomicReference<T>();
        final AtomicInteger usageCount = new AtomicInteger();

        void bind(T value) {
            ref.set(value);
            latch.countDown();
        }

        void unbind() {
            ref.set(null);
            latch.countDown();
        }

        T getIfPresent() {
            return ref.get();
        }

        T get(long timeout, TimeUnit unit) {
            usageCount.incrementAndGet();
            try {
                if (!latch.await(timeout, unit)) {
                    throw new DynamicReferenceException(String.format(TIMEOUT_MESSAGE_FORMAT, name));
                }
            } catch (InterruptedException ex) {
                throw new DynamicReferenceException(String.format(INTERRUPTED_MESSAGE_FORMAT, name), ex);
            } finally {
                usageCount.decrementAndGet();
            }

            T value = ref.get();
            if (value == null) {
                throw new DynamicReferenceException(String.format(UNBOUND_MESSAGE_FORMAT, name));
            }

            return value;
        }
    }
}
