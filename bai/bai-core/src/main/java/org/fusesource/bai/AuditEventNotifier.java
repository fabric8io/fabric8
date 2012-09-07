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

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Producer;
import org.apache.camel.management.event.AbstractExchangeEvent;
import org.apache.camel.management.event.ExchangeCompletedEvent;
import org.apache.camel.management.event.ExchangeCreatedEvent;
import org.apache.camel.management.event.ExchangeFailedEvent;
import org.apache.camel.management.event.ExchangeRedeliveryEvent;
import org.apache.camel.management.event.ExchangeSendingEvent;
import org.apache.camel.management.event.ExchangeSentEvent;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.ServiceHelper;
import org.apache.camel.util.URISupport;
import org.fusesource.bai.config.EventType;
import org.fusesource.bai.support.EventTypeConfigurationSet;

import java.util.EventObject;
import java.util.List;

/**
 * A notifier of {@link AuditEvent} objects
 * <p/>
 * { _id: <breadcrumbId>,
 * exchanges: [
 * { timestamp: <timestamp>,
 * endpointUri: <uri>,
 * in: <inMessage>,
 * out: <outMessage>
 * },
 * { timestamp: <timestamp>,
 * endpointUri: <uri>,
 * in: <inMessage>,
 * out: <outMessage>
 * },
 * { timestamp: <timestamp>,
 * endpointUri: <uri>,
 * in: <inMessage>,
 * out: <outMessage>
 * }
 * ],
 * <p/>
 * failures: [
 * { timestamp: <timestamp>,
 * error: <exception and message>
 * }
 * ],
 * <p/>
 * redeliveries:
 * { endpoint: [timestamps],
 * endpoint: [timestamps]
 * }
 * ],
 * <p/>
 * <p/>
 * }
 *
 * @author raul
 */
public class AuditEventNotifier extends AuditEventNotifierSupport {
    private EventTypeConfiguration createdConfig = new EventTypeConfiguration();
    private EventTypeConfiguration completedConfig = new EventTypeConfiguration();
    private EventTypeConfiguration sendingConfig = new EventTypeConfiguration();
    private EventTypeConfiguration sentConfig = new EventTypeConfiguration();
    private EventTypeConfiguration failureConfig = new EventTypeConfiguration();
    private EventTypeConfiguration failureHandledConfig = new EventTypeConfiguration();
    private EventTypeConfiguration redeliveryConfig = new EventTypeConfiguration();
    protected Endpoint endpoint;
    protected String endpointUri;
    private Producer producer;

    public AuditEventNotifier() {
        setIgnoreCamelContextEvents(true);
        setIgnoreRouteEvents(true);
        setIgnoreServiceEvents(true);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + (endpoint != null ? endpoint : URISupport
                .sanitizeUri(endpointUri)) + "]";
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
    protected boolean isEnabledFor(EventObject coreEvent, AbstractExchangeEvent exchangeEvent) {
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
        String uri = AuditEvent.endpointUri(coreEvent);
        if (config == null) return false;
        return config.matchesEvent(uri, exchangeEvent);
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
        return completedConfig.getExchangeFilters();
    }

    public void setCompletedFilters(List<Predicate> filters) {
        completedConfig.setExchangeFilters(filters);
    }

    public List<String> getCompletedIncludeRegexList() {
        return completedConfig.getEndpointIncludeRegexps();
    }

    public void setCompletedIncludeRegexList(List<String> includeRegexList) {
        completedConfig.setEndpointIncludeRegexps(includeRegexList);
    }


    // created
    public boolean isIncludeCreated() {
        return createdConfig.isInclude();
    }

    public void setIncludeCreated(boolean include) {
        createdConfig.setInclude(include);
    }

    public List<Predicate> getCreatedFilters() {
        return createdConfig.getExchangeFilters();
    }

    public void setCreatedFilters(List<Predicate> filters) {
        createdConfig.setExchangeFilters(filters);
    }

    public List<String> getCreatedIncludeRegexList() {
        return createdConfig.getEndpointIncludeRegexps();
    }

    public void setCreatedIncludeRegexList(List<String> includeRegexList) {
        createdConfig.setEndpointIncludeRegexps(includeRegexList);
    }


    // sending
    public boolean isIncludeSending() {
        return sendingConfig.isInclude();
    }

    public void setIncludeSending(boolean include) {
        sendingConfig.setInclude(include);
    }

    public List<Predicate> getSendingFilters() {
        return sendingConfig.getExchangeFilters();
    }

    public void setSendingFilters(List<Predicate> filters) {
        sendingConfig.setExchangeFilters(filters);
    }

    public List<String> getSendingcludeRegexList() {
        return sendingConfig.getEndpointIncludeRegexps();
    }

    public void setSendingIncludeRegexList(List<String> includeRegexList) {
        sendingConfig.setEndpointIncludeRegexps(includeRegexList);
    }


    // sent
    public boolean isIncludeSent() {
        return sentConfig.isInclude();
    }

    public void setIncludeSent(boolean include) {
        sentConfig.setInclude(include);
    }

    public List<Predicate> getSentFilters() {
        return sentConfig.getExchangeFilters();
    }

    public void setSentFilters(List<Predicate> filters) {
        sentConfig.setExchangeFilters(filters);
    }

    public List<String> getSentIncludeRegexList() {
        return sentConfig.getEndpointIncludeRegexps();
    }

    public void setSentIncludeRegexList(List<String> includeRegexList) {
        sentConfig.setEndpointIncludeRegexps(includeRegexList);
    }


    // failure
    public boolean isIncludeFailure() {
        return failureConfig.isInclude();
    }

    public void setIncludeFailure(boolean include) {
        failureConfig.setInclude(include);
    }

    public List<Predicate> getFailureFilters() {
        return failureConfig.getExchangeFilters();
    }

    public void setFailureFilters(List<Predicate> filters) {
        failureConfig.setExchangeFilters(filters);
    }

    public List<String> getFailureIncludeRegexList() {
        return failureConfig.getEndpointIncludeRegexps();
    }

    public void setFailureIncludeRegexList(List<String> includeRegexList) {
        failureConfig.setEndpointIncludeRegexps(includeRegexList);
    }


    // failureHandled
    public boolean isIncludeFailureHandled() {
        return failureHandledConfig.isInclude();
    }

    public void setIncludeFailureHandled(boolean include) {
        failureHandledConfig.setInclude(include);
    }

    public List<Predicate> getFailureHandledFilters() {
        return failureHandledConfig.getExchangeFilters();
    }

    public void setFailureHandledFilters(List<Predicate> filters) {
        failureHandledConfig.setExchangeFilters(filters);
    }

    public List<String> getFailureHandledIncludeRegexList() {
        return failureHandledConfig.getEndpointIncludeRegexps();
    }

    public void setFailureHandledIncludeRegexList(List<String> includeRegexList) {
        failureHandledConfig.setEndpointIncludeRegexps(includeRegexList);
    }


    // redelivery
    public boolean isIncludeRedelivery() {
        return redeliveryConfig.isInclude();
    }

    public void setIncludeRedelivery(boolean include) {
        redeliveryConfig.setInclude(include);
    }

    public List<Predicate> getRedeliveryFilters() {
        return redeliveryConfig.getExchangeFilters();
    }

    public void setRedeliveryFilters(List<Predicate> filters) {
        redeliveryConfig.setExchangeFilters(filters);
    }

    public List<String> getRedeliveryIncludeRegexList() {
        return redeliveryConfig.getEndpointIncludeRegexps();
    }

    public void setRedeliveryIncludeRegexList(List<String> includeRegexList) {
        redeliveryConfig.setEndpointIncludeRegexps(includeRegexList);
    }


    public Producer getProducer() {
        return producer;
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

    protected void processAuditEvent(AuditEvent auditEvent) throws Exception {
        Exchange exchange = producer.createExchange();
        exchange.getIn().setBody(createPayload(auditEvent));

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

    @Override
    protected void doStop() throws Exception {
        ServiceHelper.stopService(producer);
    }

    /**
     * Creates the audit payload from the audit event
     */
    protected Object createPayload(AuditEvent auditEvent) {
        return auditEvent;
    }
}
