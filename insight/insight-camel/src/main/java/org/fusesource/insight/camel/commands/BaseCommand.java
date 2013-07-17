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
package org.fusesource.insight.camel.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.console.AbstractAction;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.fusesource.insight.camel.base.Activator;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 */
public abstract class BaseCommand extends OsgiCommandSupport {

    @Argument(name = "enabled", required = false)
    Boolean enabled;

    @Option(name = "--context")
    String context;

    @Option(name = "--route")
    String route;

    @Option(name = "--reset")
    boolean reset;

    @Option(name = "--clear")
    boolean clear;

    public abstract String getStrategy();

    @Override
    public Object execute(CommandSession session) throws Exception {
        setBundleContext(FrameworkUtil.getBundle(getClass()).getBundleContext());
        return super.execute(session);
    }

    @Override
    protected Object doExecute() throws Exception {
        Configuration cfg = getConfiguration();
        Dictionary<String, Object> props = cfg.getProperties();
        if (props == null) {
            props = new Hashtable<String, Object>();
        }

        // If no argument is given at all, display config
        if (!reset && !clear && enabled == null && context == null && route == null) {
            Map<String, String> map = new TreeMap<String, String>();
            for (Enumeration<String> e = props.keys(); e.hasMoreElements();) {
                String s = e.nextElement();
                if (s.startsWith(getStrategy())) {
                    map.put(s.substring(getStrategy().length() + 1), props.get(s).toString());
                }
            }
            if (map.isEmpty()) {
                System.out.println("No configuration set");
            }
            for (Map.Entry<String, String> e : map.entrySet()) {
                System.out.println(e.getKey() + " = " + e.getValue());
            }
            return null;
        }

        if (reset) {
            Set<String> keys = new HashSet<String>();
            for (Enumeration<String> e = props.keys(); e.hasMoreElements();) {
                String s = e.nextElement();
                if (s.startsWith(getStrategy())) {
                    keys.add(s);
                }
            }
            for (String s : keys) {
                props.remove(s);
            }
        }

        String key;
        if (context != null) {
            key = getStrategy() + ".context." + context;
        } else if (route != null) {
            key = getStrategy() + ".route." + route;
        } else {
            key = getStrategy() + ".enabled";
        }

        if (clear) {
            props.remove(key);
        } else if (enabled != null) {
            props.put(key, enabled.toString());
        }
        cfg.update(props);
        return null;
    }

    protected Configuration getConfiguration() throws IOException {
        return getConfigAdmin().getConfiguration(Activator.INSIGHT_CAMEL_PID, null);
    }

    protected ConfigurationAdmin getConfigAdmin() {
        ServiceReference ref = getBundleContext().getServiceReference(ConfigurationAdmin.class.getName());
        return (ConfigurationAdmin) getService(ref);
    }

}
