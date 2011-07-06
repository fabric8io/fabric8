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
import org.fusesource.fabric.pomegranate.util.Filters;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 */
public class DependencyTreeFilters {
    /**
     * Parsers a dependency filter of the form "" for match none, "*" for all, or a space
     * separated list of "groupId:artifactId" allowing wildcards
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
