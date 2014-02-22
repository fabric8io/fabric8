/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.fabric8.api;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public final class ServiceLocator {

	public static final Long DEFAULT_TIMEOUT = 60000L;

	private ServiceLocator() {
		//Utility Class
	}

    public static <T> T awaitService(BundleContext bundleContext, Class<T> type) {
        return awaitService(bundleContext, type, null, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

	public static <T> T awaitService(BundleContext bundleContext, Class<T> type, long timeout, TimeUnit unit) {
		return awaitService(bundleContext, type, null, timeout, unit);
	}

    public static <T> T awaitService(BundleContext bundleContext, Class<T> type, String filter) {
        return awaitService(bundleContext, type, filter, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public static <T> T awaitService(final BundleContext bundleContext, Class<T> type, String filterspec, long timeout, TimeUnit unit) {

        final Filter srvfilter;
        try {
            srvfilter = filterspec != null ? FrameworkUtil.createFilter(filterspec) : null;
        } catch (InvalidSyntaxException ex) {
            throw new IllegalArgumentException("Invalid filter", ex);
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<T> serviceRef = new AtomicReference<T>();
        ServiceTracker<T, T> tracker = new ServiceTracker<T, T>(bundleContext, type, null) {
            @Override
            public T addingService(ServiceReference<T> sref) {
                T service = super.addingService(sref);
                if (srvfilter == null || srvfilter.match(sref)) {
                    serviceRef.set(bundleContext.getService(sref));
                    latch.countDown();
                }
                return service;
            }
        };
        tracker.open();
        try {
            if (!latch.await(timeout, unit)) {
                throw new RuntimeException("Cannot obtain service: " + srvfilter);
            }
            return serviceRef.get();
        } catch (InterruptedException ex) {
            throw new IllegalStateException();
        } finally {
            tracker.close();
        }
	}
}
