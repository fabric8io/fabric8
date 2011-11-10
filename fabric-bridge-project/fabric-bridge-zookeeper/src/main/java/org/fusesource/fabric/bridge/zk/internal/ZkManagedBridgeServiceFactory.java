/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.bridge.zk.internal;

import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.bridge.model.BridgeDestinationsConfig;
import org.fusesource.fabric.bridge.model.BrokerConfig;
import org.fusesource.fabric.bridge.zk.ZkBridgeConnector;
import org.fusesource.fabric.bridge.zk.model.ZkBridgeDestinationsConfigFactory;
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

import javax.jms.ConnectionFactory;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * A {@link ManagedServiceFactory} for creating {@link ZkBridgeConnector ZkBridgeConnectors}.
 *
 * @author Dhiraj Bokde
 */
public class ZkManagedBridgeServiceFactory implements ManagedServiceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ZkManagedBridgeServiceFactory.class);

    private static final String CONNECTION_FACTORY_CLASS_NAME = ConnectionFactory.class.getName();
    private static final String DESTINATION_RESOLVER_CLASS_NAME = DestinationResolver.class.getName();

    private FabricService fabricService;

    private BundleContext bundleContext;

    private Map<String, ZkBridgeConnector> bridgeConnectorMap = new ConcurrentHashMap<String, ZkBridgeConnector>();
    private Map<String, List<ServiceReference>> serviceReferenceMap = new ConcurrentHashMap<String, List<ServiceReference>>();

    public void init() throws Exception {
        if (fabricService == null) {
            throw new IllegalArgumentException("Property fabricService must be set!");
        }
        if (bundleContext == null) {
            throw new IllegalArgumentException("Property bundleContext must be set!");
        }

        LOG.info("Started");
    }

    public void destroy() throws Exception {
        // destroy all running bridges
        for (String pid : bridgeConnectorMap.keySet()) {
            deleted(pid);
        }

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

    @Override
    public String getName() {
        return "Fabric Bridge Server";
    }

    @Override
    public void updated(String pid, Dictionary incoming) throws ConfigurationException {

        if (bridgeConnectorMap.containsKey(pid)) {
            // destroy and recreate bridge connector
            deleted(pid);
        }

        Dictionary<String,String> properties = new Hashtable<String, String>();
        for (Enumeration keys = incoming.keys(); keys.hasMoreElements(); ) {
            String key = (String) keys.nextElement();
            Object value = incoming.get(key);
            if (value != null) {
                properties.put(key, value.toString());
            }
        }

        // create and add bridge connector
        bridgeConnectorMap.put(pid, createBridgeConnector(pid, properties));

    }

    @Override
    public void deleted(String pid) {

        ZkBridgeConnector bridgeConnector = bridgeConnectorMap.remove(pid);
        if (bridgeConnector != null) {

            try {
                bridgeConnector.destroy();
            } catch (Exception e) {
                LOG.error("Error destroying bridge " + pid + " : " + e.getMessage(), e);
            }

            // unget service references for this pid
            if (serviceReferenceMap.containsKey(pid)) {
                for (ServiceReference reference : serviceReferenceMap.remove(pid)) {
                    bundleContext.ungetService(reference);
                }
            }

        } else {
            LOG.error("Bridge " + pid + " not found");
        }

    }

    private ZkBridgeConnector createBridgeConnector(String pid, Dictionary<String, String> properties) throws ConfigurationException {
        ZkBridgeConnector bridgeConnector = new ZkBridgeConnector();
        bridgeConnector.setFabricService(this.fabricService);
        bridgeConnector.setId(pid);

        // populate bridge properties
        if (StringUtils.hasText(properties.get("versionName"))) {
            bridgeConnector.setVersionName(properties.get("versionName"));
        }
        if (StringUtils.hasText(properties.get("gatewayProfileName"))) {
            bridgeConnector.setGatewayProfileName(properties.get("gatewayProfileName"));
        }
        if (StringUtils.hasText(properties.get("gatewayConnectRetries"))) {
            bridgeConnector.setGatewayConnectRetries(Integer.parseInt(properties.get("gatewayConnectRetries")));
        }
        if (StringUtils.hasText(properties.get("gatewayStartupDelay"))) {
            bridgeConnector.setGatewayStartupDelay(Integer.parseInt(properties.get("gatewayStartupDelay")));
        }
        if (StringUtils.hasText(properties.get("inboundDestinationsRef"))) {
            bridgeConnector.setInboundDestinations(createDestinationsConfig(pid, properties.get("inboundDestinationsRef")));
        }
        if (StringUtils.hasText(properties.get("outboundDestinationsRef"))) {
            bridgeConnector.setOutboundDestinations(createDestinationsConfig(pid, properties.get("outboundDestinationsRef")));
        }

        bridgeConnector.setLocalBrokerConfig(createBrokerConfig(pid, "localBroker", properties));
        bridgeConnector.setRemoteBrokerConfig(createBrokerConfig(pid, "remoteBroker", properties));
        bridgeConnector.setExportedBrokerConfig(createBrokerConfig(pid, "exportedBroker", properties));

        // create a dummy Spring ApplicationContext that will get beans from OSGi service registry
        ApplicationContext applicationContext = (ApplicationContext) Proxy.newProxyInstance(ApplicationContext.class.getClassLoader(),
            new Class[]{ApplicationContext.class},
            new SpringOsgiBeanBridge(pid, this));

        bridgeConnector.setApplicationContext(applicationContext);

        try {
            bridgeConnector.afterPropertiesSet();
            bridgeConnector.start();
        } catch (Exception e) {
            String msg = "Error starting bridge " + pid + " : " + e.getMessage();
            LOG.error(msg);
            throw new ConfigurationException("Start", msg, e);
        }

        return bridgeConnector;

    }

    // lookup destinationsRef using ZkBridgeDestinationsConfigFactory
    private BridgeDestinationsConfig createDestinationsConfig(String pid, String destinationsRef) throws ConfigurationException {

        ZkBridgeDestinationsConfigFactory factory = new ZkBridgeDestinationsConfigFactory();
        factory.setFabricService(fabricService);
        factory.setId(destinationsRef);
        try {
            return factory.getObject();
        } catch (Exception e) {
            String msg = "Error getting destinations for " +
                pid + "." + destinationsRef + " : " + e.getMessage();
            LOG.error(msg, e);
            throw new ConfigurationException(destinationsRef, msg, e);
        }

    }

    private BrokerConfig createBrokerConfig(String pid, String prefix, Dictionary<String, String> properties) throws ConfigurationException {

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

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
