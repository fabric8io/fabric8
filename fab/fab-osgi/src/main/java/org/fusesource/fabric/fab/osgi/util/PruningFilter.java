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
package org.fusesource.fabric.fab.osgi.util;

import org.fusesource.fabric.fab.DependencyTree;
import org.fusesource.fabric.fab.osgi.internal.FabClassPathResolver;
import org.fusesource.fabric.fab.util.Filter;

/**
 * A pruning filter is used by {@link FabClassPathResolver} to prune the resolved dependency tree (e.g. removing
 * already installed bundles or features and their dependencies from the tree) after the initial resolution of dependencies.
 *
 * The filter can be enabled or disabled per resolver, depending on the requirements or the configuration.
 */
public interface PruningFilter extends Filter<DependencyTree> {

    /**
     * Is this pruning filter enabled for the provided FabClassPathResolver?
     *
     * @param resolver the resolver
     * @return <code>true</code> if the pruning filter should be used by this resolver
     */
    //TODO: Add an interface here to avoid the tight coupling between the filter and the resolver
    public boolean isEnabled(FabClassPathResolver resolver);

}
