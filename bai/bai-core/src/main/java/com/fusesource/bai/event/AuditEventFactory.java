package com.fusesource.bai.event;

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
        ae.sourceRouteId = (e.getUnitOfWork() == null || e.getUnitOfWork().getRouteContext() == null) ? e.getFromRouteId() : e.getUnitOfWork().getRouteContext().getRoute().getId();
        ae.breadCrumbId = e.getIn().getHeader(Exchange.BREADCRUMB_ID, String.class);
        if (ae.breadCrumbId == null && e.hasOut()) {
            ae.breadCrumbId = e.getOut().getHeader(Exchange.BREADCRUMB_ID, String.class);
        }
    }

}
