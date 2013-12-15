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

