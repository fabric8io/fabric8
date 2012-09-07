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

import org.fusesource.bai.AuditEvent;
import org.fusesource.bai.support.FilterHelpers;
import org.fusesource.common.util.Filter;
import org.fusesource.common.util.Filters;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a filter of endpoints
 */
@XmlRootElement(name = "endpoints")
@XmlAccessorType(XmlAccessType.FIELD)
public class EndpointsFilter extends HasIdentifier implements Filter<AuditEvent> {
    @XmlElementWrapper(name = "include", required = false)
    @XmlElement(name = "endpoint", required = false)
    private List<EndpointFilter> includeEndpointFilters;

    @XmlElementWrapper(name = "exclude", required = false)
    @XmlElement(name = "endpoint", required = false)
    private List<EndpointFilter> excludeEndpointFilters;

    public EndpointsFilter() {
    }

    @Override
    public String toString() {
        return "Endpoints(" +
                FilterHelpers.includeExcludeListsToText(includeEndpointFilters, excludeEndpointFilters) +
                ")";
    }


    public void addPattern(boolean include, String pattern) {
        if (include) {
            includeEndpoint(pattern);
        } else {
            excludeEndpoint(pattern);
        }
    }

    public EndpointsFilter excludeEndpoint(String pattern) {
        return excludeEndpoint(new EndpointFilter(pattern));
    }

    public EndpointsFilter excludeEndpoint(EndpointFilter filter) {
        if (excludeEndpointFilters == null) {
            excludeEndpointFilters = new ArrayList<EndpointFilter>();
        }
        excludeEndpointFilters.add(filter);
        return this;
    }

    public EndpointsFilter includeEndpoint(String pattern) {
        return includeEndpoint(new EndpointFilter(pattern));
    }

    public EndpointsFilter includeEndpoint(EndpointFilter filter) {
        if (includeEndpointFilters == null) {
            includeEndpointFilters = new ArrayList<EndpointFilter>();
        }
        includeEndpointFilters.add(filter);
        return this;
    }

    public boolean matches(AuditEvent event) {
        return Filters.matches(event, includeEndpointFilters, excludeEndpointFilters);
    }

    // Properties
    //-------------------------------------------------------------------------
    public List<EndpointFilter> getExcludeEndpointFilters() {
        return excludeEndpointFilters;
    }

    public void setExcludeEndpointFilters(List<EndpointFilter> excludeEndpointFilters) {
        this.excludeEndpointFilters = excludeEndpointFilters;
    }

    public List<EndpointFilter> getIncludeEndpointFilters() {
        return includeEndpointFilters;
    }

    public void setIncludeEndpointFilters(List<EndpointFilter> includeEndpointFilters) {
        this.includeEndpointFilters = includeEndpointFilters;
    }

}
