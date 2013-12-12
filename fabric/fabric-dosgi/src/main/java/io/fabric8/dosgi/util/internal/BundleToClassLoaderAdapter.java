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


package io.fabric8.dosgi.util.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

/**
 * @deprecated Please use AriesFrameworkUtil.getClassLoader to get a class loader for a bundle instead of this method
 */
@Deprecated
public class BundleToClassLoaderAdapter extends ClassLoader implements BundleReference {
    private Bundle b;

    public BundleToClassLoaderAdapter(Bundle bundle) {
        b = bundle;
    }

    @Override
    public URL getResource(final String name) {
        return AccessController.doPrivileged(new PrivilegedAction<URL>() {
            public URL run() {
                return b.getResource(name);
            }
        });
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        URL url = getResource(name);

        InputStream result = null;

        if (url != null) {
            try {
                result = url.openStream();
            } catch (IOException e) {
            }
        }

        return result;
    }

    @Override
    public Enumeration<URL> getResources(final String name) throws IOException {
        Enumeration<URL> urls;
        try {
            urls = AccessController.doPrivileged(new PrivilegedExceptionAction<Enumeration<URL>>() {
                @SuppressWarnings("unchecked")
                public Enumeration<URL> run() throws IOException {
                    return b.getResources(name);
                }
            });
        } catch (PrivilegedActionException e) {
            Exception cause = e.getException();

            if (cause instanceof IOException) throw (IOException) cause;
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            throw new IOException(name, cause);
        }

        if (urls == null) {
            urls = Collections.enumeration(new ArrayList<URL>());
        }

        return urls;
    }

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                public Class<?> run() throws ClassNotFoundException {
                    return b.loadClass(name);
                }
            });
        } catch (PrivilegedActionException e) {
            Exception cause = e.getException();

            if (cause instanceof ClassNotFoundException) throw (ClassNotFoundException) cause;
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;

            throw new ClassNotFoundException(name, cause);
        }
    }

    public Bundle getBundle() {
        return b;
    }
}