/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab;

import org.fusesource.fabric.fab.util.CompositeFilter;
import org.fusesource.fabric.fab.util.Filter;
import org.fusesource.fabric.fab.util.Filters;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

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
    public static Filter<DependencyTree> parseExcludeFilter(String dependencyFilterText, String includeOptionalDependencyFilterText) {
        Filter<DependencyTree> filter = parse(dependencyFilterText);
        Filter<DependencyTree> filteredOptionals = createExcludeOptionalFilter(includeOptionalDependencyFilterText);
        Filter<DependencyTree> filterScopes = Filters.or(testScopeFilter, filteredOptionals);
        // if no filter text then assume it matches nothing
        if (isEmpty(filter)) {
            return filterScopes;
        }
        return Filters.or(filterScopes, filter);
    }

    private static Filter<DependencyTree> createExcludeOptionalFilter(String includeOptionalDependencyFilterText) {
        final Filter<DependencyTree> filter = parse(includeOptionalDependencyFilterText);
        final boolean excludeAll = isEmpty(filter);
        return new Filter<DependencyTree>() {
            @Override
            public boolean matches(DependencyTree tree) {
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

    protected static boolean isEmpty(Filter<DependencyTree> filter) {
        boolean empty = false;
        if (filter instanceof CompositeFilter) {
            // lets treat empty filters as not matching anything
            CompositeFilter<DependencyTree> compositeFilter = (CompositeFilter<DependencyTree>) filter;
            empty = compositeFilter.isEmpty();
        }
        return empty;
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
        StringTokenizer iter = new StringTokenizer(dependencyFilterText);
        while (iter.hasMoreElements()) {
            String text = iter.nextToken();
            Filter<DependencyTree> filter = parseSingleFilter(text);
            if (filter != null) {
                filters.add(filter);
            }
        }
        return Filters.compositeFilter(filters);
    }

    protected static Filter<DependencyTree> parseSingleFilter(String text) {
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
            return new DependencyTreeFilter(groupFilter, artifactFilter);
        }
    }

    protected static Filter<String> createStringFilter(final String text) {
        if (text == null || text.length() == 0 || text.startsWith("*")) {
            return Filters.trueFilter();
        } else {
            return new Filter<String>() {
                public boolean matches(String s) {
                    return text.equals(s);
                }
            };
        }
    }
}
