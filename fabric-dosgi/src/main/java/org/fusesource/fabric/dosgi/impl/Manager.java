/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.thoughtworks.xstream.XStream;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.fusesource.fabric.dosgi.capset.CapabilitySet;
import org.fusesource.fabric.dosgi.capset.SimpleFilter;
import org.fusesource.fabric.dosgi.io.Transport;
import org.fusesource.fabric.dosgi.io.TransportAcceptListener;
import org.fusesource.fabric.dosgi.io.TransportListener;
import org.fusesource.fabric.dosgi.io.TransportServer;
import org.fusesource.fabric.dosgi.tcp.LengthPrefixedCodec;
import org.fusesource.fabric.dosgi.tcp.TcpTransport;
import org.fusesource.fabric.dosgi.tcp.TcpTransportFactory;
import org.fusesource.fabric.dosgi.tcp.TcpTransportServer;
import org.fusesource.fabric.dosgi.util.BundleDelegatingClassLoader;
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

public class Manager implements ServiceListener, ListenerHook, EventHook, FindHook, NodeEventsListener<String>, TransportListener, TransportAcceptListener {

    public static final String CONFIG = "fabric-dosgi";

    private static final Logger LOGGER = LoggerFactory.getLogger(Manager.class);
    private static final String DOSGI_REGISTRY = "/fabric/dosgi";
    private static final String FABRIC_ADDRESS = "fabric.address";

    private final BundleContext bundleContext;

    private ServiceRegistration registration;

    private ExecutorService executor = Executors.newCachedThreadPool();

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

    private final Map<String, ExportRegistration> exportedServicesPerId;

    private String uuid;

    private TcpTransportServer server;

    private final Map<String, TcpTransport> transports;

    private final Map<String, List<RemoteRequest>> pending;

    private String uri = "tcp://0.0.0.0:2543";

    private final Map<String, AtomicReference<RemoteResponse>> requests;

    public Manager(BundleContext context, IZKClient zooKeeper) {
        this.queue = Dispatch.createQueue();
        this.importedServices = new ConcurrentHashMap<EndpointDescription, Map<Long, ImportRegistration>>();
        this.exportedServices = new ConcurrentHashMap<ServiceReference, ExportRegistration>();
        this.exportedServicesPerId = new ConcurrentHashMap<String, ExportRegistration>();
        this.listeners = new ConcurrentHashMap<ListenerInfo, SimpleFilter>();
        this.remoteEndpoints = new CapabilitySet<EndpointDescription>(Collections.singletonList(Constants.OBJECTCLASS), false);
        this.bundleContext = context;
        this.zooKeeper = zooKeeper;
        this.transports = new ConcurrentHashMap<String, TcpTransport>();
        this.requests = new ConcurrentHashMap<String, AtomicReference<RemoteResponse>>();
        this.pending = new ConcurrentHashMap<String, List<RemoteRequest>>();
    }

    public void init() throws Exception {
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
        // Start server
        this.server = new TcpTransportFactory().bind(this.uri);
        this.server.setDispatchQueue(queue);
        this.server.setAcceptListener(this);
        this.server.start();
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
        this.server.stop();
        this.tree.destroy();
        this.registration.unregister();
        this.bundleContext.removeServiceListener(this);
    }

    //
    // TransportAcceptListener
    //

    public void onAccept(TransportServer transportServer, TcpTransport transport) {
        LOGGER.debug("Accepting incoming connection");
        transport.setProtocolCodec(new LengthPrefixedCodec());
        transport.setDispatchQueue(queue);
        transport.setTransportListener(this);
        transport.start();
    }

    public void onAcceptError(TransportServer transportServer, Exception error) {
        LOGGER.error("Error while accepting incoming connection", error);
    }

    //
    // TransportListener
    //

    public void onTransportConnected(Transport transport) {
        LOGGER.debug("Transport connected");
        transport.resumeRead();
        onRefill(transport);
    }

    public void onTransportDisconnected(Transport transport) {
        LOGGER.debug("Transport diconnected");
    }

    public void onTransportFailure(Transport transport, IOException error) {
        LOGGER.info("Transport failure", error);
    }

    public void onRefill(Transport transport) {
        List<RemoteRequest> pr = pending.get(transport.getRemoteAddress());
        if (pr != null && !pr.isEmpty()) {
            final RemoteRequest request = pr.remove(0);
            final ExportRegistration registration = exportedServicesPerId.get(request.serviceId);
            try {
                transport.offer(request.toByteArray(registration.getXStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void onTransportCommand(final Transport transport, Object data) {
        try {
            final ByteArrayInputStream bais = new ByteArrayInputStream((byte[]) data);
            final DataInputStream dis = new DataInputStream(bais);
            final boolean isRequest = dis.readByte() == 0;
            final String correlation = dis.readUTF();
            final String serviceId = dis.readUTF();
            final ExportRegistration registration = exportedServicesPerId.get(serviceId);
            if (isRequest) {
                try {
                    final Invocation invocation = (Invocation) registration.getXStream().fromXML(dis);
                    Object service = bundleContext.getService(registration.getExportedService());
                    try {
                        Method method = service.getClass().getMethod(invocation.method, invocation.types);
                        Object response = method.invoke(service, invocation.args);
                        transport.offer(new RemoteResponse(correlation, serviceId, response));
                    } finally {
                        bundleContext.ungetService(registration.getExportedService());
                    }
                }
                catch (Throwable t) {
                    try {
                        if (t instanceof InvocationTargetException) {
                            t = ((InvocationTargetException) t).getTargetException();
                        }
                        transport.offer(new RemoteResponse(correlation, serviceId, t).toByteArray(registration.getXStream()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                final boolean error = dis.readBoolean();
                final Object v = registration.getXStream().fromXML(dis);
                RemoteResponse response = error ? new RemoteResponse(correlation, serviceId, (Throwable) v) : new RemoteResponse(correlation, serviceId, v);
                AtomicReference<RemoteResponse> ref = requests.get(response.correlation);
                synchronized (ref) {
                    ref.set(response);
                    ref.notifyAll();
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
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
                // TODO: the service properties have been modified, we need to
                // TODO: update the registration accordingly
                break;
            case ServiceEvent.UNREGISTERING:
                unExportService(reference);
                break;
        }
    }

    //
    // ListenerHook
    //

    public void added(final Collection listenerInfos) {
        executor.execute(new Runnable() {
            public void run() {
                for (ListenerInfo listenerInfo : (Collection<ListenerInfo>) listenerInfos) {
                    // Ignore our own listeners or those that don't have any filter
                    if (listenerInfo.getBundleContext() == bundleContext || listenerInfo.getFilter() == null) {
                        continue;
                    }
                    // Make sure we only import remote services
                    SimpleFilter exFilter = SimpleFilter.parse(extendFilter(listenerInfo.getFilter()));
                    listeners.put(listenerInfo, exFilter);
                    // Iterate through known services and import them if needed
                    Set<EndpointDescription> matches = remoteEndpoints.match(exFilter);
                    for (EndpointDescription endpoint : matches) {
                        doImportService(endpoint, listenerInfo);
                    }
                }
            }
        });
    }

    public void removed(final Collection listenerInfos) {
        executor.execute(new Runnable() {
            public void run() {
                for (ListenerInfo listenerInfo : (Collection<ListenerInfo>) listenerInfos) {
                    // Ignore our own listeners or those that don't have any filter
                    if (listenerInfo.getBundleContext() == bundleContext || listenerInfo.getFilter() == null) {
                        continue;
                    }
                    SimpleFilter exFilter = listeners.remove(listenerInfo);
                    // Iterate through known services and import them if needed
                    // TODO: we could cache the information instead of computing it again?
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
        });
    }

    public String extendFilter(String filter) {
        return filter;
//        TODO: return "(&" + filter + "(!(" + ENDPOINT_FRAMEWORK_UUID + "=" + this.uuid + ")))";
    }

    //
    // EventHook
    //


    public void event(ServiceEvent event, Collection collection) {
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

    public void find(BundleContext context, String name, String filter, boolean allServices, Collection collection) {
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
        executor.execute(new Runnable() {
            public void run() {
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
                            case UPDATED:
                                // TODO: data has changed, certainly because the service properties of the exported
                                // TODO: service have been updated, we need to reflect that on the registered service
                                break;
                            case DELETED: {
                                EndpointDescription endpoint = Utils.getEndpointDescription(event.getData());
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
        });
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
                    exportedServicesPerId.put(registration.getExportedEndpoint().getId(), registration);
                }
            } catch (Exception e) {
                LOGGER.info("Error when exporting endpoint", e);
            }
        }
    }

    protected void unExportService(final ServiceReference reference) {
        try {
            ExportRegistration registration = exportedServices.remove(reference);
            if (registration != null) {
                zooKeeper.delete(registration.getZooKeeperNode());
            }
        } catch (Exception e) {
            LOGGER.info("Error when unexporting endpoint", e);
        }
    }

    protected ExportRegistration doExportService(ServiceReference reference) throws Exception {
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

    //
    // Request logic
    //

    protected Object request(EndpointDescription description, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            if ("toString".equals(method.getName())) {
                return description.toString();
            } else if ("equals".equals(method.getName())) {
                return description.equals(((Factory) Proxy.getInvocationHandler(args[0])).description);
            } else if ("hashCode".equals(method.getName())) {
                return description.hashCode();
            }
        }
        final RemoteRequest request = new RemoteRequest(description, new Invocation(method.getName(), method.getParameterTypes(), args));
        AtomicReference<RemoteResponse> result = new AtomicReference<RemoteResponse>();
        synchronized (result) {
            requests.put(request.correlation, result);
            doRequest(request);
            result.wait();
        }
        RemoteResponse response = result.get();
        if (response.throwable != null) {
            throw response.throwable;
        } else {
            return response.value;
        }
    }

    protected void doRequest(RemoteRequest request) {
        try {
            String address = (String) request.endpoint.getProperties().get(FABRIC_ADDRESS);
            TcpTransport transport = transports.get(address);
            if (transport == null) {
                transport = new TcpTransportFactory().connect(address);
                transports.put(address, transport);
                transport.setDispatchQueue(queue);
                transport.setProtocolCodec(new LengthPrefixedCodec());
                transport.setTransportListener(this);
                transport.start();
                pending.put(transport.getRemoteAddress(), new CopyOnWriteArrayList<RemoteRequest>());
            }
            if (!transport.isConnected()) {
                List<RemoteRequest> pr = pending.get(transport.getRemoteAddress());
                pr.add(request);
            } else {
                final ExportRegistration registration = exportedServicesPerId.get(request.serviceId);
                transport.offer(request.toByteArray(registration.getXStream()));
            }
        } catch (Exception e) {
            AtomicReference<RemoteResponse> response = requests.get(request.correlation);
            synchronized (response) {
                response.set(new RemoteResponse(request.correlation, request.serviceId, e));
            }
        }
    }

    class Factory implements ServiceFactory, InvocationHandler {

        private final EndpointDescription description;

        Factory(EndpointDescription description) {
            this.description = description;
        }

        public Object getService(Bundle bundle, ServiceRegistration registration) {
            ClassLoader classLoader = new BundleDelegatingClassLoader(bundle, Manager.class.getClassLoader());
            List<Class> interfaces = new ArrayList<Class>();
            for (String interfaceName : description.getInterfaces()) {
                try {
                    interfaces.add(classLoader.loadClass(interfaceName));
                } catch (ClassNotFoundException e) {
                    // Ignore
                }
            }
            return Proxy.newProxyInstance(classLoader, interfaces.toArray(new Class[interfaces.size()]), this);
        }

        public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            return request(description, method, args);
        }
    }

    static public class RemoteRequest {
        public EndpointDescription endpoint;
        public String correlation;
        public String serviceId;
        public Invocation invocation;

        public RemoteRequest(EndpointDescription endpoint, Invocation invocation) {
            this.endpoint = endpoint;
            this.correlation = UuidGenerator.getUUID();
            this.serviceId = endpoint.getId();
            this.invocation = invocation;
        }

        public byte[] toByteArray(XStream xstream) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(0);
            dos.writeUTF(correlation);
            dos.writeUTF(serviceId);
            xstream.toXML(invocation, dos);
            dos.close();
            return baos.toByteArray();
        }
    }

    static public class Invocation {
        public String method;
        public Class[] types;
        public Object[] args;

        public Invocation() {
        }

        public Invocation(String method, Class[] types, Object[] args) {
            this.method = method;
            this.types = types;
            this.args = args;
        }
    }

    static public class RemoteResponse {
        public String correlation;
        public String serviceId;
        public Object value;
        public Throwable throwable;

        public RemoteResponse(String correlation, String serviceId, Throwable throwable) {
            this.correlation = correlation;
            this.serviceId = serviceId;
            this.throwable = throwable;
        }

        public RemoteResponse(String correlation, String serviceId, Object value) {
            this.correlation = correlation;
            this.serviceId = serviceId;
            this.value = value;
        }

        public byte[] toByteArray(XStream xstream) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(1);
            dos.writeUTF(correlation);
            dos.writeUTF(serviceId);
            dos.writeBoolean(throwable != null);
            xstream.toXML(throwable != null ? throwable : value, dos);
            dos.close();
            return baos.toByteArray();
        }
    }

}