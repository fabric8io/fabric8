/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.api.permit;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import io.fabric8.api.gravia.IllegalArgumentAssertion;
import io.fabric8.api.gravia.IllegalStateAssertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* The default implementation of a {@link PermitManager}
*
* @author thomas.diesler@jboss.com
* @since 05-Mar-2014
*/
public final class DefaultPermitManager implements PermitManager {

    public static long DEFAULT_TIMEOUT = 60000L;

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPermitManager.class);
    private final Map<PermitKey<?>, PermitState<?>> permits = new HashMap<PermitKey<?>, PermitState<?>>();

    @Override
    public <T> void activate(PermitKey<T> key, T instance) {
        getPermitState(key).activate(instance);
    }

    @Override
    public void deactivate(PermitKey<?> key) {
        getPermitState(key).deactivate(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    @Override
    public void deactivate(PermitKey<?> key, long timeout, TimeUnit unit) {
        getPermitState(key).deactivate(timeout, unit);
    }

    @Override
    public <T> Permit<T> aquirePermit(PermitKey<T> key, boolean exclusive) {
        return getPermitState(key).acquire(exclusive, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    @Override
    public <T> Permit<T> aquirePermit(PermitKey<T> key, boolean exclusive, long timeout, TimeUnit unit) {
        return getPermitState(key).acquire(exclusive, timeout, unit);
    }

    @SuppressWarnings("unchecked")
    private <T> PermitState<T> getPermitState(PermitKey<T> key) {
        IllegalArgumentAssertion.assertNotNull(key, "key");
        synchronized (permits) {
            PermitState<?> permitState = permits.get(key);
            if (permitState == null) {
                permitState = new PermitState<T>(key);
                permits.put(key, permitState);
            }
            return (PermitState<T>) permitState;
        }
    }

    static class PermitState<T> {

        private final Semaphore semaphore = new Semaphore(0);
        private final ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock(true);
        private final AtomicReference<T> activeInstance = new AtomicReference<T>();
        private final AtomicBoolean exclusiveLock = new AtomicBoolean();
        private final AtomicBoolean active = new AtomicBoolean();
        private final PermitKey<T> key;

        PermitState(PermitKey<T> key) {
            this.key = key;
        }

        void activate(T instance) {
            boolean nowactive = active.compareAndSet(false, true);
            IllegalStateAssertion.assertTrue(nowactive, "Cannot activate an already active state");

            LOGGER.debug("activating: {}",  key);

            activeInstance.set(instance);
            semaphore.release(1);
        }

        Permit<T> acquire(final boolean exclusive, long timeout, TimeUnit unit) {

            final String exclstr = exclusive ? " exclusive" : "";
            if (LOGGER.isTraceEnabled()) {
                String timestr = unit != null ? " in " + unit.toMillis(timeout) + "ms" : "";
                LOGGER.trace("aquiring" + exclstr + timestr + ": {}", key);
            }

            // A Permit has a single semaphore permit - get it
            // Once we pass this, no other thread can get passed here
            // Same happens in deactivate, so no two threads can activate/deactivate at the same time
            getSinglePermit(timeout, unit);

            // Get a reference to the instance that is currently
            // associated with this Permit
            final T instance = activeInstance.get();

            final Lock lock;
            if (exclusive) {
                // For an exclusive Permit we aquire a write-lock
                // This will block until all readers/writers have returned their locks
                // We do not release the semaphore permit, this will get done when the exclusive Permit is released
                lock = writeLock(timeout, unit);
                exclusiveLock.set(true);
            } else {
                // For an non-exclusive Permit we aquire a read-lock
                // This will block until a writer returns its lock
                // Multiple readers can get passed this
                // We do release the semaphore permit
                lock = readLock(timeout, unit);

                // Release the single semahore permit
                semaphore.release(1);
            }

            LOGGER.trace("aquired" + exclstr + ": {}", key);

            return new Permit<T>() {

                @Override
                public PermitKey<T> getPermitKey() {
                    return key;
                }

                @Override
                public T getInstance() {
                    return instance;
                }

                @Override
                public void release() {
                    LOGGER.trace("releasing" + exclstr + ": {}", key);

                    // Always unlock the read/write lock
                    lock.unlock();

                    // Release the semaphore permit for a writer
                    if (exclusive) {
                        exclusiveLock.set(false);
                        semaphore.release(1);
                    }
                }
            };
        }

        void deactivate(long timeout, TimeUnit unit) {

            LOGGER.trace("deactivating: {}",  key);

            // Deactivate on an already inactive Permit has no effect
            if (!active.get()) {
                LOGGER.trace("not active: {}",  key);
                return;
            }

            // Deactivating while holding an exclusive lock
            if (exclusiveLock.get()) {
                LOGGER.debug("deactivated (exclusive): {}",  key);
                active.set(false);
                return;
            }

            // A Permit has a single semaphore permit - get it
            // Once we pass this, no other thread can get passed here
            // Same happens in aquire, so no two threads can activate/deactivate at the same time
            getSinglePermit(timeout, unit);

            // Deactivation requires a write-lock
            // We do not release the semaphore permit - this happens in activate
            try {
                writeLock(timeout, unit).unlock();
                LOGGER.debug("deactivated: {}",  key);
                active.set(false);
            } catch (PermitStateTimeoutException ex) {
                semaphore.release(1);
                throw new PermitStateTimeoutException("Cannot deactivate state [" + key.getName() + "] in time", key, timeout, unit);
            }
        }

        private void getSinglePermit(long timeout, TimeUnit unit) {
            try {
                if (!semaphore.tryAcquire(timeout, unit)) {
                    throw new PermitStateTimeoutException("Cannot aquire permit for [" + key.getName() + "] in time", key, timeout, unit);
                }
            } catch (InterruptedException ex) {
                throw new IllegalStateException(ex);
            }
        }

        private ReadLock readLock(long timeout, TimeUnit unit) {
            ReadLock lock = rwlock.readLock();
            try {
                if (!lock.tryLock() && !lock.tryLock(timeout, unit))
                    throw new PermitStateTimeoutException("Cannot aquire read lock for [" + key.getName() + "] in time", key, timeout, unit);
            } catch (InterruptedException ex) {
                throw new IllegalStateException(ex);
            }
            return lock;
        }

        private WriteLock writeLock(long timeout, TimeUnit unit) {
            WriteLock lock = rwlock.writeLock();
            try {
                if (!lock.tryLock() && !lock.tryLock(timeout, unit))
                    throw new PermitStateTimeoutException("Cannot aquire write lock for [" + key.getName() + "] in time", key, timeout, unit);
            } catch (InterruptedException ex) {
                throw new IllegalStateException(ex);
            }
            return lock;
        }
    }
}
