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
package org.fusesource.insight.camel.base;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.api.management.ManagedAttribute;
import org.fusesource.insight.camel.commands.BaseCommand;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

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

/**
 *
 */
public abstract class SwitchableContainerStrategy implements ContainerStrategy, SwitchableContainerStrategyMBean {

    private Map<String, String> properties;
    private final boolean defaultEnable;
    private final AtomicBoolean enabled;
    private final Map<String, Boolean> perContext = new ConcurrentHashMap<String, Boolean>();
    private final Map<String, Boolean> perRoute = new ConcurrentHashMap<String, Boolean>();

    protected SwitchableContainerStrategy() {
        this(true);
    }

    public String getStrategy() {
        return getClass().getSimpleName().toLowerCase();
    }

    protected SwitchableContainerStrategy(boolean defaultEnable) {
        this.defaultEnable = defaultEnable;
        this.enabled = new AtomicBoolean(defaultEnable);
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
        Map<String, Boolean> perContext = new HashMap<String, Boolean>();
        Map<String, Boolean> perRoute = new HashMap<String, Boolean>();
        if (properties != null) {
            for (String key : properties.keySet()) {
                String val = properties.get(key);
                if ("enabled".equals(key)) {
                    enabled = Boolean.parseBoolean(val);
                } else if (key.startsWith("context.")) {
                    perContext.put(key.substring("context.".length()), Boolean.parseBoolean(val));
                } else if (key.startsWith("route.")) {
                    perRoute.put(key.substring("route.".length()), Boolean.parseBoolean(val));
                }
            }
        }
        this.enabled.set(enabled);
        updateMap(this.perContext, perContext);
        updateMap(this.perRoute, perRoute);
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

            String strategy = getStrategy();
            BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
            ServiceReference<ConfigurationAdmin> sr = bundleContext.getServiceReference(ConfigurationAdmin.class);
            ConfigurationAdmin ca = bundleContext.getService(sr);
            if (ca != null) {
                Configuration config = ca.getConfiguration(Activator.INSIGHT_CAMEL_PID);
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
        perContext.clear();
        perRoute.clear();
    }

    public void enable() {
        enabled.set(true);
    }

    public void disable() {
        enabled.set(false);
    }

    @Override
    @ManagedAttribute(description = "Is service enabled")
    public boolean isEnabled() {
        return enabled.get();
    }

    @Override
    @ManagedAttribute(description = "Is service enabled")
    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    @Override
    public void enableForContext(String context) {
        perContext.put(context, true);
    }

    @Override
    public void disableForContext(String context) {
        perContext.put(context, false);
    }

    @Override
    public void clearForContext(String context) {
        perContext.remove(context);
    }

    @Override
    public void enableForRoute(String route) {
        perRoute.put(route, true);
    }

    @Override
    public void disableForRoute(String route) {
        perRoute.put(route, false);
    }

    @Override
    public void clearForRoute(String route) {
        perRoute.remove(route);
    }

    public void enable(CamelContext context) {
        enableForContext(context.getName());
    }

    public void disable(CamelContext context) {
        disableForContext(context.getName());
    }

    public void clear(CamelContext context) {
        clearForContext(context.getName());
    }

    public void enable(Route route) {
        enableForRoute(route.getId());
    }

    public void disable(Route route) {
        disableForRoute(route.getId());
    }

    public void clear(Route route) {
        clearForRoute(route.getId());
    }

    public boolean isEnabled(Exchange exchange) {
        Boolean b = isRouteEnabled(exchange);
        if (b == null) {
            b = isContextEnabled(exchange);
        }
        return (b == null) ? enabled.get() : b;
    }

    public Boolean isRouteEnabled(Exchange exchange) {
        if (exchange.getFromRouteId() != null) {
            return perRoute.get(exchange.getFromRouteId());
        } else {
            return true;
        }
    }

    public Boolean isContextEnabled(Exchange exchange) {
        return perContext.get(exchange.getContext().getName());
    }

}
