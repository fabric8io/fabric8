package io.fabric8.common.util;

import java.util.concurrent.Callable;

/**
 */
public class ClassLoaders {

    /**
     * Invokes the given {@link Callable} while the thread context class loader is set to the given class loader
     *
     * @return the result of the {@link java.util.concurrent.Callable#call()} method
     */
    public static <T> T withContextClassLoader(ClassLoader classLoader, Callable<T> callable) throws Exception {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            return callable.call();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }
}
