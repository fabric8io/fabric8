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

import org.apache.camel.*;
import org.apache.camel.management.PublishEventNotifier;
import org.apache.camel.management.event.*;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.ServiceHelper;
import org.apache.camel.util.URISupport;

import java.util.Arrays;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A notifier of {@link AuditEvent} objects
 *
 * { _id: <breadcrumbId>,
     exchanges: [
     { timestamp: <timestamp>,
        endpointUri: <uri>,
        in: <inMessage>,
        out: <outMessage>
     },
     { timestamp: <timestamp>,
        endpointUri: <uri>,
        in: <inMessage>,
        out: <outMessage>
     },
     { timestamp: <timestamp>,
        endpointUri: <uri>,
        in: <inMessage>,
        out: <outMessage>
     }
   ],
   
   failures: [
     { timestamp: <timestamp>,
       error: <exception and message>
     }
   ],
   
   redeliveries: 
     { endpoint: [timestamps],
       endpoint: [timestamps]
     }
   ],
   
   
}
 * @author raul
 *
 */
public class AuditEventNotifier extends PublishEventNotifier {
    private EventTypeConfiguration createdConfig = new EventTypeConfiguration();
    private EventTypeConfiguration completedConfig = new EventTypeConfiguration();
    private EventTypeConfiguration sendingConfig = new EventTypeConfiguration();
    private EventTypeConfiguration sentConfig = new EventTypeConfiguration();
    private EventTypeConfiguration failureConfig = new EventTypeConfiguration();
    private EventTypeConfiguration failureHandledConfig = new EventTypeConfiguration();
    private EventTypeConfiguration redeliveryConfig = new EventTypeConfiguration();

    private CamelContext camelContext;
    private Endpoint endpoint;
    private String endpointUri;
    private Producer producer;

    public AuditEventNotifier() {
		setIgnoreCamelContextEvents(true);
		setIgnoreRouteEvents(true);
		setIgnoreServiceEvents(true);
	}

    @Override
    public String toString() {
        return "PublishEventNotifier[" + (endpoint != null ? endpoint : URISupport.sanitizeUri(endpointUri)) + "]";
    }

    /**
     * Updates this notifier with the given configuration set
     */
    public void configure(EventTypeConfigurationSet configs) {
        this.createdConfig = configs.getCreatedConfig();
        this.completedConfig = configs.getCompletedConfig();
        this.sendingConfig = configs.getSendingConfig();
        this.sentConfig = configs.getSentConfig();
        this.failureConfig = configs.getFailureConfig();
        this.failureHandledConfig = configs.getFailureHandledConfig();
        this.redeliveryConfig = configs.getRedeliveryConfig();
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
        EventTypeConfiguration config = null;
        if (coreEvent instanceof ExchangeCreatedEvent) {
            config = getCreatedConfig();
        } else if (coreEvent instanceof ExchangeCompletedEvent) {
            config = getCompletedConfig();
        } else if (coreEvent instanceof ExchangeSendingEvent) {
            config = getSendingConfig();
        } else if (coreEvent instanceof ExchangeSentEvent) {
            config = getSentConfig();
        } else if (coreEvent instanceof ExchangeRedeliveryEvent) {
            config = getRedeliveryConfig();
        }
        // logic if it's a failure is different; we compare against Exception
        else if (coreEvent instanceof ExchangeFailedEvent) {
            ExchangeFailedEvent failedEvent = (ExchangeFailedEvent) coreEvent;
            String exceptionClassName = failedEvent.getExchange().getException().getClass().getCanonicalName();
            config = getFailureConfig();

            // TODO allow filter by exception class name!
            // return testRegexps(exceptionClassName, failureRegex, filter, exchangeEvent);
        }
        String uri = endpointUri(event);
        if (config == null) return false;
        return testConfig(uri, config, exchangeEvent);
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

    private boolean testConfig(String uri, EventTypeConfiguration config, AbstractExchangeEvent exchangeEvent) {
        if (!config.isInclude()) {
            return false;
        }
        List<String> regexList = config.getIncludeRegexList();
        if (!regexList.isEmpty()) {
            if (endpointUri == null) {
                return false;
            }
            for (String regex : regexList) {
          			if (!uri.matches(regex)) {
                          return false;
          			}
          		}
        }
        Exchange exchange = exchangeEvent.getExchange();
        if (exchange == null) {
            return false;
        }
        List<Predicate> filters = config.getFilters();
        if (filters.isEmpty()) {
            return true;
        } else {
            for (Predicate filter : filters) {
                if (filter.matches(exchange)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Add a unique dispatchId property to the original Exchange, which will come back to us later.
     * Camel does not correlate the individual sends/dispatches of the same exchange to the same endpoint, e.g.
     * Exchange X sent to http://localhost:8080, again sent to http://localhost:8080... When both happen in parallel, and are marked in_progress in BAI, when the Sent or Completed
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

        Exchange exchange = producer.createExchange();
        exchange.getIn().setBody(auditEvent);

        // make sure we don't send out events for this as well
        // mark exchange as being published to event, to prevent creating new events
        // for this as well (causing a endless flood of events)
        exchange.setProperty(Exchange.NOTIFY_EVENT, Boolean.TRUE);
        try {
            producer.process(exchange);
        } finally {
            // and remove it when its done
            exchange.removeProperty(Exchange.NOTIFY_EVENT);
        }
    }

    /**
     * Factory method to create a new {@link AuditEvent} in case a sub class wants to create a different derived kind of event
     */
    protected AuditEvent createAuditEvent(AbstractExchangeEvent ae) {
        return new AuditEvent(ae.getExchange(), ae);
    }

    /**
	 * Substitute all arrays with CopyOnWriteArrayLists
	 */
	@Override
	protected void doStart() throws Exception {
		ObjectHelper.notNull(camelContext, "camelContext", this);
        if (endpoint == null && endpointUri == null) {
            throw new IllegalArgumentException("Either endpoint or endpointUri must be configured");
        }

        if (endpoint == null) {
            endpoint = camelContext.getEndpoint(endpointUri);
        }

        producer = endpoint.createProducer();
        ServiceHelper.startService(producer);

	}

	public CamelContext getCamelContext() {
        return camelContext;
    }

    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public String getEndpointUri() {
        return endpointUri;
    }

    public void setEndpointUri(String endpointUri) {
        this.endpointUri = endpointUri;
    }

    public EventTypeConfiguration getConfig(EventType eventType) {
        switch (eventType) {
           case CREATED:
                return getCreatedConfig();
            case COMPLETED:
                return getCompletedConfig();
            case SENDING:
                return getSendingConfig();
            case SENT:
                return getSentConfig();
            case FAILURE:
                return getFailureConfig();
            case FAILURE_HANDLED:
                return getFailureHandledConfig();
            case REDELIVERY:
                return getRedeliveryConfig();
            default:
                return null;
        }
    }

    public EventTypeConfiguration getCompletedConfig() {
        return completedConfig;
    }

    public void setCompletedConfig(EventTypeConfiguration completedConfig) {
        this.completedConfig = completedConfig;
    }

    public EventTypeConfiguration getCreatedConfig() {
        return createdConfig;
    }

    public void setCreatedConfig(EventTypeConfiguration createdConfig) {
        this.createdConfig = createdConfig;
    }

    public EventTypeConfiguration getFailureConfig() {
        return failureConfig;
    }

    public void setFailureConfig(EventTypeConfiguration failureConfig) {
        this.failureConfig = failureConfig;
    }

    public EventTypeConfiguration getFailureHandledConfig() {
        return failureHandledConfig;
    }

    public void setFailureHandledConfig(EventTypeConfiguration failureHandledConfig) {
        this.failureHandledConfig = failureHandledConfig;
    }

    public EventTypeConfiguration getRedeliveryConfig() {
        return redeliveryConfig;
    }

    public void setRedeliveryConfig(EventTypeConfiguration redeliveryConfig) {
        this.redeliveryConfig = redeliveryConfig;
    }

    public EventTypeConfiguration getSendingConfig() {
        return sendingConfig;
    }

    public void setSendingConfig(EventTypeConfiguration sendingConfig) {
        this.sendingConfig = sendingConfig;
    }

    public EventTypeConfiguration getSentConfig() {
        return sentConfig;
    }

    public void setSentConfig(EventTypeConfiguration sentConfig) {
        this.sentConfig = sentConfig;
    }

    @Override
    protected void doStop() throws Exception {
        ServiceHelper.stopService(producer);
    }

    // Delegate methods to make it easier to configure directly in spring
    //-------------------------------------------------------------------------

    // completed
    public boolean isIncludeCompleted() {
        return completedConfig.isInclude();
    }

    public void setIncludeCompleted(boolean include) {
        completedConfig.setInclude(include);
    }

    public List<Predicate> getCompletedFilters() {
        return completedConfig.getFilters();
    }

    public void setCompletedFilters(List<Predicate> filters) {
        completedConfig.setFilters(filters);
    }

    public List<String> getCompletedIncludeRegexList() {
        return completedConfig.getIncludeRegexList();
    }

    public void setCompletedIncludeRegexList(List<String> includeRegexList) {
        completedConfig.setIncludeRegexList(includeRegexList);
    }


    // created
    public boolean isIncludeCreated() {
        return createdConfig.isInclude();
    }

    public void setIncludeCreated(boolean include) {
        createdConfig.setInclude(include);
    }

    public List<Predicate> getCreatedFilters() {
        return createdConfig.getFilters();
    }

    public void setCreatedFilters(List<Predicate> filters) {
        createdConfig.setFilters(filters);
    }

    public List<String> getCreatedIncludeRegexList() {
        return createdConfig.getIncludeRegexList();
    }

    public void setCreatedIncludeRegexList(List<String> includeRegexList) {
        createdConfig.setIncludeRegexList(includeRegexList);
    }


    // sending
    public boolean isIncludeSending() {
        return sendingConfig.isInclude();
    }

    public void setIncludeSending(boolean include) {
        sendingConfig.setInclude(include);
    }

    public List<Predicate> getSendingFilters() {
        return sendingConfig.getFilters();
    }

    public void setSendingFilters(List<Predicate> filters) {
        sendingConfig.setFilters(filters);
    }

    public List<String> getSendingcludeRegexList() {
        return sendingConfig.getIncludeRegexList();
    }

    public void setSendingIncludeRegexList(List<String> includeRegexList) {
        sendingConfig.setIncludeRegexList(includeRegexList);
    }


    // sent
    public boolean isIncludeSent() {
        return sentConfig.isInclude();
    }

    public void setIncludeSent(boolean include) {
        sentConfig.setInclude(include);
    }

    public List<Predicate> getSentFilters() {
        return sentConfig.getFilters();
    }

    public void setSentFilters(List<Predicate> filters) {
        sentConfig.setFilters(filters);
    }

    public List<String> getSentIncludeRegexList() {
        return sentConfig.getIncludeRegexList();
    }

    public void setSentIncludeRegexList(List<String> includeRegexList) {
        sentConfig.setIncludeRegexList(includeRegexList);
    }


    // failure
    public boolean isIncludeFailure() {
        return failureConfig.isInclude();
    }

    public void setIncludeFailure(boolean include) {
        failureConfig.setInclude(include);
    }

    public List<Predicate> getFailureFilters() {
        return failureConfig.getFilters();
    }

    public void setFailureFilters(List<Predicate> filters) {
        failureConfig.setFilters(filters);
    }

    public List<String> getFailureIncludeRegexList() {
        return failureConfig.getIncludeRegexList();
    }

    public void setFailureIncludeRegexList(List<String> includeRegexList) {
        failureConfig.setIncludeRegexList(includeRegexList);
    }


    // failureHandled
    public boolean isIncludeFailureHandled() {
        return failureHandledConfig.isInclude();
    }

    public void setIncludeFailureHandled(boolean include) {
        failureHandledConfig.setInclude(include);
    }

    public List<Predicate> getFailureHandledFilters() {
        return failureHandledConfig.getFilters();
    }

    public void setFailureHandledFilters(List<Predicate> filters) {
        failureHandledConfig.setFilters(filters);
    }

    public List<String> getFailureHandledIncludeRegexList() {
        return failureHandledConfig.getIncludeRegexList();
    }

    public void setFailureHandledIncludeRegexList(List<String> includeRegexList) {
        failureHandledConfig.setIncludeRegexList(includeRegexList);
    }


    // redelivery
    public boolean isIncludeRedelivery() {
        return redeliveryConfig.isInclude();
    }

    public void setIncludeRedelivery(boolean include) {
        redeliveryConfig.setInclude(include);
    }

    public List<Predicate> getRedeliveryFilters() {
        return redeliveryConfig.getFilters();
    }

    public void setRedeliveryFilters(List<Predicate> filters) {
        redeliveryConfig.setFilters(filters);
    }

    public List<String> getRedeliveryIncludeRegexList() {
        return redeliveryConfig.getIncludeRegexList();
    }

    public void setRedeliveryIncludeRegexList(List<String> includeRegexList) {
        redeliveryConfig.setIncludeRegexList(includeRegexList);
    }



}
