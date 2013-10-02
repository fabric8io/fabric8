package org.fusesource.insight.activemq.base;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.activemq.broker.BrokerPluginSupport;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.broker.region.MessageReference;
import org.apache.activemq.broker.region.Subscription;
import org.apache.activemq.command.Message;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class SwichtableBrokerPlugin extends BrokerPluginSupport implements SwichtableBrokerPluginMBean, InsightBrokerPlugin {

    private Map<String, String> properties;
    private final boolean defaultEnable;
    private final AtomicBoolean enabled;
    private final Map<String, Boolean> perDest = new ConcurrentHashMap<String, Boolean>();

    protected SwichtableBrokerPlugin() {
        this(true);
    }

    protected SwichtableBrokerPlugin(boolean defaultEnable) {
        this.defaultEnable = defaultEnable;
        this.enabled = new AtomicBoolean(defaultEnable);
    }
    public String getPlugin() {
        return getClass().getSimpleName();
    }

    public void update(Map<String, String> properties) {
        if (properties == null) {
            properties = new HashMap<String, String>();
        }
        if (this.properties == null || !this.properties.equals(properties)) {
            doUpdate(properties);
        }
        this.properties = properties;
    }

    private void doUpdate(Map<String, String> properties) {
        boolean enabled = defaultEnable;
        Map<String, Boolean> perDest = new HashMap<String, Boolean>();
        if (properties != null) {
            for (String key : properties.keySet()) {
                String val = properties.get(key);
                if ("enabled".equals(key)) {
                    enabled = Boolean.parseBoolean(val);
                } else if (key.startsWith("dest.")) {
                    perDest.put(key.substring("dest.".length()), Boolean.parseBoolean(val));
                }
            }
        }
        this.enabled.set(enabled);
        updateMap(this.perDest, perDest);
    }

    public Map<String, ?> getProperties() {
        return properties;
    }

    private void updateMap(Map<String, Boolean> oldMap, Map<String, Boolean> newMap) {
        oldMap.putAll(newMap);
        for (Iterator<String> it = oldMap.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            if (!newMap.containsKey(key)) {
                it.remove();
            }
        }
    }

    @Override
    public String getConfiguration() {
        Properties props = new Properties();
        if (properties != null) {
            props.putAll(properties);
        }
        StringWriter sw = new StringWriter();
        try {
            props.store(sw, null);
        } catch (IOException e) {
            return null;
        }
        return sw.toString();
    }

    @Override
    public void setConfiguration(String configuration) {
        try {
            Properties props = new Properties();
            props.load(new StringReader(configuration));

            String strategy = getPlugin();
            BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
            ServiceReference<ConfigurationAdmin> sr = bundleContext.getServiceReference(ConfigurationAdmin.class);
            ConfigurationAdmin ca = bundleContext.getService(sr);
            if (ca != null) {
                Configuration config = ca.getConfiguration(Activator.INSIGHT_ACTIVEMQ_PID);
                Dictionary<String, Object> dic = config.getProperties();
                if (dic == null) {
                    dic = new Hashtable<String, Object>();
                }
                Set<String> s = new HashSet<String>();
                for (Enumeration<String> keyEnum = dic.keys(); keyEnum.hasMoreElements();) {
                    String key = keyEnum.nextElement();
                    if (key.startsWith(strategy + ".")) {
                        s.add(key);
                    }
                }
                for (String key : s) {
                    dic.remove(key);
                }
                for (String key : props.stringPropertyNames()) {
                    dic.put(strategy + "." + key, props.getProperty(key));
                }
                config.update(dic);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reset() {
        enabled.set(defaultEnable);
        perDest.clear();
    }

    public void enable() {
        enabled.set(true);
    }

    public void disable() {
        enabled.set(false);
    }

    @Override
//    @ManagedAttribute(description = "Is service enabled")
    public boolean isEnabled() {
        return enabled.get();
    }

    @Override
//    @ManagedAttribute(description = "Is service enabled")
    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    @Override
    public void enableForDest(String dest) {
        perDest.put(dest, true);
    }

    @Override
    public void disableForDest(String dest) {
        perDest.put(dest, false);
    }

    @Override
    public void clearForDest(String dest) {
        perDest.remove(dest);
    }

    public boolean isEnabled(MessageReference message) {
        Boolean b = isDestEnabled(message);
        return (b == null) ? enabled.get() : b;
    }

    public Boolean isDestEnabled(MessageReference message) {
        String name = getDestName(message);
        if (name != null) {
            return perDest.get(name);
        } else {
            return true;
        }
    }

    private String getDestName(MessageReference message) {
        return message.getMessage().getDestination().toString();
    }

}
