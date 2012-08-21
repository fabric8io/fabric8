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

import org.apache.camel.Exchange;
import org.apache.camel.management.event.*;

import java.util.Date;

/**
 * DTO that represents an AuditEvent
 *
 * @author raul
 */
public class AuditEvent extends AbstractExchangeEvent {
    private static final long serialVersionUID = 6818757465057171170L;
    
    public AuditEvent(Exchange source, AbstractExchangeEvent event) {
        super(source);
        this.event = event;
        this.timestamp = new Date();

        this.endpointURI = AuditEventNotifier.endpointUri(event);
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
        this.breadCrumbId = source.getIn().getHeader(Exchange.BREADCRUMB_ID, String.class);
        if (this.getBreadCrumbId() == null && source.hasOut()) {
            this.breadCrumbId = source.getOut().getHeader(Exchange.BREADCRUMB_ID, String.class);
        }
        this.exception = source.getException() == null ? source.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class) : source.getException();
        this.redelivered = source.getProperty(Exchange.REDELIVERED, boolean.class);
        
    }
    
    private AbstractExchangeEvent event;
    private Date timestamp;
    private String endpointURI;
    private Exception exception;
    private String sourceContextId;
    private String sourceRouteId;
    private String breadCrumbId;
    private Boolean redelivered;
    private String currentRouteId;

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public AbstractExchangeEvent getEvent() {
        return event;
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
}
