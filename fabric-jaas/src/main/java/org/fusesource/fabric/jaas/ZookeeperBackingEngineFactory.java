/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.jaas;

import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.BackingEngineFactory;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;
import org.apache.karaf.jaas.modules.properties.PropertiesBackingEngine;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.service.FabricServiceImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ZookeeperBackingEngineFactory implements BackingEngineFactory {

    protected BundleContext bundleContext;

    protected FabricServiceImpl service;

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ZookeeperBackingEngineFactory.class);

    @Override
    public String getModuleClass() {
        return ZookeeperLoginModule.class.getName();
    }

    @Override
    public BackingEngine build(Map options) {
        ZookeeperBackingEngine engine = null;
        this.bundleContext = (BundleContext) options.get(BundleContext.class.getName());
        ServiceReference ref = bundleContext.getServiceReference(FabricService.class.getName());
        service = (FabricServiceImpl) bundleContext.getService(ref);
        String path = (String)options.get("path");
        if (path == null) {
            path = ZookeeperBackingEngine.USERS_NODE;
        }
        try {
            ZookeeperProperties users = new ZookeeperProperties(service.getZooKeeper(), path);
            EncryptionSupport encryptionSupport = new EncryptionSupport(options);
            engine = new ZookeeperBackingEngine(users, encryptionSupport);
        } catch (Exception e) {
            LOGGER.warn("Cannot initialize engine", e);
        } finally {
            return engine;
        }
    }
}
