package org.fusesource.insight.activemq.audit;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.broker.ProducerBrokerExchange;
import org.apache.activemq.broker.region.MessageReference;
import org.apache.activemq.broker.region.Subscription;
import org.apache.activemq.command.Message;
import org.fusesource.insight.activemq.base.SwichtableBrokerPlugin;
import org.fusesource.insight.storage.StorageService;
import org.mvel2.ParserContext;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class Auditor extends SwichtableBrokerPlugin implements ManagedService, AuditorMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(Auditor.class);

    private StorageService storage;
    private String type = "activemq";

    private Dictionary<String, ?> properties;
    private ParserContext context;
    private Map<String, CompiledTemplate> templates = new ConcurrentHashMap<String, CompiledTemplate>();
    private Map<URL, String> sources = new ConcurrentHashMap<URL, String>();
    private URL defaultTemplateUrl = getClass().getResource("default.mvel");

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
    public void send(ProducerBrokerExchange producerExchange, Message messageSend) throws Exception {
        if (isEnabled(messageSend)) {
            String json = toJson("Sent", messageSend);
            storage.store(type, System.currentTimeMillis(), json);
        }
        super.send(producerExchange, messageSend);
    }

    @Override
    public void messageConsumed(ConnectionContext context, MessageReference messageReference) {
        if (isEnabled(messageReference)) {
            String json = toJson("Consumed", messageReference);
            storage.store(type, System.currentTimeMillis(), json);
        }
        super.messageConsumed(context, messageReference);
    }

    @Override
    public void messageDelivered(ConnectionContext context, MessageReference messageReference) {
        if (isEnabled(messageReference)) {
            String json = toJson("Delivered", messageReference);
            storage.store(type, System.currentTimeMillis(), json);
        }
        super.messageDelivered(context, messageReference);
    }

    @Override
    public void messageDiscarded(ConnectionContext context, Subscription sub, MessageReference messageReference) {
        if (isEnabled(messageReference)) {
            String json = toJson("Discarded", messageReference);
            storage.store(type, System.currentTimeMillis(), json);
        }
        super.messageDiscarded(context, sub, messageReference);
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        this.properties = properties;
    }

    protected String toJson(String eventType, MessageReference messageReference) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(Auditor.class.getClassLoader());
            CompiledTemplate template = getTemplate(eventType, messageReference);
            Map<String, Object> vars = new HashMap<String, Object>();
            vars.put("event", eventType);
            vars.put("host", System.getProperty("karaf.name"));
            vars.put("timestamp", new Date());
            vars.put("message", messageReference.getMessage());
            vars.put("messageReference", messageReference);

            return TemplateRuntime.execute(template, context, vars).toString();
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    private CompiledTemplate getTemplate(String event, MessageReference message) {
        String source = getTemplateSource(event, message);
        CompiledTemplate template = templates.get(source);
        if (template == null) {
            template = TemplateCompiler.compileTemplate(source, context);
            templates.put(source, template);
        }
        return template;
    }

    private String getTemplateSource(String event, MessageReference message) {
        String source = null;
        URL url = getTemplateUrl(event, message);
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

    private URL getTemplateUrl(String event, MessageReference message) {
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

}
