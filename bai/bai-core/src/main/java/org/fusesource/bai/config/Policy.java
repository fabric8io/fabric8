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
import org.fusesource.bai.EventType;
import org.fusesource.bai.agent.CamelContextService;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a policy of auditing that applies to a selection of CamelContexts
 */
@XmlRootElement(name = "policy")
@XmlAccessorType(XmlAccessType.FIELD)
public class Policy extends HasIdentifier {
    @XmlElement
    private ContextsFilter contexts = new ContextsFilter();

    @XmlElement
    private EventsFilter events = new EventsFilter();

    @XmlElement
    private EndpointsFilter endpoints = new EndpointsFilter();

    @XmlAttribute(required = false)
    private Boolean enabled;

    public Policy() {
    }

    public Policy(String id) {
        super(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(enabled: " + isEnabled() + " " + contexts +
                " " + events + ")";
    }

    public boolean isEnabled() {
        return enabled == null || enabled.booleanValue();
    }

    public boolean matchesContext(CamelContextService contextService) {
        return contexts == null || contexts.matches(contextService);
    }

    public boolean matchesEvent(AuditEvent event) {
        return isEnabled() && (events == null || events.matches(event)) &&
                (endpoints == null || endpoints.matches(event));
    }

    public Policy excludeContext(String bundle, String name) {
        getContexts().excludeContext(bundle, name);
        return this;
    }

    public Policy excludeContext(ContextFilter filter) {
        getContexts().excludeContext(filter);
        return this;
    }

    public Policy includeContext(String bundle, String name) {
        getContexts().includeContext(bundle, name);
        return this;
    }

    public Policy includeContext(ContextFilter filter) {
        getContexts().includeContext(filter);
        return this;
    }

    public Policy excludeEvent(EventType eventType) {
        getEvents().excludeEvent(eventType);
        return this;
    }

    public Policy excludeEvent(EventFilter filter) {
        getEvents().excludeEvent(filter);
        return this;
    }

    public Policy includeEvent(EventType eventType) {
        getEvents().includeEvent(eventType);
        return this;
    }

    public Policy includeEvent(EventFilter filter) {
        getEvents().includeEvent(filter);
        return this;
    }

    public Policy excludeEndpoint(EndpointFilter filter) {
        getEndpoints().excludeEndpoint(filter);
        return this;
    }

    public Policy excludeEndpoint(String pattern) {
        getEndpoints().excludeEndpoint(pattern);
        return this;
    }

    public Policy includeEndpoint(String pattern) {
        getEndpoints().includeEndpoint(pattern);
        return this;
    }

    public Policy includeEndpoint(EndpointFilter filter) {
        getEndpoints().includeEndpoint(filter);
        return this;
    }

    // Properties
    //-------------------------------------------------------------------------
    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public ContextsFilter getContexts() {
        return contexts;
    }

    public void setContexts(ContextsFilter contexts) {
        this.contexts = contexts;
    }

    public EndpointsFilter getEndpoints() {
        return endpoints;
    }

    public EventsFilter getEvents() {
        return events;
    }
}
