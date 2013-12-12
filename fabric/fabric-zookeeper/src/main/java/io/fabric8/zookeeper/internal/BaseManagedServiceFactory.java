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
package io.fabric8.zookeeper.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseManagedServiceFactory<T> implements ManagedServiceFactory {

    public static final long DEFAULT_TIMEOUT_BEFORE_INTERRUPT = 30000;

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final BundleContext context;
    private final String name;
    private final long timeoutBeforeInterrupt;
    private final AtomicBoolean destroyed;
    private final ExecutorService executor;
    private final Map<String, Pair<T, ServiceRegistration>> services;

    public BaseManagedServiceFactory(BundleContext context, String name) {
        this(context, name, DEFAULT_TIMEOUT_BEFORE_INTERRUPT);
    }

    public BaseManagedServiceFactory(BundleContext context, String name, long timeoutBeforeInterrupt) {
        this.context = context;
        this.name = name;
        this.timeoutBeforeInterrupt = timeoutBeforeInterrupt;
        this.destroyed = new AtomicBoolean(false);
        this.executor = Executors.newSingleThreadExecutor();
        this.services = new ConcurrentHashMap<String, Pair<T, ServiceRegistration>>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void updated(final String pid, final Dictionary properties) throws ConfigurationException {
        LOGGER.info("Configuration {} updated: {}", pid, properties);
        if (destroyed.get()) {
            return;
        }
        checkConfiguration(pid, properties);
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    internalUpdate(pid, properties);
                } catch (Throwable t) {
                    LOGGER.warn("Error destroying service for ManagedServiceFactory " + getName(), t);
                }
            }
        });
    }

    @Override
    public void deleted(final String pid) {
        LOGGER.info("Configuration {} delete", pid);
        if (destroyed.get()) {
            return;
        }
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    internalDelete(pid);
                } catch (Throwable throwable) {
                    LOGGER.warn("Error destroying service for ManagedServiceFactory " + getName(), throwable);
                }
            }
        });
    }

    protected void checkConfiguration(String pid, Dictionary properties) throws ConfigurationException {
        // Do nothing
    }

    protected abstract T doCreate(Dictionary properties) throws Exception;

    protected T doUpdate(T t, Dictionary properties) throws Exception {
        doDestroy(t);
        return doCreate(properties);
    }

    protected abstract void doDestroy(T t) throws Exception;

    protected abstract String[] getExposedClasses(T t);

    private void internalUpdate(String pid, Dictionary properties) {
        Pair<T, ServiceRegistration> pair = services.get(pid);
        if (pair != null) {
            try {
                T t = doUpdate(pair.getFirst(), properties);
                pair.setFirst(t);
                pair.getSecond().setProperties(properties);
            } catch (Throwable throwable) {
                internalDelete(pid);
                LOGGER.warn("Error updating service for ManagedServiceFactory " + getName(), throwable);
            }
        } else {
            if (destroyed.get()) {
                return;
            }
            try {
                T t = doCreate(properties);
                try {
                    if (destroyed.get()) {
                        throw new IllegalStateException("ManagedServiceFactory has been destroyed");
                    }
                    ServiceRegistration registration = context.registerService(getExposedClasses(t), t, properties);
                    services.put(pid, new Pair<T, ServiceRegistration>(t, registration));
                } catch (Throwable throwable1) {
                    try {
                        doDestroy(t);
                    } catch (Throwable throwable2) {
                        // Ignore
                    }
                    throw throwable1;
                }
            } catch (Throwable throwable) {
                if (!destroyed.get()) {
                    LOGGER.warn("Error creating service for ManagedServiceFactory " + getName(), throwable);
                }
            }
        }
    }

    private void internalDelete(String pid) {
        Pair<T, ServiceRegistration> pair = services.remove(pid);
        if (pair != null) {
            try {
                pair.getSecond().unregister();
            } catch (Throwable t) {
                LOGGER.info("Error unregistering service", t);
            }
            try {
                doDestroy(pair.getFirst());
            } catch (Throwable t) {
                LOGGER.info("Error destroying service", t);
            }
        }
    }

    public void destroy() {
        if (destroyed.compareAndSet(false, true)) {
            executor.shutdown();
            try {
                executor.awaitTermination(timeoutBeforeInterrupt, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException("Shutdown interrupted");
            }
            if (!executor.isTerminated()) {
                executor.shutdownNow();
                try {
                    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Shutdown interrupted");
                }
            }

            while (!services.isEmpty()) {
                String pid = services.keySet().iterator().next();
                internalDelete(pid);
            }
        }
    }

    static class Pair<U,V> {
        private U first;
        private V second;
        public Pair(U first, V second) {
            this.first = first;
            this.second = second;
        }
        public U getFirst() {
            return first;
        }
        public V getSecond() {
            return second;
        }
        public void setFirst(U first) {
            this.first = first;
        }
        public void setSecond(V second) {
            this.second = second;
        }
    }

}
