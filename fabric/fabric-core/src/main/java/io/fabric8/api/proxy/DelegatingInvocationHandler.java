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
package io.fabric8.api.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import io.fabric8.api.DynamicReference;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public final class DelegatingInvocationHandler<T> implements InvocationHandler {

    public static long DEFAULT_TIMEOUT = 30000L;

    private final DynamicReference<T> dynamicReference;
    private final ServiceTracker<T, T> tracker;

    public DelegatingInvocationHandler(BundleContext context, Class<T> type) {
        this(context, type, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public DelegatingInvocationHandler(BundleContext context, Class<T> type, long timeout, TimeUnit unit) {
        dynamicReference = new DynamicReference<T>(type.getSimpleName(), timeout, unit);
        tracker = new ServiceTracker<T, T>(context, type, null) {

            @Override
            public T addingService(ServiceReference<T> reference) {
                T service =  super.addingService(reference);
                dynamicReference.bind(service);
                return service;
            }

            @Override
            public void modifiedService(ServiceReference<T> reference, T service) {
                super.modifiedService(reference, service);
                dynamicReference.bind(service);
            }

            @Override
            public void removedService(ServiceReference<T> reference, T service) {
                super.removedService(reference, service);
                dynamicReference.unbind(service);
            }
        };
        tracker.open();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            T service = dynamicReference.get();
            return method.invoke(service, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
