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

import io.fabric8.api.jcip.ThreadSafe;

import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleContext;

@ThreadSafe
public final class ServiceProxy {

    private final BundleContext bundleContext;
    private DelegatingInvocationHandler<?> invocationHandler;

    public ServiceProxy(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClazz) {
        return (T) Proxy.newProxyInstance(serviceClazz.getClassLoader(), new Class[] { serviceClazz }, createInvocationHandler(serviceClazz, 0, null));
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClazz, long timeout, TimeUnit timeUnit) {
        return (T) Proxy.newProxyInstance(serviceClazz.getClassLoader(), new Class[] { serviceClazz }, createInvocationHandler(serviceClazz, timeout, timeUnit));
    }

    public synchronized void close() {
        if (invocationHandler != null) {
            invocationHandler.close();
            invocationHandler = null;
        }
    }

    private synchronized <T> DelegatingInvocationHandler<T> createInvocationHandler(Class<T> serviceClazz, long timeout, TimeUnit timeUnit) {
        if (invocationHandler != null)
            throw new IllegalStateException("InvocationHandler already constructed");

        DelegatingInvocationHandler<T> result;
        if (timeout != 0 && timeUnit != null) {
            result = new DelegatingInvocationHandler<T>(bundleContext, serviceClazz, timeout, timeUnit);
        } else {
            result = new DelegatingInvocationHandler<T>(bundleContext, serviceClazz);
        }

        invocationHandler = result;
        return result;
    }
}
