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

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import java.lang.reflect.Proxy;

public class ServiceProxy {

    public static <T>  T getOsgiServiceProxy(Class<T> clazz) {
        return getOsgiServiceProxy(getBundleContext(), clazz);
    }

    public static <T>  T getOsgiServiceProxy(BundleContext bundleContext, Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
                new Class[]{clazz},
                new DelegatingInvocationHandler<T>(bundleContext, clazz));
    }

    /**
     * Returns the bundle context.
     *
     * @return
     */
    private static BundleContext getBundleContext() {
        return FrameworkUtil.getBundle(ServiceProxy.class).getBundleContext();
    }
}
