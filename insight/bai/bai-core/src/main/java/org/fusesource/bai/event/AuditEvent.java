package org.fusesource.bai.event;

import java.util.Date;

import org.apache.camel.Exchange;
import org.apache.camel.management.event.AbstractExchangeEvent;

/**
 * DTO that represents an AuditEvent
 * @author raul
 *
 */
public class AuditEvent extends AbstractExchangeEvent {
    private static final long serialVersionUID = 6818757465057171170L;
    
    public AuditEvent(Exchange source, AbstractExchangeEvent realEvent) {
        super(source);
        this.event = realEvent;
        this.timestamp = new Date();
    }
    
    public AbstractExchangeEvent event;
    public Date timestamp;
    public String endpointURI;
    public Exception exception;
    public String sourceContextId;
    public String sourceRouteId;
    public String breadCrumbId;
    
}
