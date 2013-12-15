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
package io.fabric8.dosgi.impl;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import io.fabric8.dosgi.api.Dispatched;
import io.fabric8.dosgi.api.SerializationStrategy;
import io.fabric8.dosgi.capset.CapabilitySet;
import io.fabric8.dosgi.capset.SimpleFilter;
import io.fabric8.dosgi.io.ClientInvoker;
import io.fabric8.dosgi.io.ServerInvoker;
import io.fabric8.dosgi.tcp.ClientInvokerImpl;
import io.fabric8.dosgi.tcp.ServerInvokerImpl;
import io.fabric8.dosgi.util.AriesFrameworkUtil;
import io.fabric8.dosgi.util.Utils;
import io.fabric8.dosgi.util.UuidGenerator;
import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;
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

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URI;
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
import java.util.concurrent.TimeUnit;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.create;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.delete;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_FRAMEWORK_UUID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_CONFIGS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTENTS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTENTS_EXTRA;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED_CONFIGS;

public class Manager implements ServiceListener, ListenerHook, EventHook, FindHook, PathChildrenCacheListener, Dispatched {

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
    private final CuratorFramework curator;
    // The tracked zookeeper tree
    private TreeCache tree;
    // Remote endpoints
    private final CapabilitySet<EndpointDescription> remoteEndpoints;

    //
    // Internal data structures
    //
    private final DispatchQueue queue;

    private final Map<ServiceReference, ExportRegistration> exportedServices;

    private final Map<EndpointDescription, Map<Long, ImportRegistration>> importedServices;

    private final Map<ListenerInfo, SimpleFilter> listeners;

    private final Map<String, SerializationStrategy> serializationStrategies;


    private String uuid;

    private final String uri;

    private final String exportedAddress;

    private final long timeout;

    private ClientInvoker client;

    private ServerInvoker server;

    public Manager(BundleContext context, CuratorFramework curator) throws Exception {
        this(context, curator, "tcp://0.0.0.0:2543", null, TimeUnit.MINUTES.toMillis(5));
    }

    public Manager(BundleContext context, CuratorFramework curator, String uri, String exportedAddress, long timeout) throws Exception {
        this.queue = Dispatch.createQueue();
        this.importedServices = new ConcurrentHashMap<EndpointDescription, Map<Long, ImportRegistration>>();
        this.exportedServices = new ConcurrentHashMap<ServiceReference, ExportRegistration>();
        this.listeners = new ConcurrentHashMap<ListenerInfo, SimpleFilter>();
        this.serializationStrategies = new ConcurrentHashMap<String, SerializationStrategy>();
        this.remoteEndpoints = new CapabilitySet<EndpointDescription>(
                Arrays.asList(Constants.OBJECTCLASS, ENDPOINT_FRAMEWORK_UUID), false);
        this.bundleContext = context;
        this.curator = curator;
        this.uri = uri;
        this.exportedAddress = exportedAddress;
        this.timeout = timeout;
    }

    public void init() throws Exception {
        // Create client and server
        this.client = new ClientInvokerImpl(queue, timeout, serializationStrategies);
        this.server = new ServerInvokerImpl(uri, queue, serializationStrategies);
        this.client.start();
        this.server.start();
        // ZooKeeper tracking
        try {
            create(curator, DOSGI_REGISTRY, CreateMode.PERSISTENT);
        } catch (KeeperException.NodeExistsException e) {
            // The node already exists, that's fine
        }
        this.tree = new TreeCache(curator,  DOSGI_REGISTRY, true);
        this.tree.getListenable().addListener(this);
        this.tree.start();
        // UUID
        this.uuid = Utils.getUUID(this.bundleContext);
        // Service listener filter
        String filter = "(" + RemoteConstants.SERVICE_EXPORTED_INTERFACES + "=*)";
        // Initialization
        this.bundleContext.addServiceListener(this, filter);
        // Service registration
        this.registration = this.bundleContext.registerService(new String[] { ListenerHook.class.getName(), EventHook.class.getName(), FindHook.class.getName() }, this, null);
        // Check existing services
        ServiceReference[] references = this.bundleContext.getServiceReferences((String) null, filter);
        if (references != null) {
            for (ServiceReference reference : references) {
                exportService(reference);
            }
        }
    }

    public void destroy() throws IOException {
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
        this.tree.close();
        if (registration != null) {
            this.registration.unregister();
        }
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
                if (context != reference.getBundle().getBundleContext() && context != this.bundleContext) {
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
                if (context != reference.getBundle().getBundleContext() && context != this.bundleContext) {
                    iterator.remove();
                }
            }
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
                delete(curator, registration.getZooKeeperNode());
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

        URI connectUri = new URI(this.server.getConnectAddress());
        String fabricAddress = connectUri.getScheme() + "://" + exportedAddress + ":" + connectUri.getPort();

        properties.remove(SERVICE_EXPORTED_CONFIGS);
        properties.put(SERVICE_IMPORTED_CONFIGS, new String[] { CONFIG });
        properties.put(ENDPOINT_FRAMEWORK_UUID, this.uuid);
        properties.put(FABRIC_ADDRESS, fabricAddress);

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
        final String nodePath = create(curator, DOSGI_REGISTRY + "/" + uuid, descStr, CreateMode.EPHEMERAL);
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

    public DispatchQueue queue() {
        return queue;
    }

    @Override
    public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent event) throws Exception {
        switch (event.getType()) {
            case CHILD_ADDED: {

                EndpointDescription endpoint = Utils.getEndpointDescription(new String(event.getData().getData()));
                remoteEndpoints.addCapability(endpoint);
                // Check existing listeners
                for (Map.Entry<ListenerInfo, SimpleFilter> entry : listeners.entrySet()) {
                    if (CapabilitySet.matches(endpoint, entry.getValue())) {
                        doImportService(endpoint, entry.getKey());
                    }
                }
            }
            break;
            case CHILD_UPDATED: {
                EndpointDescription endpoint = Utils.getEndpointDescription(new String(event.getData().getData()));
                Map<Long, ImportRegistration> registrations = importedServices.get(endpoint);
                if (registrations != null) {
                    for (ImportRegistration reg : registrations.values()) {
                        reg.importedService.setProperties(new Hashtable<String, Object>(endpoint.getProperties()));
                    }
                }
            }
            break;
            case CHILD_REMOVED: {
                EndpointDescription endpoint = Utils.getEndpointDescription(new String(event.getData().getData()));
                remoteEndpoints.removeCapability(endpoint);
                Map<Long, ImportRegistration> registrations = importedServices.remove(endpoint);
                if (registrations != null) {
                    for (ImportRegistration reg : registrations.values()) {
                        reg.getImportedService().unregister();
                    }
                }
            }
            break;
        }
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