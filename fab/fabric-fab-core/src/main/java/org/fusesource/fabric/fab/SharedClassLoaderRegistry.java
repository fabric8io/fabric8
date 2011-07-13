/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab;

import org.fusesource.fabric.fab.util.Filter;

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
    public DependencyClassLoader getClassLoader(DependencyTree tree, Filter<DependencyTree> sharedFilter, Filter<DependencyTree> excludeFilter) throws MalformedURLException {
        DependencyClassLoader answer = null;
        if (excludeFilter != null && excludeFilter.matches(tree)) {
            // ignore
        }
        else if (sharedFilter != null && sharedFilter.matches(tree)) {
            synchronized (cache) {
                answer = cache.get(tree);
                if (answer == null) {
                    answer = createClassLoader(tree, sharedFilter, excludeFilter);
                    cache.put(tree, answer);
                }
            }
        } else {
            answer = createClassLoader(tree, sharedFilter, excludeFilter);
        }
        return answer;
    }

    protected DependencyClassLoader createClassLoader(DependencyTree tree, Filter<DependencyTree> sharedFilter, Filter<DependencyTree> excludeFilter) throws MalformedURLException {
        List<DependencyClassLoader> childClassLoaders = new ArrayList<DependencyClassLoader>();
        List<DependencyTree> children = tree.getChildren();
        List<DependencyTree> nonSharedDependencies = new ArrayList<DependencyTree>();
        for (DependencyTree child : children) {
            if (excludeFilter != null && excludeFilter.matches(tree)) {
                continue;
            } else if (sharedFilter != null && sharedFilter.matches(child)) {
                DependencyClassLoader childClassLoader = getClassLoader(child, sharedFilter, excludeFilter);
                if (childClassLoader != null) {
                    childClassLoaders.add(childClassLoader);
                }
            } else {
                // we now need to recursively flatten all non-shared dependencies...
                nonSharedDependencies.add(child);
                child.addDescendants(nonSharedDependencies);
            }
        }
        return DependencyClassLoader.newInstance(tree, nonSharedDependencies, childClassLoaders, null);
    }


}
