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
package io.fabric8.runtime.itests.support;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.gravia.runtime.Filter;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.ServiceReference;
import org.jboss.gravia.runtime.ServiceTracker;

public final class ServiceLocator {

	public static final Long DEFAULT_TIMEOUT = 60000L;

	private ServiceLocator() {
		//Utility Class
	}

    public static <T> T awaitService(ModuleContext moduleContext, Class<T> type) {
        return awaitService(moduleContext, type, null, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

	public static <T> T awaitService(ModuleContext moduleContext, Class<T> type, long timeout, TimeUnit unit) {
		return awaitService(moduleContext, type, null, timeout, unit);
	}

    public static <T> T awaitService(ModuleContext moduleContext, Class<T> type, String filter) {
        return awaitService(moduleContext, type, filter, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public static <T> T awaitService(final ModuleContext moduleContext, Class<T> type, String filterspec, long timeout, TimeUnit unit) {

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<T> serviceRef = new AtomicReference<T>();
        final Filter srvfilter = filterspec != null ? moduleContext.createFilter(filterspec) : null;
        ServiceTracker<T, T> tracker = new ServiceTracker<T, T>(moduleContext, type, null) {
            @Override
            public T addingService(ServiceReference<T> sref) {
                T service = super.addingService(sref);
                if (srvfilter == null || srvfilter.match(sref)) {
                    serviceRef.set(moduleContext.getService(sref));
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
