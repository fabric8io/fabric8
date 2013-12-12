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

import org.fusesource.common.util.Filter;

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
