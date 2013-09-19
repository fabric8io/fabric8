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

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An object that is intended to hold a reference to a dynamic object (e.g. OSGi services).
 * The DynamicReference will return the object if available or wait for it for a configurable amount of time.
 * Usually solutions like SCR tackle the dynamic nature of references, but nothing prevents the user of obtaining
 * a reference to a deactivated component with unbound references.
 * @param <T>
 */
public class DynamicReference<T> implements Callable<T> {

    private static final long DEFAULT_TIMEOUT = 5000;
    private static final TimeUnit DEFAULT_TIMEUNIT = TimeUnit.MILLISECONDS;

    private final AtomicReference<T> ref = new AtomicReference<T>();
    private final Semaphore semaphore = new Semaphore(0);

    private final long timeout;
    private final TimeUnit timeUnit;



    public DynamicReference() {
        this(DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);
    }

    /**
     * Constructor
     * @param timeout
     * @param timeUnit
     */
    public DynamicReference(long timeout, TimeUnit timeUnit) {
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }


    /**
     * Get the reference or wait until it becomes available.
     * @return Returns the reference or throws a {@link DynamicReferenceException}.
     */
    public T get() {
        T value = ref.get();
        long startAt = System.currentTimeMillis();
        long remaining = timeout;
        while (value == null && remaining > 0) {
            try {
                semaphore.tryAcquire(remaining, timeUnit);
                value = ref.get();
            } catch (InterruptedException ex) {
                throw new DynamicReferenceException("Interrupted while waiting for dynamic reference.", ex);
            }
            remaining = timeout + startAt - System.currentTimeMillis();
        }

        if (value == null) {
            throw new DynamicReferenceException("Gave up waiting for dynamic reference.");
        }
        return value;
    }

    /**
     * Returns the value if present.
     * @return  The value or Null.
     */
    public T getIfPresent() {
        return ref.get();
    }


    /**
     * Binds the reference.
     * @param value
     */
    public void bind(T value) {
        if (value != null) {
            ref.set(value);
            semaphore.release();
        } else {
            unbind();
        }
    }

    /**
     * Unbinds the reference.
     */
    public void unbind() {
        this.semaphore.drainPermits();
        this.ref.set(null);
    }

    @Override
    public T call() throws Exception {
        return get();
    }
}
