/*
 * #%L
 * Fabric8 :: API
 * %%
 * Copyright (C) 2014 Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package io.fabric8.api.gravia;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.fabric8.api.gravia.IllegalStateAssertion;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Locate a service in the {@link Runtime}
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public final class ServiceLocator {

    public static final Long DEFAULT_TIMEOUT = 10000L;

    private ServiceLocator() {
        //Utility Class
    }

    public static BundleContext getSystemContext() {
        BundleContext result = null;
        ClassLoader classLoader = ServiceLocator.class.getClassLoader();
        if (classLoader instanceof BundleReference) {
            BundleReference bndref = (BundleReference) classLoader;
            result = bndref.getBundle().getBundleContext().getBundle(0).getBundleContext();
        }
        return result;
    }
    
    public static <T> T getService(Class<T> type) {
        BundleContext bundleContext = getSystemContext();
        ServiceReference<T> sref = bundleContext.getServiceReference(type);
        return sref != null ? bundleContext.getService(sref) : null;
    }

    public static <T> T getService(BundleContext bundleContext, Class<T> type) {
        ServiceReference<T> sref = bundleContext.getServiceReference(type);
        return sref != null ? bundleContext.getService(sref) : null;
    }

    public static <T> T getRequiredService(Class<T> type) {
        return getRequiredService(getSystemContext(), type);
    }

    public static <T> T getRequiredService(BundleContext bundleContext, Class<T> type) {
        T service = getService(bundleContext, type);
        IllegalStateAssertion.assertNotNull(service, "Service not available: " + type.getName());
        return service;
    }

    public static <T> T awaitService(Class<T> type) {
        BundleContext bundleContext = getSystemContext();
        return awaitService(bundleContext, type, null, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public static <T> T awaitService(BundleContext bundleContext, Class<T> type) {
        return awaitService(bundleContext, type, null, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public static <T> T awaitService(Class<T> type, String filterspec) {
        BundleContext bundleContext = getSystemContext();
        return awaitService(bundleContext, type, filterspec, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public static <T> T awaitService(Class<T> type, long timeout, TimeUnit unit) {
        BundleContext bundleContext = getSystemContext();
        return awaitService(bundleContext, type, null, timeout, unit);
    }

    public static <T> T awaitService(Class<T> type, String filterspec, long timeout, TimeUnit unit) {
        BundleContext bundleContext = getSystemContext();
        return awaitService(bundleContext, type, filterspec, timeout, unit);
    }

    public static <T> T awaitService(final BundleContext bundleContext, Class<T> type, String filterspec, long timeout, TimeUnit unit) {

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<T> serviceRef = new AtomicReference<T>();
        final Filter serviceFilter;
        try {
            serviceFilter = filterspec != null ? bundleContext.createFilter(filterspec) : null;
        } catch (InvalidSyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
        ServiceTracker<T, T> tracker = new ServiceTracker<T, T>(bundleContext, type, null) {
            @Override
            public T addingService(ServiceReference<T> sref) {
                T service = super.addingService(sref);
                if (serviceFilter == null || serviceFilter.match(sref)) {
                    serviceRef.set(bundleContext.getService(sref));
                    latch.countDown();
                }
                return service;
            }
        };
        tracker.open();
        try {
            if (!latch.await(timeout, unit)) {
                String srvspec = (type != null ? type.getName() : "") + (serviceFilter != null ? serviceFilter : "");
                throw new IllegalStateException("Cannot obtain service: " + srvspec);
            }
            return serviceRef.get();
        } catch (InterruptedException ex) {
            throw new IllegalStateException();
        } finally {
            tracker.close();
        }
    }
}
