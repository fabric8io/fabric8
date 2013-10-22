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
package org.fusesource.common.util;

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
