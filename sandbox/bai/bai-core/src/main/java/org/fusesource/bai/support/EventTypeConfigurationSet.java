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
package org.fusesource.bai.support;

import org.apache.camel.Predicate;
import org.apache.camel.spi.Language;
import org.fusesource.bai.EventTypeConfiguration;
import org.fusesource.bai.agent.CamelContextService;
import org.fusesource.bai.agent.filters.CamelContextFilters;
import org.fusesource.bai.config.EventType;
import org.fusesource.common.util.Filter;
import org.fusesource.common.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Represents a set of configurations for each event type
 */
public class EventTypeConfigurationSet {
    private static final transient Logger LOG = LoggerFactory.getLogger(EventTypeConfigurationSet.class);

    public static final String EVENT_FLAG = "event.";
    public static final String EXCHANGE_FILTER = "exchange.filter.";
    public static final String ENDPOINT_REGEX = "endpoint.regex.";

    private EventTypeConfiguration createdConfig = new EventTypeConfiguration();
    private EventTypeConfiguration completedConfig = new EventTypeConfiguration();
    private EventTypeConfiguration sendingConfig = new EventTypeConfiguration();
    private EventTypeConfiguration sentConfig = new EventTypeConfiguration();
    private EventTypeConfiguration failureConfig = new EventTypeConfiguration();
    private EventTypeConfiguration failureHandledConfig = new EventTypeConfiguration();
    private EventTypeConfiguration redeliveryConfig = new EventTypeConfiguration();
    private EventTypeConfiguration allConfig = new CompositeEventTypeConfiguration(Arrays.asList(
            createdConfig, completedConfig, sendingConfig, sentConfig, failureConfig, failureHandledConfig, redeliveryConfig));

    @Override
    public String toString() {
        return "EventTypeConfigurationSet(" +
                "\n  createdConfig: " + createdConfig +
                "\n  completedConfig: " + completedConfig +
                "\n  sendingConfig: " + sendingConfig +
                "\n  sentConfig: " + sentConfig +
                ")";
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
            case ALL:
                return allConfig;
            default:
                return null;
        }
    }

    /**
     * Configures one of the event types using a key/value from a configuration file
     */
    public void configureValue(CamelContextService camelContextService, String key, String value) {
        Pair<EventTypeConfiguration, String> configAndRemaining = parseEventType(key, EVENT_FLAG);
        if (configAndRemaining != null) {
            // event.eventType.camelContextPattern = true/false
            if (matchesCamelContextService(camelContextService, configAndRemaining.getSecond())) {
                configAndRemaining.getFirst().configureEventFlag(value);
            }
        } else {
            configAndRemaining = parseEventType(key, EXCHANGE_FILTER);
            if (configAndRemaining != null) {
                // exchange.filter.eventType.language.camelContextPattern = language
                Pair<Predicate, String> languageAndRemaining = parsePredicateAndRemaining(camelContextService, configAndRemaining.getSecond(), value);
                if (languageAndRemaining != null) {
                    if (matchesCamelContextService(camelContextService, languageAndRemaining.getSecond())) {
                        configAndRemaining.getFirst().addFilter(languageAndRemaining.getFirst());
                    }
                }
            } else {
                // endpoint.regex.eventType.camelContextPattern = regex
                configAndRemaining = parseEventType(key, ENDPOINT_REGEX);
                if (configAndRemaining != null) {
                    if (matchesCamelContextService(camelContextService, configAndRemaining.getSecond())) {
                        configAndRemaining.getFirst().addEndpointIncludeRegexp(value);
                    }
                }
            }
        }
    }

    /**
     * Parses the event type if the key matches the given prefix which is stripped off and
     * the configuration and remaining text is returned
     */
    protected Pair<EventTypeConfiguration, String> parseEventType(String key, String prefix) {
        if (key.startsWith(prefix)) {
            String next = key.substring(prefix.length());
            // now lets parse the next eventType
            int idx = next.indexOf('.');
            if (idx > 0) {
                String eventText = next.substring(0, idx);
                String remaining = next.substring(idx + 1);
                EventType eventType = EventType.simpleNames.get(eventText);
                if (eventType == null) {
                    LOG.warn("Invalid EventType: " + eventText + " when parsing " + key);
                } else {
                    EventTypeConfiguration config = getConfig(eventType);
                    if (config == null) {
                        LOG.warn("Could not find an EventTypeConfiguration for eventType: " + eventType);
                    } else {
                        return new Pair<EventTypeConfiguration, String>(config, remaining);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Parses a string of the form "languageName.remaining = expression" returning the Predicate and the remaining text
     */
    protected Pair<Predicate, String> parsePredicateAndRemaining(CamelContextService camelContextService, String key, String expression) {
        // lets assume no dots in language names
        int idx = key.indexOf('.');
        if (idx > 0) {
            String languageName = key.substring(0, idx);
            String remaining = key.substring(idx + 1);
            Language language = camelContextService.getCamelContext().resolveLanguage(languageName);
            if (languageName == null) {
                LOG.error("Could not resolve language '" + languageName + "' with expression '" + expression + "'");
            } else {
                try {
                    Predicate predicate = language.createPredicate(expression);
                    if (predicate == null) {
                        LOG.error("Could not create predicate for language " + language + " and expression '" + expression + "'");
                    } else {
                        return new Pair<Predicate, String>(predicate, remaining);
                    }
                } catch (Exception e) {
                    LOG.error("Failed to parse " + language + " expression '" + expression + "'. Reason: " + e);
                }
            }
        }
        return null;
    }

    /**
     * Returns true if the camel context pattern matches the given camelContextService
     */
    protected boolean matchesCamelContextService(CamelContextService camelContextService, String camelContextPattern) {
        Filter<CamelContextService> filter = CamelContextFilters.createCamelContextFilter(camelContextPattern);
        return filter.matches(camelContextService);
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

}
