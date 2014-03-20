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
 * Represents a filter of audit {@link EventType}
 */
@XmlRootElement(name = "events")
@XmlAccessorType(XmlAccessType.FIELD)
public class EventsFilter extends HasIdentifier implements Filter<AuditEvent> {
    @XmlElementWrapper(name = "include", required = false)
    @XmlElement(name = "event", required = false)
    private List<EventFilter> includeEventFilters;

    @XmlElementWrapper(name = "exclude", required = false)
    @XmlElement(name = "event", required = false)
    private List<EventFilter> excludeEventFilters;

    public EventsFilter() {
    }

    @Override
    public String toString() {
        return "Events(" +
                FilterHelpers.includeExcludeListsToText(includeEventFilters, excludeEventFilters) +
                ")";
    }

    public void addEvent(boolean include, EventType eventType) {
        if (include) {
            includeEvent(eventType);
        } else {
            excludeEvent(eventType);
        }
    }

    public EventsFilter excludeEvent(EventType eventType) {
        return excludeEvent(new EventFilter(eventType));
    }

    public EventsFilter excludeEvent(EventFilter filter) {
        if (excludeEventFilters == null) {
            excludeEventFilters = new ArrayList<EventFilter>();
        }
        excludeEventFilters.add(filter);
        return this;
    }

    public EventsFilter includeEvent(EventType eventType) {
        return includeEvent(new EventFilter(eventType));
    }

    public EventsFilter includeEvent(EventFilter filter) {
        if (includeEventFilters == null) {
            includeEventFilters = new ArrayList<EventFilter>();
        }
        includeEventFilters.add(filter);
        return this;
    }

    public boolean matches(AuditEvent event) {
        return Filters.matches(event, includeEventFilters, excludeEventFilters);
    }

    // Properties
    //-------------------------------------------------------------------------
    public List<EventFilter> getExcludeEventFilters() {
        return excludeEventFilters;
    }

    public void setExcludeEventFilters(List<EventFilter> excludeEventFilters) {
        this.excludeEventFilters = excludeEventFilters;
    }

    public List<EventFilter> getIncludeEventFilters() {
        return includeEventFilters;
    }

    public void setIncludeEventFilters(List<EventFilter> includeEventFilters) {
        this.includeEventFilters = includeEventFilters;
    }

}
