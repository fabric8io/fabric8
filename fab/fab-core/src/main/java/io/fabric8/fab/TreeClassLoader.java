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

package io.fabric8.fab;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

/**
 * A {@link ClassLoader} which takes a list of child class loaders
 */
public class TreeClassLoader extends SecureClassLoader {

    private final List<DependencyClassLoader> childClassLoaders;

    public TreeClassLoader(List<DependencyClassLoader> childClassLoaders, ClassLoader parent) {
        super(parent);
        this.childClassLoaders = childClassLoaders;
    }

    @Override
    public String toString() {
        return "TreeClassLoader[" + childClassLoaders + "]";
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c = findLoadedClass(name);
        if (c == null) {
            // lets try all our child dependencies next
            for (DependencyClassLoader childClassLoader : childClassLoaders) {
                try {
                    c = childClassLoader.loadClass(name, false);
                    if (c != null) {
                        break;
                    }
                } catch (ClassNotFoundException e) {
                    // ignore
                }
            }
        }
        if (c == null) {
            try {
                ClassLoader cl = ClassLoader.getSystemClassLoader();
                c = cl.loadClass(name);
            } catch (Throwable t) {
                // ignore
            }
        }
        if (c == null) {
            c = findClass(name);
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String actual = name.replace('.', '/') + ".class";
        byte[] bytes = null;
        if (bytes != null) {
            synchronized (this) {
                Class clazz = findLoadedClass(name);
                if (clazz == null) {
                    clazz = defineClass(name, bytes, 0, bytes.length);
                }
                return clazz;
            }
        }
        throw new ClassNotFoundException(name);
    }

    private byte[] getContentEntry(Content jar, String actual) {
        InputStream is = null;
        try {
            URL url = jar.getURL(actual);
            if (url != null) {
                is = url.openStream();
            }
            if (is != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
                byte[] buf = new byte[4096];
                int n = 0;
                while ((n = is.read(buf, 0, buf.length)) >= 0) {
                    baos.write(buf, 0, n);
                }
                return baos.toByteArray();
            }
        } catch (IOException e) {
            // Ignore
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        return null;
    }

    @Override
    public URL getResource(String name) {
        for (DependencyClassLoader childClassLoader : childClassLoaders) {
            URL url = childClassLoader.getResource(name);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Vector<URL> list = new Vector<URL>();
        for (DependencyClassLoader childClassLoader : childClassLoaders) {
            Enumeration<URL> e = childClassLoader.getResources(name);
            while (e.hasMoreElements()) {
                list.add(e.nextElement());
            }
        }
        return list.elements();
    }


}

