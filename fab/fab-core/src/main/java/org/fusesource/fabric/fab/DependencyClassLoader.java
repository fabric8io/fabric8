/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link ClassLoader} for a single {@link DependencyTree} instance which can
 * take a list of child dependency class loaders.
 */
public class DependencyClassLoader extends URLClassLoader {
    private final DependencyTree tree;

    public static DependencyClassLoader newInstance(DependencyTree tree, List<DependencyTree> nonSharedDependencies, List<DependencyClassLoader> childClassLoaders, ClassLoader parent) throws MalformedURLException {
        ClassLoader parentClassLoader;
        if (childClassLoaders == null || childClassLoaders.isEmpty()) {
            parentClassLoader = parent;
        } else {
            parentClassLoader = new TreeClassLoader(childClassLoaders, parent);
        }
        List<DependencyTree> dependencies = new ArrayList<DependencyTree>();
        if (tree.isValidLibrary()) {
            dependencies.add(tree);
        }
        dependencies.addAll(nonSharedDependencies);
        List<URL> urlList = new ArrayList<URL>();
        for (DependencyTree dependency : dependencies) {
            if( dependency.isValidLibrary() ) {
                URL u = dependency.getJarURL();
                urlList.add(u);
            }
        }
        URL[] urls = urlList.toArray(new URL[urlList.size()]);
        return new DependencyClassLoader(tree, urls, parentClassLoader);
    }


    public DependencyClassLoader(DependencyTree tree, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.tree = tree;
    }

    @Override
    public String toString() {
        return "ClassLoader[" + tree.getDependencyId() + ":" + tree.getVersion() + "]";
    }

    // TODO make it public for now
    @Override
    public Class<?> loadClass(String s, boolean b) throws ClassNotFoundException {
        return super.loadClass(s, b);
    }
}

