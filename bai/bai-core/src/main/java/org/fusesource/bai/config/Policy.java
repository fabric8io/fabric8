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

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.builder.ExpressionClause;
import org.apache.camel.model.language.ExpressionDefinition;
import org.fusesource.bai.AuditEvent;
import org.fusesource.bai.agent.CamelContextService;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Represents a policy of auditing that applies to a selection of CamelContexts
 */
@XmlRootElement(name = "policy")
@XmlAccessorType(XmlAccessType.FIELD)
public class Policy extends HasIdentifier {
    @XmlAttribute(required = false)
    private Boolean enabled;

    @XmlElement(required = false)
    private ContextsFilter contexts;

    @XmlElement(required = false)
    private EndpointsFilter endpoints;

    @XmlElement(required = false)
    private EventsFilter events;

    @XmlElement(required = false)
    private ExchangeFilter filter;

    @XmlTransient
    private Predicate predicate;

    public Policy() {
    }

    public Policy(String id) {
        super(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(enabled: " + isEnabled() + " " + contexts +
                " " + events +
                (filter != null ? " " + filter : "") +
                ")";
    }

    public boolean isEnabled() {
        return enabled == null || enabled.booleanValue();
    }

    public boolean matchesContext(CamelContextService contextService) {
        return contexts == null || contexts.matches(contextService);
    }

    public boolean matchesEvent(AuditEvent event) {
        if (isEnabled() && (events == null || events.matches(event)) &&
                (endpoints == null || endpoints.matches(event))) {

            Exchange exchange = event.getExchange();
            if (exchange != null) {
                if (predicate == null) {
                    ExpressionDefinition expression = null;
                    if (filter != null) {
                        expression = filter.getFilter();
                    }
                    if (expression != null) {
                        CamelContext camelContext = exchange.getContext();
                        predicate = expression.createPredicate(camelContext);
                    }
                }
                if (predicate != null) {
                    return predicate.matches(exchange);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Returns the contexts filter, lazily creating one if it does not exist
     */
    public ContextsFilter contexts() {
        if (contexts == null) {
            contexts = new ContextsFilter();
        }
        return contexts;
    }

    /**
     * Returns the events filter, lazily creating one if it does not exist
     */
    public EventsFilter events() {
        if (events == null) {
            events = new EventsFilter();
        }
        return events;
    }

    /**
     * Returns the endpoints filter, lazily creating one if it does not exist
     */
    public EndpointsFilter endpoints() {
        if (endpoints == null) {
            endpoints = new EndpointsFilter();
        }
        return endpoints;
    }


    public Policy excludeContext(String bundle, String name) {
        contexts().excludeContext(bundle, name);
        return this;
    }

    public Policy excludeContext(ContextFilter filter) {
        contexts().excludeContext(filter);
        return this;
    }

    public Policy includeContext(String bundle, String name) {
        contexts().includeContext(bundle, name);
        return this;
    }

    public Policy includeContext(ContextFilter filter) {
        contexts().includeContext(filter);
        return this;
    }

    public Policy excludeEvent(EventType eventType) {
        events().excludeEvent(eventType);
        return this;
    }

    public Policy excludeEvent(EventFilter filter) {
        events().excludeEvent(filter);
        return this;
    }

    public Policy includeEvent(EventType eventType) {
        events().includeEvent(eventType);
        return this;
    }

    public Policy includeEvent(EventFilter filter) {
        events().includeEvent(filter);
        return this;
    }

    public Policy excludeEndpoint(EndpointFilter filter) {
        endpoints().excludeEndpoint(filter);
        return this;
    }

    public Policy excludeEndpoint(String pattern) {
        endpoints().excludeEndpoint(pattern);
        return this;
    }

    public Policy includeEndpoint(String pattern) {
        endpoints().includeEndpoint(pattern);
        return this;
    }

    public Policy includeEndpoint(EndpointFilter filter) {
        endpoints().includeEndpoint(filter);
        return this;
    }

    /**
     * Use the DSL to create an expression
     */
    public ExpressionClause<Policy> filter() {
        ExpressionClause<Policy> clause = new ExpressionClause<Policy>(this);
        setFilter(new ExchangeFilter(clause));
        return clause;
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

    public ExchangeFilter getFilter() {
        return filter;
    }

    public void setFilter(ExchangeFilter filter) {
        this.filter = filter;
        this.predicate = null;
    }

    public Predicate getPredicate() {
        return predicate;
    }
}
