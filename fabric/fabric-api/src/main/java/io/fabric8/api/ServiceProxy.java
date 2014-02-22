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
package io.fabric8.api;

import io.fabric8.api.jcip.ThreadSafe;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

@ThreadSafe
public final class ServiceProxy<T> {

    public static long DEFAULT_TIMEOUT = 30000L;

    private final Class<T> serviceClazz;
    private final DelegatingInvocationHandler<T> invocationHandler;

    public static <T> ServiceProxy<T> createServiceProxy(BundleContext bundleContext, Class<T> serviceClazz) {
        return new ServiceProxy<T>(bundleContext, serviceClazz, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public static <T> ServiceProxy<T> createServiceProxy(BundleContext bundleContext, Class<T> serviceClazz, long timeout, TimeUnit timeUnit) {
        return new ServiceProxy<T>(bundleContext, serviceClazz, timeout, timeUnit);
    }

    private ServiceProxy(BundleContext bundleContext, Class<T> serviceClazz, long timeout, TimeUnit timeUnit) {
        this.invocationHandler = new DelegatingInvocationHandler<T>(bundleContext, serviceClazz, timeout, timeUnit);
        this.serviceClazz = serviceClazz;
    }

    @SuppressWarnings("unchecked")
    public T getService() {
        return (T) Proxy.newProxyInstance(serviceClazz.getClassLoader(), new Class[] { serviceClazz }, invocationHandler);
    }

    public void close() {
        invocationHandler.close();
    }

    static class DelegatingInvocationHandler<T> implements InvocationHandler {

        private final DynamicReference<T> dynamicReference;
        private final ServiceTracker<T, T> tracker;

        DelegatingInvocationHandler(BundleContext context, Class<T> type, long timeout, TimeUnit unit) {
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

        void close() {
            tracker.close();
        }
    }
}
