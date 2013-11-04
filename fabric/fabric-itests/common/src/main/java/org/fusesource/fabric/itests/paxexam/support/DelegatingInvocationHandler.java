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
package org.fusesource.fabric.itests.paxexam.support;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DelegatingInvocationHandler<T> implements InvocationHandler {

    private final ServiceTracker<T, T> tracker;

    public DelegatingInvocationHandler(BundleContext bundleContext, Class<T> type) {
        this.tracker = new ServiceTracker<T, T>(bundleContext, type, null) {
            @Override
            public T addingService(ServiceReference<T> reference) {
                T service =  super.addingService(reference);
                return service;
            }

            @Override
            public void modifiedService(ServiceReference<T> reference, T service) {
                super.modifiedService(reference, service);
            }

            @Override
            public void removedService(ServiceReference<T> reference, T service) {
                super.removedService(reference, service);
            }
        };
        tracker.open();
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            T service = tracker.waitForService(10000);
            return method.invoke(service, args);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
