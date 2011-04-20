/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.fusesource.fabric.dosgi.capset.CapabilitySet;
import org.fusesource.fabric.dosgi.capset.SimpleFilter;
import org.fusesource.fabric.dosgi.io.ClientInvoker;
import org.fusesource.fabric.dosgi.io.ServerInvoker;
import org.fusesource.fabric.dosgi.tcp.ClientInvokerImpl;
import org.fusesource.fabric.dosgi.tcp.ServerInvokerImpl;
import org.fusesource.fabric.dosgi.util.AriesFrameworkUtil;
import org.fusesource.fabric.dosgi.util.Utils;
import org.fusesource.fabric.dosgi.util.UuidGenerator;
import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.tracker.NodeEvent;
import org.linkedin.zookeeper.tracker.NodeEventsListener;
import org.linkedin.zookeeper.tracker.ZKStringDataReader;
import org.linkedin.zookeeper.tracker.ZooKeeperTreeTracker;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.EventHook;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.osgi.service.remoteserviceadmin.RemoteConstants.*;

public class Manager implements ServiceListener, ListenerHook, EventHook, FindHook, NodeEventsListener<String> {

    public static final String CONFIG = "fabric-dosgi";

    private static final Logger LOGGER = LoggerFactory.getLogger(Manager.class);
    private static final String DOSGI_REGISTRY = "/fabric/dosgi";
    private static final String FABRIC_ADDRESS = "fabric.address";

    private final BundleContext bundleContext;

    private ServiceRegistration registration;

    //
    // Discovery part
    //

    // The zookeeper client
    private final IZKClient zooKeeper;
    // The tracked zookeeper tree
    private ZooKeeperTreeTracker<String> tree;
    // Remote endpoints
    private final CapabilitySet<EndpointDescription> remoteEndpoints;

    //
    // Internal data structures
    //
    private final DispatchQueue queue;

    private final Map<ServiceReference, ExportRegistration> exportedServices;

    private final Map<EndpointDescription, Map<Long, ImportRegistration>> importedServices;

    private final Map<ListenerInfo, SimpleFilter> listeners;

    private String uuid;

    private final String uri;

    private ClientInvoker client;

    private ServerInvoker server;

    public Manager(BundleContext context, IZKClient zooKeeper) throws Exception {
        this(context, zooKeeper, "tcp://0.0.0.0:2543");
    }

    public Manager(BundleContext context, IZKClient zooKeeper, String uri) throws Exception {
        this.queue = Dispatch.createQueue();
        this.importedServices = new ConcurrentHashMap<EndpointDescription, Map<Long, ImportRegistration>>();
        this.exportedServices = new ConcurrentHashMap<ServiceReference, ExportRegistration>();
        this.listeners = new ConcurrentHashMap<ListenerInfo, SimpleFilter>();
        this.remoteEndpoints = new CapabilitySet<EndpointDescription>(
                Arrays.asList(Constants.OBJECTCLASS, ENDPOINT_FRAMEWORK_UUID), false);
        this.bundleContext = context;
        this.zooKeeper = zooKeeper;
        this.uri = uri;
    }

    public void init() throws Exception {
        // Create client and server
        this.client = new ClientInvokerImpl(queue);
        this.server = new ServerInvokerImpl(uri, queue);
        this.client.start();
        this.server.start();
        // ZooKeeper tracking
        try {
            this.zooKeeper.createWithParents(DOSGI_REGISTRY, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException.NodeExistsException e) {
            // The node already exists, that's fine
        }
        this.tree = new ZooKeeperTreeTracker<String>(this.zooKeeper, new ZKStringDataReader(), DOSGI_REGISTRY);
        this.tree.track(this);
        // UUID
        this.uuid = Utils.getUUID(this.bundleContext);
        // Service listener filter
        String filter = "(" + RemoteConstants.SERVICE_EXPORTED_INTERFACES + "=*)";
        // Initialization
        this.bundleContext.addServiceListener(this, filter);
        // Service registration
        this.registration = this.bundleContext.registerService(new String[] { ListenerHook.class.getName(), EventHook.class.getName(), FindHook.class.getName() }, this, null);
        // Check existing services
        ServiceReference[] references = this.bundleContext.getServiceReferences(null, filter);
        if (references != null) {
            for (ServiceReference reference : references) {
                exportService(reference);
            }
        }
    }

    public void destroy() {
        for (Map<Long, ImportRegistration> registrations : this.importedServices.values()) {
            for (ImportRegistration registration : registrations.values()) {
                registration.getImportedService().unregister();
            }
        }
        for (ServiceReference reference : this.exportedServices.keySet()) {
            unExportService(reference);
        }
        this.server.stop();
        this.client.stop();
        this.tree.destroy();
        this.registration.unregister();
        this.bundleContext.removeServiceListener(this);
    }

    //
    // ServiceListener
    //

    public void serviceChanged(final ServiceEvent event) {
        final ServiceReference reference = event.getServiceReference();
        switch (event.getType()) {
            case ServiceEvent.REGISTERED:
                exportService(reference);
                break;
            case ServiceEvent.MODIFIED:
                updateService(reference);
                break;
            case ServiceEvent.UNREGISTERING:
                unExportService(reference);
                break;
        }
    }

    //
    // ListenerHook
    //

    @SuppressWarnings("unchecked")
    public void added(final Collection listenerInfos) {
        for (ListenerInfo listenerInfo : (Collection<ListenerInfo>) listenerInfos) {
            // Ignore our own listeners or those that don't have any filter
            if (listenerInfo.getBundleContext() == bundleContext || listenerInfo.getFilter() == null) {
                continue;
            }
            // Make sure we only import remote services
            String filter = "(&" + listenerInfo.getFilter() + "(!(" + ENDPOINT_FRAMEWORK_UUID + "=" + this.uuid + ")))";
            SimpleFilter exFilter = SimpleFilter.parse(filter);
            listeners.put(listenerInfo, exFilter);
            // Iterate through known services and import them if needed
            Set<EndpointDescription> matches = remoteEndpoints.match(exFilter);
            for (EndpointDescription endpoint : matches) {
                doImportService(endpoint, listenerInfo);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void removed(final Collection listenerInfos) {
        for (ListenerInfo listenerInfo : (Collection<ListenerInfo>) listenerInfos) {
            // Ignore our own listeners or those that don't have any filter
            if (listenerInfo.getBundleContext() == bundleContext || listenerInfo.getFilter() == null) {
                continue;
            }
            SimpleFilter exFilter = listeners.remove(listenerInfo);
            // Iterate through known services and dereference them if needed
            Set<EndpointDescription> matches = remoteEndpoints.match(exFilter);
            for (EndpointDescription endpoint : matches) {
                Map<Long, ImportRegistration> registrations = importedServices.get(endpoint);
                if (registrations != null) {
                    ImportRegistration registration = registrations.get(listenerInfo.getBundleContext().getBundle().getBundleId());
                    if (registration != null) {
                        registration.removeReference(listenerInfo);
                        if (!registration.hasReferences()) {
                            registration.getImportedService().unregister();
                            registrations.remove(listenerInfo.getBundleContext().getBundle().getBundleId());
                        }
                    }
                }
            }
        }
    }

    //
    // EventHook
    //

    @SuppressWarnings("unchecked")
    public void event(ServiceEvent event, Collection collection) {
        // Our imported services are exported from within the importing bundle and should only be visible it
        ServiceReference reference = event.getServiceReference();
        if (reference.getProperty(SERVICE_IMPORTED) != null && reference.getProperty(FABRIC_ADDRESS) != null) {
            Collection<BundleContext> contexts = (Collection<BundleContext>) collection;
            for (Iterator<BundleContext> iterator = contexts.iterator(); iterator.hasNext();) {
                BundleContext context = iterator.next();
                if (context != reference.getBundle().getBundleContext()) {
                    iterator.remove();
                }
            }
        }
    }

    //
    // FindHook
    //

    @SuppressWarnings("unchecked")
    public void find(BundleContext context, String name, String filter, boolean allServices, Collection collection) {
        // Our imported services are exported from within the importing bundle and should only be visible it
        Collection<ServiceReference> references = (Collection<ServiceReference>) collection;
        for (Iterator<ServiceReference> iterator = references.iterator(); iterator.hasNext();) {
            ServiceReference reference = iterator.next();
            if (reference.getProperty(SERVICE_IMPORTED) != null && reference.getProperty(FABRIC_ADDRESS) != null) {
                if (context != reference.getBundle().getBundleContext()) {
                    iterator.remove();
                }
            }
        }
    }


    //
    // NodeEventsListener
    //

    public void onEvents(final Collection<NodeEvent<String>> nodeEvents) {
        try {
            for (NodeEvent<String> event : nodeEvents) {
                if (event.getDepth() == 0) {
                    continue;
                }
                switch (event.getEventType()) {
                    case ADDED: {
                        EndpointDescription endpoint = Utils.getEndpointDescription(event.getData());
                        remoteEndpoints.addCapability(endpoint);
                        // Check existing listeners
                        for (Map.Entry<ListenerInfo, SimpleFilter> entry : listeners.entrySet()) {
                            if (CapabilitySet.matches(endpoint, entry.getValue())) {
                                doImportService(endpoint, entry.getKey());
                            }
                        }
                    }
                    break;
                    case UPDATED: {
                        EndpointDescription endpoint = Utils.getEndpointDescription(event.getData());
                        Map<Long, ImportRegistration> registrations = importedServices.get(endpoint);
                        for (ImportRegistration reg : registrations.values()) {
                            reg.importedService.setProperties(new Hashtable<String, Object>(endpoint.getProperties()));
                        }
                    }
                    break;
                    case DELETED: {
                        EndpointDescription endpoint = Utils.getEndpointDescription(event.getData());
                        remoteEndpoints.removeCapability(endpoint);
                        Map<Long, ImportRegistration> registrations = importedServices.remove(endpoint);
                        for (ImportRegistration reg : registrations.values()) {
                            reg.getImportedService().unregister();
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.info("Error when handling zookeeper events", e);
        }
    }

    //
    // Export logic
    //

    protected void exportService(final ServiceReference reference) {
        if (!exportedServices.containsKey(reference)) {
            try {
                ExportRegistration registration = doExportService(reference);
                if (registration != null) {
                    exportedServices.put(reference, registration);
                }
            } catch (Exception e) {
                LOGGER.info("Error when exporting endpoint", e);
            }
        }
    }

    protected void updateService(final ServiceReference reference) {
        ExportRegistration registration = exportedServices.get(reference);
        if (registration != null) {
            try {
                // TODO: implement logic
                // TODO: need to reflect simple properties change, but also export
                // TODO: related properties like the exported interfaces
            } catch (Exception e) {
                LOGGER.info("Error when updating endpoint", e);
            }
        }
    }

    protected void unExportService(final ServiceReference reference) {
        try {
            ExportRegistration registration = exportedServices.remove(reference);
            if (registration != null) {
                server.unregisterService(registration.getExportedEndpoint().getId());
                zooKeeper.delete(registration.getZooKeeperNode());
            }
        } catch (Exception e) {
            LOGGER.info("Error when unexporting endpoint", e);
        }
    }

    protected ExportRegistration doExportService(final ServiceReference reference) throws Exception {
        // Compute properties
        Map<String, Object> properties = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        for (String k : reference.getPropertyKeys()) {
            properties.put(k, reference.getProperty(k));
        }
        // Bail out if there is any intents specified, we don't support any
        Set<String> intents = Utils.normalize(properties.get(SERVICE_EXPORTED_INTENTS));
        Set<String> extraIntents = Utils.normalize(properties.get(SERVICE_EXPORTED_INTENTS_EXTRA));
        if (!intents.isEmpty() || !extraIntents.isEmpty()) {
            throw new UnsupportedOperationException();
        }
        // Bail out if there are any configurations specified, we don't support any
        Set<String> configs = Utils.normalize(properties.get(SERVICE_EXPORTED_CONFIGS));
        if (configs.isEmpty()) {
            configs.add(CONFIG);
        } else if (!configs.contains(CONFIG)) {
            throw new UnsupportedOperationException();
        }

        properties.remove(SERVICE_EXPORTED_CONFIGS);
        properties.put(SERVICE_IMPORTED_CONFIGS, new String[] { CONFIG });
        properties.put(ENDPOINT_FRAMEWORK_UUID, this.uuid);
        properties.put(FABRIC_ADDRESS, this.server.getConnectAddress());

        String uuid = UuidGenerator.getUUID();
        properties.put(ENDPOINT_ID, uuid);

        // Now, export the service
        EndpointDescription description = new EndpointDescription(properties);

        // Export it
        server.registerService(description.getId(), new ServerInvoker.ServiceFactory() {
            public Object get() {
                return reference.getBundle().getBundleContext().getService(reference);
            }
            public void unget() {
                reference.getBundle().getBundleContext().ungetService(reference);
            }
        }, AriesFrameworkUtil.getClassLoader(reference.getBundle()));

        String descStr = Utils.getEndpointDescriptionXML(description);
        // Publish in ZooKeeper
        final String nodePath = zooKeeper.create(DOSGI_REGISTRY + "/" + uuid,
                descStr.getBytes("UTF-8"),
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        // Return
        return new ExportRegistration(reference, description, nodePath);
    }

    //
    // Import logic
    //

    protected ImportRegistration doImportService(final EndpointDescription endpoint, final ListenerInfo listener) {
        Map<Long, ImportRegistration> registrations = importedServices.get(endpoint);
        if (registrations == null) {
            registrations = new HashMap<Long, ImportRegistration>();
            importedServices.put(endpoint, registrations);
        }
        ImportRegistration reg = registrations.get(listener.getBundleContext().getBundle().getBundleId());
        if (reg == null) {
            Bundle bundle = bundleContext.getBundle(listener.getBundleContext().getBundle().getBundleId());
            ServiceRegistration registration = bundle.getBundleContext().registerService(
                    endpoint.getInterfaces().toArray(new String[endpoint.getInterfaces().size()]),
                    new Factory(endpoint),
                    new Hashtable<String, Object>(endpoint.getProperties())
            );
            reg = new ImportRegistration(registration, endpoint);
            registrations.put(listener.getBundleContext().getBundle().getBundleId(), reg);
        }
        reg.addReference(listener);
        return reg;
    }

    class Factory implements ServiceFactory {

        private final EndpointDescription description;

        Factory(EndpointDescription description) {
            this.description = description;
        }

        public Object getService(Bundle bundle, ServiceRegistration registration) {
            ClassLoader classLoader = AriesFrameworkUtil.getClassLoader(bundle);
            List<Class> interfaces = new ArrayList<Class>();
            for (String interfaceName : description.getInterfaces()) {
                try {
                    interfaces.add(classLoader.loadClass(interfaceName));
                } catch (ClassNotFoundException e) {
                    // Ignore
                }
            }
            String address = (String) description.getProperties().get(FABRIC_ADDRESS);
            InvocationHandler handler = client.getProxy(address, description.getId(), classLoader);
            return Proxy.newProxyInstance(classLoader, interfaces.toArray(new Class[interfaces.size()]), handler);
        }

        public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
        }

    }

}