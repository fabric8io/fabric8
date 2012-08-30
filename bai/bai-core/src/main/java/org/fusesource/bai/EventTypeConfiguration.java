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
import org.fusesource.common.util.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents the configuration of a kind of events
 */
public class EventTypeConfiguration {
	public static final String CATCH_ALL_REGEX = ".*";
    private boolean include = true;
    private List<String> endpointIncludeRegexps = Arrays.asList(CATCH_ALL_REGEX);
    private List<Predicate> exchangeFilters = new ArrayList<Predicate>();

    @Override
    public String toString() {
        return "EventTypeConfig(" + include + ", " + endpointIncludeRegexps + ", " + exchangeFilters + ")";
    }

    public List<Predicate> getExchangeFilters() {
    	if (exchangeFilters == null) {
    		exchangeFilters = new ArrayList<Predicate>();
    	}
        return exchangeFilters;
    }

    public void setExchangeFilters(List<Predicate> exchangeFilters) {
        this.exchangeFilters = exchangeFilters;
    }

    public boolean isInclude() {
        return include;
    }

    public void setInclude(boolean include) {
        this.include = include;
    }

    public List<String> getEndpointIncludeRegexps() {
    	if (endpointIncludeRegexps == null) {
    		endpointIncludeRegexps = new ArrayList<String>();
    	}
        return endpointIncludeRegexps;
    }
    
    public void addEndpointIncludeRegexp(String regexp) {
    	// if it only contains 1 element and that element is the wildcard
    	if (this.getEndpointIncludeRegexps().size() == 1 && this.getEndpointIncludeRegexps().contains(CATCH_ALL_REGEX)) {
    		endpointIncludeRegexps.clear();
    	}
    	endpointIncludeRegexps.add(regexp);
    }

    public void setEndpointIncludeRegexps(List<String> endpointIncludeRegexps) {
        this.endpointIncludeRegexps = endpointIncludeRegexps;
    }

    /**
     * Sets the event include flag to true/false (true if empty)
     */
    public void configureEventFlag(String value) {
        include = Strings.isNullOrBlank(value) || !value.equalsIgnoreCase("false");
    }
}
