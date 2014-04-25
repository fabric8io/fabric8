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
package io.fabric8.runtime.itests.support;

import io.fabric8.api.DynamicReference;
import io.fabric8.api.jcip.ThreadSafe;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.ServiceReference;
import org.jboss.gravia.runtime.ServiceTracker;


@ThreadSafe
public final class ServiceProxy<T> {

    public static long DEFAULT_TIMEOUT = 30000L;

    private final Class<T> serviceClazz;
    private final DelegatingInvocationHandler<T> invocationHandler;

    public static <T> ServiceProxy<T> createServiceProxy(ModuleContext context, Class<T> serviceClazz) {
        return new ServiceProxy<T>(context, serviceClazz, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public static <T> ServiceProxy<T> createServiceProxy(ModuleContext context, Class<T> serviceClazz, long timeout, TimeUnit timeUnit) {
        return new ServiceProxy<T>(context, serviceClazz, timeout, timeUnit);
    }

    private ServiceProxy(ModuleContext moduleContext, Class<T> serviceClazz, long timeout, TimeUnit timeUnit) {
        this.invocationHandler = new DelegatingInvocationHandler<T>(moduleContext, serviceClazz, timeout, timeUnit);
        this.serviceClazz = serviceClazz;
    }

    @SuppressWarnings("unchecked")
    public T getService() {
        return (T) Proxy.newProxyInstance(serviceClazz.getClassLoader(), new Class[] { serviceClazz }, invocationHandler);
    }

    public void close() {
        invocationHandler.close();
    }

    private static class DelegatingInvocationHandler<T> implements InvocationHandler {

        private final DynamicReference<T> dynamicReference;
        private final ServiceTracker<T, T> tracker;

        DelegatingInvocationHandler(ModuleContext context, Class<T> type, long timeout, TimeUnit unit) {
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
