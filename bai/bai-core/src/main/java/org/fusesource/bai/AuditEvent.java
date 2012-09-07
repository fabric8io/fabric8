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

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.management.event.AbstractExchangeEvent;
import org.apache.camel.management.event.ExchangeCompletedEvent;
import org.apache.camel.management.event.ExchangeCreatedEvent;
import org.apache.camel.management.event.ExchangeFailedEvent;
import org.apache.camel.management.event.ExchangeFailureHandledEvent;
import org.apache.camel.management.event.ExchangeRedeliveryEvent;
import org.apache.camel.management.event.ExchangeSendingEvent;
import org.apache.camel.management.event.ExchangeSentEvent;
import org.fusesource.bai.config.EventType;

import java.util.Date;
import java.util.EventObject;

/**
 * DTO that represents an AuditEvent
 *
 * @author raul
 */
public class AuditEvent extends AbstractExchangeEvent {
    private static final long serialVersionUID = 6818757465057171170L;

    private final AbstractExchangeEvent event;
    private final Date timestamp;
    private final String endpointURI;
    private final Exception exception;
    private final String sourceContextId;
    private final String sourceRouteId;
    private final String breadCrumbId;
    private final Boolean redelivered;
    private final String currentRouteId;
    private final EventType eventType;

    public AuditEvent(Exchange source, AbstractExchangeEvent event) {
        super(source);
        this.event = event;
        this.timestamp = new Date();

        this.endpointURI = endpointUri(event);
        this.sourceContextId = source.getContext().getName();
        // if the UoW exists, get the info from there as it's more accurate; if it doesn't exist, we have received an ExchangeCreated event so the info in the
        // exchange's fromRouteId will be correct anyway... so it's all good
        // all consumer endpoints create an exchange (ExchangeCreatedEvent) except for direct (even seda and VM create new Exchanges)
        // the sourceRouteId serves as correlation and in the MongoDB backend, it's part of the collection name
        // if the routeId changes WITHOUT an exchange being created in the new route, the event will never be written because
        // sourceRouteId: what route created the Exchange
        this.sourceRouteId = source.getFromRouteId();
        // currentRouteId: what route the Exchange currently is in
        this.currentRouteId = (source.getUnitOfWork() == null || source.getUnitOfWork().getRouteContext() == null)
                ? null : source.getUnitOfWork().getRouteContext().getRoute().getId();
        this.breadCrumbId = getBreadCrumbId(source);
        this.exception = source.getException() == null ? source.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class) : source.getException();
        this.redelivered = source.getProperty(Exchange.REDELIVERED, boolean.class);

        if (event instanceof ExchangeCreatedEvent) {
            eventType = EventType.CREATED;
        } else if (event instanceof ExchangeCompletedEvent) {
            eventType = EventType.COMPLETED;
        } else if (event instanceof ExchangeSendingEvent) {
            eventType = EventType.SENDING;
        } else if (event instanceof ExchangeSentEvent) {
            eventType = EventType.SENT;
        } else if (event instanceof ExchangeRedeliveryEvent) {
            eventType = EventType.REDELIVERY;
        } else if (event instanceof ExchangeFailureHandledEvent) {
            eventType = EventType.FAILURE_HANDLED;
        } else {
            eventType = EventType.FAILURE;
        }
    }

    public static String endpointUri(EventObject event) {
        if (event instanceof AuditEvent) {
            AuditEvent auditEvent = (AuditEvent) event;
            return auditEvent.getEndpointURI();
        } else if (event instanceof ExchangeSendingEvent) {
            ExchangeSendingEvent sentEvent = (ExchangeSendingEvent) event;
            return sentEvent.getEndpoint().getEndpointUri();
        } else if (event instanceof ExchangeSentEvent) {
            ExchangeSentEvent sentEvent = (ExchangeSentEvent) event;
            return sentEvent.getEndpoint().getEndpointUri();
        } else if (event instanceof AbstractExchangeEvent) {
            AbstractExchangeEvent ae = (AbstractExchangeEvent) event;
            Exchange exchange = ae.getExchange();
            if (event instanceof ExchangeFailureHandledEvent || event instanceof ExchangeFailedEvent) {
                return exchange.getProperty(Exchange.FAILURE_ENDPOINT, String.class);
            } else {
                Endpoint fromEndpoint = exchange.getFromEndpoint();
                if (fromEndpoint != null) {
                    return fromEndpoint.getEndpointUri();
                }
            }
        }
        return null;
    }

    protected String getBreadCrumbId(Exchange source) {
        String bid = source.getIn().getHeader(Exchange.BREADCRUMB_ID, String.class);
        if (bid == null && source.hasOut()) {
            bid = source.getOut().getHeader(Exchange.BREADCRUMB_ID, String.class);
        }
        return bid;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public AbstractExchangeEvent getEvent() {
        return event;
    }

    public CamelContext getCamelContext() {
        Exchange exchange = getExchange();
        if (exchange != null) {
            return exchange.getContext();
        }
        return null;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getEndpointURI() {
        return endpointURI;
    }

    public Exception getException() {
        return exception;
    }

    public String getSourceContextId() {
        return sourceContextId;
    }

    public String getSourceRouteId() {
        return sourceRouteId;
    }

    public String getBreadCrumbId() {
        return breadCrumbId;
    }

    public Boolean getRedelivered() {
        return redelivered;
    }

    public String getCurrentRouteId() {
        return currentRouteId;
    }

    public boolean isCreatedEvent() {
        return event instanceof ExchangeCreatedEvent;
    }

    public boolean isCompletedEvent() {
        return event instanceof ExchangeCompletedEvent;
    }

    public boolean isSendingEvent() {
        return event instanceof ExchangeSendingEvent;
    }

    public boolean isSentEvent() {
        return event instanceof ExchangeSentEvent;
    }

    public boolean isFailureEvent() {
        return event instanceof ExchangeFailedEvent;
    }

    public boolean isRedeliveryEvent() {
        return event instanceof ExchangeRedeliveryEvent;
    }

    public EventType getEventType() {
        return eventType;
    }
}
