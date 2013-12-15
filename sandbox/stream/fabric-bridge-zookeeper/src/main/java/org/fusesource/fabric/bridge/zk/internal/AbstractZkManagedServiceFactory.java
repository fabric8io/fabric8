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

package io.fabric8.bridge.zk.internal;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.jms.ConnectionFactory;

import org.apache.curator.framework.CuratorFramework;
import io.fabric8.api.FabricService;
import io.fabric8.bridge.MessageConverter;
import io.fabric8.bridge.model.BridgeDestinationsConfig;
import io.fabric8.bridge.model.BridgedDestination;
import io.fabric8.bridge.model.BrokerConfig;
import io.fabric8.bridge.model.DispatchPolicy;
import io.fabric8.bridge.zk.model.ZkBridgeDestinationsConfigFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.util.StringUtils;

/**
 * Base {@link ManagedServiceFactory} for Fabric {@link io.fabric8.bridge.zk.ZkBridgeConnector} and {@link io.fabric8.bridge.zk.ZkGatewayConnector}.
 *
 * @see ZkManagedBridgeServiceFactory
 * @see ZkManagedGatewayServiceFactory
 * @author Dhiraj Bokde
 */
public abstract class AbstractZkManagedServiceFactory implements ManagedServiceFactory {

    private static final String CONNECTION_FACTORY_CLASS_NAME = ConnectionFactory.class.getName();
    private static final String DESTINATION_RESOLVER_CLASS_NAME = DestinationResolver.class.getName();
    private static final String MESSAGE_CONVERTER_CLASS_NAME = MessageConverter.class.getName();

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    private CuratorFramework zooKeeper;

    private FabricService fabricService;

    private BundleContext bundleContext;

    protected Map<String, List<ServiceReference>> serviceReferenceMap = new ConcurrentHashMap<String, List<ServiceReference>>();

    public final void init() throws Exception {
        if (zooKeeper == null) {
            throw new IllegalArgumentException("Property zooKeeper must be set!");
        }
        if (fabricService == null) {
            throw new IllegalArgumentException("Property fabricService must be set!");
        }
        if (bundleContext == null) {
            throw new IllegalArgumentException("Property bundleContext must be set!");
        }

        LOG.info("Started");
    }

    public final void destroy() throws Exception {
        // do derived class cleanup
        doDestroy();

        // assert that all service references have been unget
        if (!serviceReferenceMap.isEmpty()) {
            LOG.error("Removing " + serviceReferenceMap.size() + " left over Service references");
            for (List<ServiceReference> references : serviceReferenceMap.values()) {
                for (ServiceReference reference : references) {
                    bundleContext.ungetService(reference);
                }
            }
        }

        LOG.info("Destroyed");
    }

    protected abstract void doDestroy() throws Exception;

    @Override
    public final void deleted(String pid) {
        // do derived class cleanup
        doDeleted(pid);

        // unget service references for this pid
        if (serviceReferenceMap.containsKey(pid)) {
            for (ServiceReference reference : serviceReferenceMap.remove(pid)) {
                bundleContext.ungetService(reference);
            }
        }

        LOG.info("Removed " + pid);
    }

    protected abstract void doDeleted(String pid);

    // lookup destinationsRef using ZkBridgeDestinationsConfigFactory
    protected BridgeDestinationsConfig createDestinationsConfig(String pid, String destinationsRef) throws ConfigurationException {

        ZkBridgeDestinationsConfigFactory factory = new ZkBridgeDestinationsConfigFactory();
        factory.setFabricService(fabricService);
        factory.setId(destinationsRef);
        try {
            BridgeDestinationsConfig destinationsConfig = factory.getObject();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Found [" + pid + "." + destinationsRef + "]: " + destinationsConfig);
            }

            // check if we need to resolve references to MessageConverter services
            DispatchPolicy dispatchPolicy = destinationsConfig.getDispatchPolicy();
            resolveDispatchPolicyRefs(pid, dispatchPolicy);

            for (BridgedDestination destination : destinationsConfig.getDestinations()) {
                if (destination.getDispatchPolicy() != null) {
                    resolveDispatchPolicyRefs(pid, destination.getDispatchPolicy());
                }
            }

            return destinationsConfig;
        } catch (Exception e) {
            String msg = "Error getting destinations for " +
            pid + "." + destinationsRef + " : " + e.getMessage();
            LOG.error(msg, e);
            throw new ConfigurationException(destinationsRef, msg, e);
        }
    }

    private void resolveDispatchPolicyRefs(String pid, DispatchPolicy dispatchPolicy) throws ConfigurationException {
        final String messageConverterRef = dispatchPolicy.getMessageConverterRef();
        if (StringUtils.hasText(messageConverterRef)) {
            // resolve connection factory OSGi service
            final String filter = "(" + Constants.SERVICE_PID + "=" + messageConverterRef + ")";
            ServiceReference[] serviceReferences;
            try {
                serviceReferences = bundleContext.getServiceReferences(MESSAGE_CONVERTER_CLASS_NAME, filter);
            } catch (InvalidSyntaxException e1) {
                String msg = "Error looking up " + messageConverterRef + " with filter [" + filter + "]";
                LOG.error(msg);
                throw new ConfigurationException(messageConverterRef, msg);
            }
            if (serviceReferences != null) {
                dispatchPolicy.setMessageConverter((MessageConverter) bundleContext.getService(serviceReferences[0]));
                // remember the service so we can unget it later
                addServiceReference(pid, serviceReferences[0]);
            } else {
                String msg = "No service found for " + messageConverterRef +
                    " with filter [" + filter + "]";
                LOG.error(msg);
                throw new ConfigurationException(messageConverterRef, msg);
            }
        }
    }

    protected BrokerConfig createBrokerConfig(String pid, String prefix, Dictionary<String, String> properties) throws ConfigurationException {

        final String keyPrefix = prefix + ".";
        for (Enumeration<String> e = properties.keys(); e.hasMoreElements(); ) {

            String key = e.nextElement();
            if (key.startsWith(keyPrefix)) {

                BrokerConfig config = new BrokerConfig();
                config.setId(pid + "." + prefix);

                config.setBrokerUrl(properties.get(prefix + ".brokerUrl"));
                config.setClientId(properties.get(prefix + ".clientId"));
                config.setUserName(properties.get(prefix + ".userName"));
                config.setPassword(properties.get(prefix + ".password"));
                if (StringUtils.hasText(properties.get(prefix + ".maxConnections"))) {
                    config.setMaxConnections(Integer.parseInt(properties.get(prefix + ".maxConnections")));
                }

                final String connectionFactoryRef = properties.get(prefix + ".connectionFactoryRef");
                if (StringUtils.hasText(connectionFactoryRef)) {
                    // resolve connection factory OSGi service
                    final String filter = "(" + Constants.SERVICE_PID + "=" + connectionFactoryRef + ")";
                    ServiceReference[] serviceReferences;
                    try {
                        serviceReferences = bundleContext.getServiceReferences(CONNECTION_FACTORY_CLASS_NAME, filter);
                    } catch (InvalidSyntaxException e1) {
                        String msg = "Error looking up " + connectionFactoryRef + " with filter [" + filter + "]";
                        LOG.error(msg);
                        throw new ConfigurationException(connectionFactoryRef, msg);
                    }
                    if (serviceReferences != null) {
                        config.setConnectionFactoryRef(connectionFactoryRef);
                        config.setConnectionFactory((ConnectionFactory) bundleContext.getService(serviceReferences[0]));
                        // remember the service so we can unget it later
                        addServiceReference(pid, serviceReferences[0]);
                    } else {
                        String msg = "No service found for " + connectionFactoryRef +
                            " with filter [" + filter + "]";
                        LOG.error(msg);
                        throw new ConfigurationException(connectionFactoryRef, msg);
                    }
                }

                final String destinationResolverRef = properties.get(prefix + ".destinationResolverRef");
                if (StringUtils.hasText(destinationResolverRef)) {
                    // resolve connection factory OSGi service
                    final String filter = "(" + Constants.SERVICE_PID + "=" + destinationResolverRef + ")";
                    ServiceReference[] serviceReferences;
                    try {
                        serviceReferences = bundleContext.getServiceReferences(DESTINATION_RESOLVER_CLASS_NAME, filter);
                    } catch (InvalidSyntaxException e1) {
                        String msg = "Error looking up " + destinationResolverRef + " with filter [" + filter + "]";
                        LOG.error(msg);
                        throw new ConfigurationException(destinationResolverRef, msg);
                    }
                    if (serviceReferences != null) {
                        config.setDestinationResolverRef(destinationResolverRef);
                        config.setDestinationResolver((DestinationResolver) bundleContext.getService(serviceReferences[0]));
                        // remember the service so we can unget it later
                        addServiceReference(pid, serviceReferences[0]);
                    } else {
                        String msg = "No service found for " + destinationResolverRef +
                            " with filter [" + filter + "]";
                        LOG.error(msg);
                        throw new ConfigurationException(destinationResolverRef, msg);
                    }
                }

                return config;
            }
        }

        LOG.info("No Broker configuration found in " + pid + " for " + prefix);
        return null;
    }

    void addServiceReference(String pid, ServiceReference serviceReference) {
        List<ServiceReference> serviceReferences = serviceReferenceMap.get(pid);
        if (serviceReferences != null) {
            serviceReferences.add(serviceReference);
        } else {
            ArrayList<ServiceReference> references = new ArrayList<ServiceReference>();
            references.add(serviceReference);
            serviceReferenceMap.put(pid, references);
        }
    }

    protected final ApplicationContext createApplicationContext(String pid) {
        // create a dummy Spring ApplicationContext that will get beans from OSGi service registry
        return (ApplicationContext) Proxy.newProxyInstance(ApplicationContext.class.getClassLoader(),
            new Class[]{ApplicationContext.class},
            new OsgiApplicationContextAdapter(pid, this));
    }

    public CuratorFramework getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(CuratorFramework zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    public final FabricService getFabricService() {
        return fabricService;
    }

    public final void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public final BundleContext getBundleContext() {
        return bundleContext;
    }

    public final void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
