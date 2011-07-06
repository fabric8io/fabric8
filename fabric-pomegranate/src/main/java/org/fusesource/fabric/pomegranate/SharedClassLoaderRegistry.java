/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.pomegranate;

import org.fusesource.fabric.pomegranate.util.Filter;

import java.net.MalformedURLException;
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
    public DependencyClassLoader getClassLoader(DependencyTree tree, Filter<DependencyTree> sharedFilter) throws MalformedURLException {
        DependencyClassLoader answer = null;
        if (sharedFilter != null && sharedFilter.matches(tree)) {
            synchronized (cache) {
                answer = cache.get(tree);
                if (answer == null) {
                    answer = createClassLoader(tree, sharedFilter);
                    cache.put(tree, answer);
                }
            }
        } else {
            answer = createClassLoader(tree, sharedFilter);
        }
        return answer;
    }

    protected DependencyClassLoader createClassLoader(DependencyTree tree, Filter<DependencyTree> sharedFilter) throws MalformedURLException {
        List<DependencyClassLoader> childClassLoaders = new ArrayList<DependencyClassLoader>();
        List<DependencyTree> children = tree.getChildren();
        List<DependencyTree> nonSharedDependencies = new ArrayList<DependencyTree>();
        for (DependencyTree child : children) {
            if (sharedFilter != null && sharedFilter.matches(child)) {
                DependencyClassLoader childClassLoader = getClassLoader(child, sharedFilter);
                if (childClassLoader != null) {
                    childClassLoaders.add(childClassLoader);
                }
            } else {
                nonSharedDependencies.add(child);
            }
        }
        return DependencyClassLoader.newInstance(tree, nonSharedDependencies, childClassLoaders, null);
    }


}
