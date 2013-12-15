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
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import static org.fusesource.common.util.Filters.isEmpty;

/**
 */
public class DependencyFilters {

    public static boolean matches(Dependency dependency, Filter<Dependency> excludeDependencyFilter) {
        return excludeDependencyFilter == null || excludeDependencyFilter.matches(dependency);
    }

    public static boolean matches(DependencyNode node, Filter<Dependency> excludeDependencyFilter) {
        return matches(node.getDependency(), excludeDependencyFilter);
    }

    public static final Filter<Dependency> testScopeFilter = createScopeFilter("test");

    public static final Filter<Dependency> createScopeFilter(final String scopeFilter) {
        return new Filter<Dependency>() {
            public boolean matches(Dependency Dependency) {
                String scope = Dependency.getScope();
                return scope != null && scopeFilter.equals(scope);
            }
        };
    }

    public static final Filter<Dependency> optionalFilter = new Filter<Dependency>() {
        public boolean matches(Dependency tree) {
            return tree.isOptional();
        }

        @Override
        public String toString() {
            return "OptionalFilter";
        }
    };

    public static final Filter<Dependency> testScopeOrOptionalFilter = Filters.or(testScopeFilter, optionalFilter);


    /**
     * Parsers the exclude dependency filter of the form "" for match none, "*" for all, or a space
     * separated list of "groupId:artifactId" allowing wildcards.
     * <p/>
     * By default it excludes all test scoped dependencies.
     */
    public static Filter<Dependency> parseExcludeFilter(String dependencyFilterText, Filter excludeOptionalDependenciesFilter) {
        Filter<Dependency> filter = parse(dependencyFilterText);
        // if no filter text then assume it matches nothing
        if (isEmpty(filter)) {
            return excludeOptionalDependenciesFilter;
        }
        return Filters.or(excludeOptionalDependenciesFilter, filter);
    }

    public static Filter<Dependency> parseExcludeOptionalFilter(String includeOptionalDependencyFilterText) {
        final Filter<Dependency> filter = parse(includeOptionalDependencyFilterText);
        final boolean excludeAll = isEmpty(filter);
        return new Filter<Dependency>() {
            @Override
            public boolean matches(Dependency tree) {
                if (tree.isOptional()) {
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
    public static Filter<Dependency> parseRequireBundleFilter(String filterText) {
        return parse(filterText);
    }

    /**
     * Creates a filter from the given String
     */
    public static Filter<Dependency> parse(String dependencyFilterText) {
        List<Filter<Dependency>> filters = new ArrayList<Filter<Dependency>>();
        StringTokenizer iter = new StringTokenizer(dependencyFilterText);
        while (iter.hasMoreElements()) {
            String text = iter.nextToken();
            Filter<Dependency> filter = parseSingleFilter(text);
            if (filter != null) {
                filters.add(filter);
            }
        }
        return Filters.compositeFilter(filters);
    }

    protected static Filter<Dependency> parseSingleFilter(String text) {
        String[] split = text.split(":");
        if (split == null || split.length == 0) {
            return null;
        } else {
            Filter<String> groupFilter = createStringFilter(split[0]);
            Filter<String> artifactFilter;
            if (split.length == 1) {
                artifactFilter = Filters.trueFilter();
            } else {
                artifactFilter = createStringFilter(split[1]);
            }
            return new DependencyFilter(groupFilter, artifactFilter);
        }
    }

    protected static Filter<String> createStringFilter(final String text) {
        if (text.startsWith("!")) {
            String remaining = text.substring(1);
            return Filters.not(createStringFilter(remaining));
        } else {
            if (text == null || text.length() == 0 || text.startsWith("*")) {
                return Filters.trueFilter();
            } else {
                if (text.endsWith("*")) {
                    final String prefix = text.substring(0, text.length() - 1);
                    return new Filter<String>() {
                        public boolean matches(String s) {
                            return s.startsWith(prefix);
                        }

                        @Override
                        public String toString() {
                            return "StartsWith(" + prefix + ")";
                        }
                    };

                } else {
                    return new Filter<String>() {
                        public boolean matches(String s) {
                            return text.equals(s);
                        }

                        @Override
                        public String toString() {
                            return "Equals(" + text + ")";
                        }
                    };
                }
            }
        }
    }
}
