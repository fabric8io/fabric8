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
package org.fusesource.bai;

import org.apache.camel.Predicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents the configuration of a kind of events
 */
public class EventTypeConfiguration {
    private boolean include = true;
    private List<String> includeRegexList = Arrays.asList(".*");
    private List<Predicate> filters = new ArrayList<Predicate>();

    public List<Predicate> getFilters() {
        return filters;
    }

    public void setFilters(List<Predicate> filters) {
        this.filters = filters;
    }

    public boolean isInclude() {
        return include;
    }

    public void setInclude(boolean include) {
        this.include = include;
    }

    public List<String> getIncludeRegexList() {
        return includeRegexList;
    }

    public void setIncludeRegexList(List<String> includeRegexList) {
        this.includeRegexList = includeRegexList;
    }
}
