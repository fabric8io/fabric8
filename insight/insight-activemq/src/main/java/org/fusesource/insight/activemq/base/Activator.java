package org.fusesource.insight.activemq.base;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.felix.gogo.commands.basic.SimpleCommand;
import org.fusesource.insight.activemq.audit.Auditor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class Activator implements BundleActivator, ManagedService {

    public static final String INSIGHT_ACTIVEMQ_PID = "org.fusesource.insight.activemq";

    private static final transient Logger LOG = LoggerFactory.getLogger(Activator.class);

    private static Activator INSTANCE;

    private final Map<String, InsightBrokerPlugin> plugins = new HashMap<String, InsightBrokerPlugin>();
    private StorageProxy storageProxy = new StorageProxy();
    private BundleContext bundleContext;
    private MBeanServer mbeanServer;
    private ServiceRegistration<ManagedService> registration;
    private List<ServiceRegistration> commandRegistrations;

    public Activator() {
        this.plugins.put("auditor", new Auditor(storageProxy));
    }

    public static Broker installPlugins(Broker broker) throws Exception {
        Activator activator = INSTANCE;
        if (activator != null) {
            return activator.doInstallPlugins(broker);
        }
        return broker;
    }

    protected Broker doInstallPlugins(Broker broker) throws Exception {
        for (BrokerPlugin plugin : plugins.values()) {
            broker = plugin.installPlugin(broker);
        }
        return broker;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        this.bundleContext = context;
        storageProxy.setContext(context);
        storageProxy.init();
        mbeanServer = lookupMBeanServer();
        if (mbeanServer != null) {
            for (BrokerPlugin plugin : plugins.values()) {
                try {
                    mbeanServer.registerMBean(plugin, getObjectName(plugin));
                } catch (Exception e) {
                    LOG.warn("An error occured during mbean server unregistration: " + e, e);
                }
            }
        }
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, INSIGHT_ACTIVEMQ_PID);
        registration = bundleContext.registerService(ManagedService.class, this, props);
        commandRegistrations = Arrays.asList(
//                SimpleCommand.export(bundleContext, AuditorCommand.class),
//                SimpleCommand.export(bundleContext, BreadcrumbsCommand.class),
//                SimpleCommand.export(bundleContext, ProfilerCommand.class),
//                SimpleCommand.export(bundleContext, TracerCommand.class)
        );
        Activator.INSTANCE = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        Activator.INSTANCE = null;
        for (ServiceRegistration sr : commandRegistrations) {
            sr.unregister();
        }
        registration.unregister();
        if (mbeanServer != null) {
            for (BrokerPlugin plugin : plugins.values()) {
                try {
                    mbeanServer.unregisterMBean(getObjectName(plugin));
                } catch (Exception e) {
                    LOG.warn("An error occured during mbean server unregistration: " + e, e);
                }
            }
        }
        storageProxy.destroy();
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        Map<String, Map<String, String>> props = new HashMap<String, Map<String, String>>();
        for (String plugin : plugins.keySet()) {
            props.put(plugin, new HashMap<String, String>());
        }
        if (properties != null) {
            for (Enumeration<String> es = properties.keys(); es.hasMoreElements();) {
                String key = es.nextElement();
                Object val = properties.get(key);
                for (String plugin : plugins.keySet()) {
                    if (key.startsWith(plugin + ".")) {
                        key = key.substring((plugin + ".").length());
                        props.get(plugin).put(key, val != null ? val.toString() : null);
                    }
                }
            }
        }
        for (String key : plugins.keySet()) {
            Map<String, String> p = props.get(key);
            plugins.get(key).update(p);
        }
    }

    protected ObjectName getObjectName(BrokerPlugin plugin) throws MalformedObjectNameException {
        return new ObjectName("org.fusesource.insight:type=ActiveMQ" + plugin.getClass().getSimpleName());
    }

    protected MBeanServer lookupMBeanServer() {
        ServiceReference ref = bundleContext.getServiceReference(MBeanServer.class.getName());
        if (ref != null) {
            return (MBeanServer) bundleContext.getService(ref);
        } else {
            LOG.warn("Could not find MBeanServer in the OSGi registry so using the platform MBeanServer instead");
            return ManagementFactory.getPlatformMBeanServer();
        }
    }

}
