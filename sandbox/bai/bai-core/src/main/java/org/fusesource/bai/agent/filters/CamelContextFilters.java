/*
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
package org.fusesource.bai.agent.filters;

import org.fusesource.bai.agent.CamelContextService;
import org.fusesource.common.util.Filter;
import org.fusesource.common.util.Filters;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 */
public class CamelContextFilters {

    /**
     * Creates a new filter taking a String of the form: bundleIdPattern[:camelContextIdPattern]
     * <p/>
     * where a pattern uses a String with * for any number of characters and ! at the front meaning to not match the pattern
     */
    public static Filter<CamelContextService> createCamelContextFilter(String contextFilterText) {
        List<Filter<CamelContextService>> filters = new ArrayList<Filter<CamelContextService>>();
        if (contextFilterText != null) {
            StringTokenizer iter = new StringTokenizer(contextFilterText);
            while (iter.hasMoreElements()) {
                String text = iter.nextToken();
                Filter<CamelContextService> filter = parseSingleFilter(text);
                if (filter != null) {
                    filters.add(filter);
                }
            }
        }
        return Filters.compositeFilter(filters);
    }

    /**
     * Creates a new filter taking a List<String> with items of the form: bundleIdPattern[:camelContextIdPattern]
     * <p/>
     * where a pattern uses a String with * for any number of characters and ! at the front meaning to not match the pattern
     */
    public static Filter<CamelContextService> createCamelContextFilter(List<String> contextFilterItems) {
        List<Filter<CamelContextService>> filters = new ArrayList<Filter<CamelContextService>>();
        if (contextFilterItems != null && contextFilterItems.size() > 0) {
            for (String text : contextFilterItems) {
                Filter<CamelContextService> filter = parseSingleFilter(text);
                if (filter != null) {
                    filters.add(filter);
                }
            }
        }
        return Filters.compositeFilter(filters);
    }

    protected static Filter<CamelContextService> parseSingleFilter(String text) {
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
            return new CamelContextServiceFilter(groupFilter, artifactFilter);
        }
    }
}
