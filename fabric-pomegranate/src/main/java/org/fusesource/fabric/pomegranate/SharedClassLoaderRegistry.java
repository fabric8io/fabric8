/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.pomegranate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A shared thread safe registry of {@link ClassLoader} instances that can be
 * shared across {@link DependencyTree} instances
 * when the dependency model makes sense to do so.
 */
public class SharedClassLoaderRegistry {

    private Map<DependencyTree, DependencyClassLoader> cache = new HashMap<DependencyTree, DependencyClassLoader>();

    /**
     * Returns the shared class loader for the given dependency tree
     */
    public DependencyClassLoader getClassLoader(DependencyTree tree) {
        DependencyClassLoader answer;
        synchronized (cache) {
            answer = cache.get(tree);
            if (answer == null) {
                List<DependencyClassLoader> childClassLoaders = new ArrayList<DependencyClassLoader>();
                List<DependencyTree> children = tree.getChildren();
                for (DependencyTree child : children) {
                    DependencyClassLoader childClassLoader = getClassLoader(child);
                    childClassLoaders.add(childClassLoader);
                }
                answer = new DependencyClassLoader(tree, childClassLoaders);
                cache.put(tree, answer);
            }
        }
        return answer;
    }


}
