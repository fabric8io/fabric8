/**
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
package org.fusesource.insight.camel.audit;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.api.management.ManagedResource;
import org.apache.camel.management.event.AbstractExchangeEvent;
import org.apache.camel.management.event.ExchangeSendingEvent;
import org.apache.camel.spi.EventNotifier;
import org.fusesource.insight.camel.base.SwitchableContainerStrategy;
import org.fusesource.insight.storage.StorageService;
import org.mvel2.ParserContext;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Dictionary;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
@ManagedResource(description = "Auditor")
public class Auditor extends SwitchableContainerStrategy implements EventNotifier, ManagedService, AuditorMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(Auditor.class);

    private StorageService storage;
    private String type = "camel";

    private boolean ignoreExchangeCreatedEvent;
    private boolean ignoreExchangeCompletedEvent;
    private boolean ignoreExchangeFailedEvents;
    private boolean ignoreExchangeRedeliveryEvents;
    private boolean ignoreExchangeSendingEvents;
    private boolean ignoreExchangeSentEvents;

    private Dictionary<String, ?> properties;
    private ParserContext context;
    private Map<String, CompiledTemplate> templates = new ConcurrentHashMap<String, CompiledTemplate>();
    private Map<URL, String> sources = new ConcurrentHashMap<URL, String>();
    private URL defaultTemplateUrl = getClass().getResource("default.mvel");

    public Auditor() {
        this(null);
    }

    public Auditor(StorageService storage) {
        super(false);
        this.storage = storage;
        context = new ParserContext();
        try {
            context.addImport("toJson", ScriptUtils.class.getMethod("toJson", Object.class));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unable to find method toJson", e);
        }
    }

    @Override
    public void manage(CamelContext context) throws Exception {
        context.getManagementStrategy().addEventNotifier(this);
    }

    public StorageService getStorage() {
        return storage;
    }

    public void setStorage(StorageService storage) {
        this.storage = storage;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        this.properties = properties;
    }

    @Override
    public void notify(EventObject eventObject) throws Exception {
        if (eventObject instanceof AbstractExchangeEvent) {
            AbstractExchangeEvent aee = (AbstractExchangeEvent) eventObject;
            if (isEnabled(aee.getExchange())) {
                if (aee instanceof ExchangeSendingEvent) {
                    aee.getExchange().getIn().setHeader("AuditCallId", aee.getExchange().getContext().getUuidGenerator().generateUuid());
                }
                String json = toJson(aee);
                storage.store(type, System.currentTimeMillis(), json);
            }
        }
    }

    protected String toJson(AbstractExchangeEvent event) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(Auditor.class.getClassLoader());
            String eventType = event.getClass().getSimpleName();
            eventType = eventType.substring("Exchange".length());
            eventType = eventType.substring(0, eventType.length() - "Event".length());

            CompiledTemplate template = getTemplate(eventType, event.getExchange());
            Map<String, Object> vars = new HashMap<String, Object>();
            vars.put("event", eventType);
            vars.put("host", System.getProperty("karaf.name"));
            vars.put("timestamp", new Date());
            vars.put("exchange", event.getExchange());

            return TemplateRuntime.execute(template, context, vars).toString();
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    private CompiledTemplate getTemplate(String event, Exchange exchange) {
        String source = getTemplateSource(event, exchange);
        CompiledTemplate template = templates.get(source);
        if (template == null) {
            template = TemplateCompiler.compileTemplate(source, context);
            templates.put(source, template);
        }
        return template;
    }

    private String getTemplateSource(String event, Exchange exchange) {
        String source = null;
        URL url = getTemplateUrl(event, exchange);
        if (url != null) {
            try {
                source = loadSource(url);
            } catch (IOException e) {
                LOGGER.warn("Unable to load mvel template " + url, e);
            }
        }
        if (source == null) {
            try {
                source = loadSource(defaultTemplateUrl);
            } catch (IOException e) {
                throw new IllegalStateException("Default template could not be loaded", e);
            }
        }
        return source;
    }

    private URL getTemplateUrl(String event, Exchange exchange) {
        return null;
    }

    private String loadSource(URL url) throws IOException {
        String source = sources.get(url);
        if (source == null) {
            source = IoUtils.loadFully(url);
            sources.put(url, source);
        }
        return source;
    }

    @Override
    public boolean isEnabled(EventObject eventObject) {
        return true;
    }

    public boolean isIgnoreCamelContextEvents() {
        return true;
    }

    public void setIgnoreCamelContextEvents(boolean ignoreCamelContextEvents) {
    }

    public boolean isIgnoreRouteEvents() {
        return true;
    }

    public void setIgnoreRouteEvents(boolean ignoreRouteEvents) {
    }

    public boolean isIgnoreServiceEvents() {
        return true;
    }

    public void setIgnoreServiceEvents(boolean ignoreServiceEvents) {
    }

    public boolean isIgnoreExchangeEvents() {
        return false;
    }

    public void setIgnoreExchangeEvents(boolean ignoreExchangeEvents) {
    }

    public boolean isIgnoreExchangeCreatedEvent() {
        return ignoreExchangeCreatedEvent;
    }

    public void setIgnoreExchangeCreatedEvent(boolean ignoreExchangeCreatedEvent) {
        this.ignoreExchangeCreatedEvent = ignoreExchangeCreatedEvent;
    }

    public boolean isIgnoreExchangeCompletedEvent() {
        return ignoreExchangeCompletedEvent;
    }

    public void setIgnoreExchangeCompletedEvent(boolean ignoreExchangeCompletedEvent) {
        this.ignoreExchangeCompletedEvent = ignoreExchangeCompletedEvent;
    }

    public boolean isIgnoreExchangeFailedEvents() {
        return ignoreExchangeFailedEvents;
    }

    public void setIgnoreExchangeFailedEvents(boolean ignoreExchangeFailedEvents) {
        this.ignoreExchangeFailedEvents = ignoreExchangeFailedEvents;
    }

    public boolean isIgnoreExchangeRedeliveryEvents() {
        return ignoreExchangeRedeliveryEvents;
    }

    public void setIgnoreExchangeRedeliveryEvents(boolean ignoreExchangeRedeliveryEvents) {
        this.ignoreExchangeRedeliveryEvents = ignoreExchangeRedeliveryEvents;
    }

    public boolean isIgnoreExchangeSendingEvents() {
        return ignoreExchangeSendingEvents;
    }

    public void setIgnoreExchangeSendingEvents(boolean ignoreExchangeSendingEvents) {
        this.ignoreExchangeSendingEvents = ignoreExchangeSendingEvents;
    }

    public boolean isIgnoreExchangeSentEvents() {
        return ignoreExchangeSentEvents;
    }

    public void setIgnoreExchangeSentEvents(boolean ignoreExchangeSentEvents) {
        this.ignoreExchangeSentEvents = ignoreExchangeSentEvents;
    }
}
