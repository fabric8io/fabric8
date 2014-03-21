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
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Predicate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.ExpressionClause;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.camel.util.CamelContextHelper;
import org.fusesource.bai.AuditEvent;
import org.fusesource.bai.AuditEventNotifier;
import org.fusesource.bai.agent.CamelContextService;
import org.fusesource.common.util.Strings;

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
    @XmlAttribute
    private String to = "vm:audit";

    @XmlElement(required = false)
    private ContextsFilter contexts;

    @XmlElement(required = false)
    private EndpointsFilter endpoints;

    @XmlElement(required = false)
    private EventsFilter events;

    @XmlElement(required = false)
    private ExchangeFilter filter;

    @XmlElement(required = false)
    private BodyExpression body;

    @XmlTransient
    private Predicate predicate;

    @XmlTransient
    private Expression bodyExpression;

    @XmlTransient
    private Endpoint toEndpoint;

    public Policy() {
    }

    public Policy(String id) {
        super(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
                Strings.joinNotNull(", ",
                        getId(),
                        "to: " + getTo(),
                        isEnabled() ? null : "disabled",
                        contexts,
                        endpoints,
                        events,
                        filter) + ")";
    }

    /**
     * Returns true if this policy is enabled
     */
    public boolean isEnabled() {
        return enabled == null || enabled.booleanValue();
    }

    /**
     * Processes the audit event
     */
    public void process(AuditEventNotifier auditor, AuditEvent auditEvent) {
        if (matchesEvent(auditEvent)) {
            ProducerTemplate producer = auditor.getProducerTemplate();
            Endpoint endpoint = getToEndpoint(auditor.getCamelContext());
            if (endpoint != null) {
                Exchange exchange = endpoint.createExchange();
                // make sure we don't send out events for this as well
                // mark exchange as being published to event, to prevent creating new events
                // for this as well (causing a endless flood of events)
                exchange.setProperty(Exchange.NOTIFY_EVENT, Boolean.TRUE);

                Object payload = createPayload(auditEvent);
                exchange.getIn().setBody(payload);
                try {
                    producer.send(endpoint, exchange);
                } finally {
                    // TODO why do we bother removing the notify event flag???
                    // and remove it when its done
                    exchange.removeProperty(Exchange.NOTIFY_EVENT);
                }
            }
        }
    }

    /**
     * Returns true if this policy matches the given Camel context service
     */
    public boolean matchesContext(CamelContextService contextService) {
        return isEnabled() && (contexts == null || contexts.matches(contextService));
    }

    public boolean matchesEvent(AuditEvent event) {
        if (isEnabled() && (events == null || events.matches(event)) &&
                (endpoints == null || endpoints.matches(event))) {

            Exchange exchange = event.getExchange();
            if (exchange != null) {
                if (predicate == null) {
                    ExpressionDefinition expression = null;
                    if (filter != null) {
                        expression = filter.getExpression();
                    }
                    if (expression != null) {
                        predicate = expression.createPredicate(event.getCamelContext());
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


    /**
     * Sets the output endpoint
     */
    public Policy to(String to) {
        setTo(to);
        return this;
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
     * Use the DSL to create a filter predicate
     */
    public ExpressionClause<Policy> filter() {
        ExpressionClause<Policy> clause = new ExpressionClause<Policy>(this);
        setFilter(new ExchangeFilter(clause));
        return clause;
    }

    /**
     * Use the DSL to create a body expression
     */
    public ExpressionClause<Policy> body() {
        ExpressionClause<Policy> clause = new ExpressionClause<Policy>(this);
        setBody(new BodyExpression(clause));
        return clause;
    }

    // Properties
    //-------------------------------------------------------------------------
    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
        this.toEndpoint = null;
    }

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

    public BodyExpression getBody() {
        return body;
    }

    public void setBody(BodyExpression body) {
        this.body = body;
        this.bodyExpression = null;
    }

    public Predicate getPredicate() {
        return predicate;
    }

    public Expression getBodyExpression() {
        return bodyExpression;
    }


    // Implementation methods
    //-------------------------------------------------------------------------
    protected Object createPayload(AuditEvent event) {
        Exchange exchange = event.getExchange();
        if (exchange != null) {
            if (bodyExpression == null) {
                ExpressionDefinition expression = null;
                if (body != null) {
                    expression = body.getExpression();
                }
                if (expression != null) {
                    bodyExpression = expression.createExpression(event.getCamelContext());
                }
            }
            if (bodyExpression != null) {
                return bodyExpression.evaluate(exchange, Object.class);
            }
        }
        return event;
    }

    /**
     * Returns the to endpoint, lazily resolving it if need be
     */
    protected Endpoint getToEndpoint(CamelContext camelContext) {
        if (toEndpoint == null) {
            if (to != null) {
                toEndpoint = CamelContextHelper.getMandatoryEndpoint(camelContext, to);
            }
        }
        return toEndpoint;
    }

}
