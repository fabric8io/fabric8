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

import java.util.EventObject;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.management.event.AbstractExchangeEvent;
import org.apache.camel.management.event.DefaultEventFactory;

public class AuditEventFactory extends DefaultEventFactory {

    @Override
    public EventObject createExchangeCreatedEvent(Exchange exchange) {
        AuditEvent ae = new AuditEvent(exchange, (AbstractExchangeEvent) super.createExchangeCreatedEvent(exchange));
        ae.endpointURI = exchange.getFromEndpoint().getEndpointUri();
        enrichWithCommonMetadata(ae, exchange);
        return ae;
    }

    @Override
    public EventObject createExchangeCompletedEvent(Exchange exchange) {
        AuditEvent ae = new AuditEvent(exchange, (AbstractExchangeEvent) super.createExchangeCompletedEvent(exchange));
        ae.endpointURI = exchange.getFromEndpoint().getEndpointUri();
        enrichWithCommonMetadata(ae, exchange);
        return ae;
    }

    @Override
    public EventObject createExchangeFailedEvent(Exchange exchange) {
        AuditEvent ae = new AuditEvent(exchange, (AbstractExchangeEvent) super.createExchangeFailedEvent(exchange));
        ae.exception = exchange.getException();
        // failure endpoint
        ae.endpointURI = exchange.getProperty(Exchange.FAILURE_ENDPOINT, String.class);
        enrichWithCommonMetadata(ae, exchange);
        return ae;
    }

    // TODO: determine what to do with this
    @Override
    public EventObject createExchangeFailureHandledEvent(Exchange exchange, Processor failureHandler, boolean deadLetterChannel) {
        AuditEvent ae = new AuditEvent(exchange, (AbstractExchangeEvent) super.createExchangeFailureHandledEvent(exchange, failureHandler, deadLetterChannel));
        ae.endpointURI = exchange.getProperty(Exchange.FAILURE_ENDPOINT, String.class);
        enrichWithCommonMetadata(ae, exchange);
        return ae;    
    }

    @Override
    public EventObject createExchangeRedeliveryEvent(Exchange exchange, int attempt) {
        AuditEvent ae = new AuditEvent(exchange, (AbstractExchangeEvent) super.createExchangeRedeliveryEvent(exchange, attempt));
        ae.endpointURI = exchange.getProperty(Exchange.FAILURE_ENDPOINT, String.class);
        enrichWithCommonMetadata(ae, exchange);
        return ae;
    }

    @Override
    public EventObject createExchangeSendingEvent(Exchange exchange, Endpoint endpoint) {
        AuditEvent ae = new AuditEvent(exchange, (AbstractExchangeEvent) super.createExchangeSendingEvent(exchange, endpoint));
        ae.endpointURI = endpoint.getEndpointUri();
        enrichWithCommonMetadata(ae, exchange);
        return ae;
    }

    @Override
    public EventObject createExchangeSentEvent(Exchange exchange, Endpoint endpoint, long timeTaken) {
        AuditEvent ae = new AuditEvent(exchange, (AbstractExchangeEvent) super.createExchangeSentEvent(exchange, endpoint, timeTaken));        
        ae.endpointURI = endpoint.getEndpointUri();
        enrichWithCommonMetadata(ae, exchange);
        return ae;

    }
    
    public void enrichWithCommonMetadata(AuditEvent ae, Exchange e) {
        ae.sourceContextId = e.getContext().getName();
        // if the UoW exists, get the info from there as it's more accurate; if it doesn't exist, we have received an ExchangeCreated event so the info in the
        // exchange's fromRouteId will be correct anyway... so it's all good
        // all consumer endpoints create an exchange (ExchangeCreatedEvent) except for direct (even seda and VM create new Exchanges)
        // the sourceRouteId serves as correlation and in the MongoDB backend, it's part of the collection name
        // if the routeId changes WITHOUT an exchange being created in the new route, the event will never be written because
        // sourceRouteId: what route created the Exchange
        ae.sourceRouteId = e.getFromRouteId();
        // currentRouteId: what route the Exchange currently is in
        ae.currentRouteId = (e.getUnitOfWork() == null || e.getUnitOfWork().getRouteContext() == null) ? null : e.getUnitOfWork().getRouteContext().getRoute().getId();
        ae.breadCrumbId = e.getIn().getHeader(Exchange.BREADCRUMB_ID, String.class);
        if (ae.breadCrumbId == null && e.hasOut()) {
            ae.breadCrumbId = e.getOut().getHeader(Exchange.BREADCRUMB_ID, String.class);
        }
        ae.exception = e.getException() == null ? e.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class) : e.getException();
        ae.redelivered = e.getProperty(Exchange.REDELIVERED, boolean.class);
    }

}
