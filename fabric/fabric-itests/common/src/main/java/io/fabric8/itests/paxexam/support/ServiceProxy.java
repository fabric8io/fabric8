package io.fabric8.itests.paxexam.support;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import java.lang.reflect.Proxy;

public class ServiceProxy {

    public static <T>  T getOsgiServiceProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
                new Class[]{clazz},
                new DelegatingInvocationHandler<T>(getBundleContext(), clazz));
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
