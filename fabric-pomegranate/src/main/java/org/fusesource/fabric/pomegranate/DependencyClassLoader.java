/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.pomegranate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.Enumeration;
import java.util.List;

/**
 * A {@link ClassLoader} for a single {@link DependencyTree} instance which can
 * take a list of child dependency class loaders.
 */
public class DependencyClassLoader extends SecureClassLoader {

    private final DependencyTree tree;
    private final List<DependencyClassLoader> childClassLoaders;

    public DependencyClassLoader(DependencyTree tree, List<DependencyClassLoader> childClassLoaders) {
        super(null);
        this.tree = tree;
        this.childClassLoaders = childClassLoaders;
        String url = tree.getUrl();
        if (url == null) {
            throw new IllegalArgumentException("No Url supplied for " + tree);
        }
        if (url.startsWith("file:")) {
            url = url.substring("file:".length());
        }

        // if we are a pom then don't add a jar...
        if (!url.endsWith(".pom")) {
            // TODO add a dependency....

            /*
            try {
                contents.add(new JarContent(new JarFile(url, false)));
            } catch (IOException e) {
                throw new BundleException("Error opening jar file: " + url, e);
            }
            */
        }
    }

    @Override
    public String toString() {
        return "ClassLoader[" + tree.getDependencyId() + ":" + tree.getVersion() + "]";
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c = findLoadedClass(name);

        /*
        if (c == null && bootWire != null) {
            c = bootWire.loadClass(name);
        }
        if (c == null && wires != null) {
            for (Wire wire : wires) {
                c = wire.loadClass(name);
                if (c != null) {
                    return c;
                }
            }
        }
        */
        if (c == null) {
            // lets try all our child dependencies next
            for (DependencyClassLoader childClassLoader : childClassLoaders) {
                try {
                    c = childClassLoader.loadClass(name, false);
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
        /*
        for (Content content : contents) {
            bytes = getContentEntry(content, actual);
            if (bytes != null) {
                break;
            }
        }
        */
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
            /*
        try {
            URL r;
            if (bootWire != null) {
                r = bootWire.getResource(name);
                if (r != null) {
                    return r;
                }
            }
            if (wires != null) {
                for (Wire wire : wires) {
                    r = wire.getResource(name);
                    if (r != null) {
                        return r;
                    }
                }
            }
            for (Content content : contents) {
                try {
                    URL e = content.getURL(name);
                    if (e != null) {
                        return e;
                    }
                } catch (MalformedURLException e) {
                    // Ignore
                }

            }
        } catch (ResourceNotFoundException e) {
            // Ignore
        }
    */
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        /*
        try {
            List<Enumeration<URL>> enums = new ArrayList<Enumeration<URL>>();
            if (bootWire != null) {
                enums.add(bootWire.getResources(name));
            }
            if (wires != null) {
                for (Wire w : wires) {
                    enums.add(w.getResources(name));
                }
            }
            enums.add(new ContentResourceEnumeration(name, contents.iterator()));
            return new CompoundEnumeration<URL>(enums.toArray(new Enumeration[enums.size()]));
        } catch (ResourceNotFoundException e) {
            return null;
        }
        */
        return null;
    }


}

