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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An object that is intended to hold a reference to a dynamic object (e.g. OSGi services).
 * The DynamicReference will return the object if available or wait for it for a configurable amount of time.
 * Usually solutions like SCR tackle the dynamic nature of references, but nothing prevents the user of obtaining
 * a reference to a deactivated component with unbound references.
 */
public final class DynamicReference<T> implements Callable<T> {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicReference.class);

    private static final long DEFAULT_TIMEOUT = 5000;
    private static final String DEFAULT_NAME = "dynamic reference";

    private static final String TIMEOUT_MESSAGE_FORMAT = "Gave up waiting for: %s";
    private static final String INTERRUPTED_MESSAGE_FORMAT = "Interrupted while waiting for: %s";

    private final AtomicReference<ValueRevision> revision = new AtomicReference<ValueRevision>();
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
        this.revision.set(new ValueRevision());
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
        T value = currentRevision().get(timeout, unit);
        while (value == null) {
            LOG.warn("Unbound while waiting for: {}", name);
            value = currentRevision().get(timeout, unit);
        }
        return value;
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
    public void unbind(T value) {
        synchronized (revision) {
            ValueRevision currev = revision.get();
            currev.unbind(value);
            if (currev.getIfPresent() == null) {
                revision.set(new ValueRevision());
            }
        }
    }

    ValueRevision currentRevision() {
        synchronized (revision) {
            return revision.get();
        }
    }

    class ValueRevision {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<T> ref = new AtomicReference<T>();

        void bind(T value) {
            LOG.debug("bind: {}", value);
            ref.set(value);
            latch.countDown();
        }

        void unbind(T value) {
            LOG.debug("unbind: {}", value);
            if (value != null) {
                ref.compareAndSet(value, null);
            } else {
                ref.set(null);
            }
            latch.countDown();
        }

        T getIfPresent() {
            return ref.get();
        }

        /**
         * Waits for the ref to get bound/unbound
         * If the ref got unbound while waiting, return null.
         */
        T get(long timeout, TimeUnit unit) {
            try {
                if (!latch.await(timeout, unit)) {
                    throw new DynamicReferenceException(String.format(TIMEOUT_MESSAGE_FORMAT, name));
                }
            } catch (InterruptedException ex) {
                throw new DynamicReferenceException(String.format(INTERRUPTED_MESSAGE_FORMAT, name), ex);
            }
            T value = ref.get();
            return value;
        }
    }
}
