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
import org.fusesource.common.util.Filters;

import java.util.*;

/**
 */
public class DependencyTreeFilters {
    protected static final Filter<DependencyTree> providedScopeFilter = createScopeFilter("provided");
    protected static final Filter<DependencyTree> testScopeFilter = createScopeFilter("test");

    protected static final Filter<DependencyTree> createScopeFilter(final String scopeFilter) {
        return new Filter<DependencyTree>() {
            public boolean matches(DependencyTree dependencyTree) {
                String scope = dependencyTree.getScope();
                return scope != null && scopeFilter.equals(scope);
            }
        };
    }

    public static final Filter<DependencyTree> optionalFilter = new Filter<DependencyTree>() {
        public boolean matches(DependencyTree tree) {
            return tree.isThisOrDescendantOptional();
        }

        @Override
        public String toString() {
            return "OptionalFilter";
        }
    };
    /**
     * Parsers a shared dependency filter of the form "" for match none, "*" for all, or a space
     * separated list of "groupId:artifactId" allowing wildcards.
     * <p/>
     * By default it shares all provided scoped dependencies.
     */
    public static Filter<DependencyTree> parseShareFilter(String dependencyFilterText) {
        Filter<DependencyTree> filter = parse(dependencyFilterText);
        return Filters.or(providedScopeFilter, filter);
    }

    /**
     * Parsers the exclude dependency filter of the form "" for match none, "*" for all, or a space
     * separated list of "groupId:artifactId" allowing wildcards.
     * <p/>
     * By default it excludes all test scoped dependencies.
     */
    public static Filter<DependencyTree> parseExcludeFilter(String dependencyFilterText, Filter excludeOptionalDependenciesFilter) {
        Filter<DependencyTree> filter = parse(dependencyFilterText);
        // if no filter text then assume it matches nothing
        if (Filters.isEmpty(filter)) {
            return Filters.or(testScopeFilter, excludeOptionalDependenciesFilter);
        }
        return Filters.or(testScopeFilter, excludeOptionalDependenciesFilter, filter);
    }

    public static Filter<DependencyTree> parseExcludeOptionalFilter(String includeOptionalDependencyFilterText) {
        final Filter<DependencyTree> filter = parse(includeOptionalDependencyFilterText);
        final boolean excludeAll = Filters.isEmpty(filter);
        return new Filter<DependencyTree>() {
            @Override
            public boolean matches(DependencyTree tree) {
                if (tree.isThisOrDescendantOptional()) {
                    if (excludeAll) {
                        return true;
                    } else {
                        // only exclude optional dependencies which don't match the include filter
                        return !filter.matches(tree);
                    }
                }
                return false;
            }
        };
    }

    /**
     * Parses the filter of which bundles should use the Require-Bundle instead of the default Import-Package
     */
    public static Filter<DependencyTree> parseRequireBundleFilter(String filterText) {
        return parse(filterText);
    }

    /**
     * Creates a filter from the given String
     */
    public static Filter<DependencyTree> parse(String dependencyFilterText) {
        List<Filter<DependencyTree>> filters = new ArrayList<Filter<DependencyTree>>();
        if (dependencyFilterText != null) {
            StringTokenizer iter = new StringTokenizer(dependencyFilterText);
            while (iter.hasMoreElements()) {
                String text = iter.nextToken();
                Filter<DependencyTree> filter = parseSingleFilter(text);
                if (filter != null) {
                    filters.add(filter);
                }
            }
        }
        return Filters.compositeFilter(filters);
    }

    protected static Filter<DependencyTree> parseSingleFilter(String text) {
        String[] split = text.split(":");
        if (split == null || split.length == 0) {
            return null;
        } else {
            Filter<String> groupFilter = Filters.createStringFilter(split[0]);
            Filter<String> artifactFilter;
            if (split.length == 1) {
                artifactFilter = Filters.trueFilter();
            } else {
                artifactFilter = Filters.createStringFilter(split[1]);
            }
            return new DependencyTreeFilter(groupFilter, artifactFilter);
        }
    }

    /**
     * Prune children (and their descendants) from the dependency tree if they match the filter provided.
     *
     * @param root the dependency tree to be pruned
     * @param filter the filter used for finding the children to be removed
     */
    public static void prune(DependencyTree root, Filter<DependencyTree> filter) {
        List<DependencyTree> children = new LinkedList<DependencyTree>();
        children.addAll(root.getChildren());

        for (DependencyTree child : children) {
            if (filter.matches(child)) {
                root.getChildren().remove(child);
            } else {
                prune(child, filter);
            }
        }
    }
}
