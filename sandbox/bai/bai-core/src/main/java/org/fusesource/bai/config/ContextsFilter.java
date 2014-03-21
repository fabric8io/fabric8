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

package org.fusesource.bai.config;

import org.fusesource.bai.agent.CamelContextService;
import org.fusesource.bai.support.FilterHelpers;
import org.fusesource.common.util.Filter;
import org.fusesource.common.util.Filters;
import org.fusesource.common.util.Strings;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a filter of CamelContexts using the bundle and camelContext name patterns using * to match any characer
 */
@XmlRootElement(name = "contexts")
@XmlAccessorType(XmlAccessType.FIELD)
public class ContextsFilter extends HasIdentifier implements Filter<CamelContextService> {
    @XmlElementWrapper(name = "include", required = false)
    @XmlElement(name = "context", required = false)
    private List<ContextFilter> includeContextFilters;

    @XmlElementWrapper(name = "exclude", required = false)
    @XmlElement(name = "context", required = false)
    private List<ContextFilter> excludeContextFilters;

    public ContextsFilter() {
    }

    @Override
    public String toString() {
        return "Contexts(" +
                FilterHelpers.includeExcludeListsToText(includeContextFilters, excludeContextFilters) +
                ")";
    }


    /**
     * Parses a space separated set of patterns of the form "bundle:pattern"
     */
    public void addPattern(boolean include, String pattern) {
        String name = "*";
        String bundle = pattern;
        String[] values = pattern.split(":", 2);
        if (values != null && values.length > 1) {
            bundle = values[0];
            name = values[1];
        }
        bundle = Strings.defaultIfEmpty(bundle, "*");
        name = Strings.defaultIfEmpty(name, "*");
        if (include) {
            includeContext(bundle, name);
        } else {
            excludeContext(bundle, name);
        }
    }

    public ContextsFilter excludeContext(String bundle, String name) {
        return excludeContext(new ContextFilter(bundle, name));
    }

    public ContextsFilter excludeContext(ContextFilter filter) {
        if (excludeContextFilters == null) {
            excludeContextFilters = new ArrayList<ContextFilter>();
        }
        excludeContextFilters.add(filter);
        return this;
    }

    public ContextsFilter includeContext(String bundle, String name) {
        return includeContext(new ContextFilter(bundle, name));
    }

    public ContextsFilter includeContext(ContextFilter filter) {
        if (includeContextFilters == null) {
            includeContextFilters = new ArrayList<ContextFilter>();
        }
        includeContextFilters.add(filter);
        return this;
    }

    public boolean matches(CamelContextService contextService) {
        return Filters.matches(contextService, includeContextFilters, excludeContextFilters);
    }

    // Properties
    //-------------------------------------------------------------------------
    public List<ContextFilter> getExcludeContextFilters() {
        return excludeContextFilters;
    }

    public void setExcludeContextFilters(List<ContextFilter> excludeContextFilters) {
        this.excludeContextFilters = excludeContextFilters;
    }

    public List<ContextFilter> getIncludeContextFilters() {
        return includeContextFilters;
    }

    public void setIncludeContextFilters(List<ContextFilter> includeContextFilters) {
        this.includeContextFilters = includeContextFilters;
    }

}
