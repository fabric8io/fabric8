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
import org.apache.camel.management.PublishEventNotifier;
import org.apache.camel.management.event.AbstractExchangeEvent;
import org.apache.camel.management.event.ExchangeCreatedEvent;
import org.apache.camel.management.event.ExchangeSendingEvent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.EventObject;

/**
 */
public abstract class AuditEventNotifierSupport extends PublishEventNotifier {
    protected CamelContext camelContext;

    @PostConstruct
    @Override
    public void start() throws Exception {
        super.start();
    }

    @PreDestroy
    @Override
    public void stop() throws Exception {
        super.stop();
    }

    @Override
    public boolean isEnabled(EventObject event) {
        EventObject coreEvent = event;
        AbstractExchangeEvent exchangeEvent = null;
        if (event instanceof AuditEvent) {
            AuditEvent auditEvent = (AuditEvent) event;
            coreEvent = auditEvent.getEvent();
        }
        if (event instanceof AbstractExchangeEvent) {
            exchangeEvent = (AbstractExchangeEvent) event;
        }
        return isEnabledFor(coreEvent, exchangeEvent);
    }


    /**
     * Add a unique dispatchId property to the original Exchange, which will come back to us later.
     * Camel does not correlate the individual sends/dispatches of the same exchange to the same endpoint, e.g.
     * Exchange X sent to http://localhost:8080, again sent to http://localhost:8080...
     * When both happen in parallel, and are marked in_progress in BAI, when the Sent or Completed
     * events arrive, BAI won't know which record to update (ambiguity)
     * So to overcome this situation, we enrich the Exchange with a DispatchID only when Created or Sending
     */
    @Override
    public void notify(EventObject event) throws Exception {
        AuditEvent auditEvent = null;
        AbstractExchangeEvent ae = null;
        if (event instanceof AuditEvent) {
            auditEvent = (AuditEvent) event;
            ae = auditEvent.getEvent();
        } else if (event instanceof AbstractExchangeEvent) {
            ae = (AbstractExchangeEvent) event;
            auditEvent = createAuditEvent(ae);
        }

        if (ae == null || auditEvent == null) {
            log.debug("Ignoring events like " + event + " as its neither a AbstractExchangeEvent or AuditEvent");
            return;
        }
        if (event instanceof ExchangeSendingEvent || event instanceof ExchangeCreatedEvent) {
            ae.getExchange().setProperty(AuditConstants.DISPATCH_ID, ae.getExchange().getContext().getUuidGenerator().generateUuid());
        }

        // only notify when we are started
        if (!isStarted()) {
            log.debug("Cannot publish event as notifier is not started: {}", event);
            return;
        }

        // only notify when camel context is running
        if (!camelContext.getStatus().isStarted()) {
            log.debug("Cannot publish event as CamelContext is not started: {}", event);
            return;
        }

        processAuditEvent(auditEvent);
    }

    // Properties
    //-------------------------------------------------------------------------

    public CamelContext getCamelContext() {
        return camelContext;
    }

    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }


    // Implementation methods
    //-------------------------------------------------------------------------


    /**
     * Factory method to create a new {@link AuditEvent} in case a sub class wants to create a different derived kind of event
     */
    protected AuditEvent createAuditEvent(AbstractExchangeEvent ae) {
        return new AuditEvent(ae.getExchange(), ae);
    }


    protected abstract void processAuditEvent(AuditEvent auditEvent) throws Exception;


    protected abstract boolean isEnabledFor(EventObject coreEvent, AbstractExchangeEvent exchangeEvent);

}
