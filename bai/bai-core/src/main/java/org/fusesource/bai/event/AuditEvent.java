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

package org.fusesource.bai.event;

import java.util.Date;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.management.event.*;

/**
 * DTO that represents an AuditEvent
 * @author raul
 *
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
        this.currentRouteId = (source.getUnitOfWork() == null || source.getUnitOfWork().getRouteContext() == null) ? null : source.getUnitOfWork().getRouteContext().getRoute().getId();
        this.breadCrumbId = source.getIn().getHeader(Exchange.BREADCRUMB_ID, String.class);
        if (this.breadCrumbId == null && source.hasOut()) {
            this.breadCrumbId = source.getOut().getHeader(Exchange.BREADCRUMB_ID, String.class);
        }
        this.exception = source.getException() == null ? source.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class) : source.getException();
        this.redelivered = source.getProperty(Exchange.REDELIVERED, boolean.class);
        
    }
    
    public AbstractExchangeEvent event;
    public Date timestamp;
    public String endpointURI;
    public Exception exception;
    public String sourceContextId;
    public String sourceRouteId;
    public String breadCrumbId;
    public Boolean redelivered;
    public String currentRouteId;
    
}
