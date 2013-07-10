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
package org.fusesource.fabric.jaas;

import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.BackingEngineFactory;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.service.FabricServiceImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ZookeeperBackingEngineFactory implements BackingEngineFactory {

    protected BundleContext bundleContext;

    protected FabricService service;

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ZookeeperBackingEngineFactory.class);

    @Override
    public String getModuleClass() {
        return ZookeeperLoginModule.class.getName();
    }

    @Override
    public BackingEngine build(Map options) {
        ZookeeperBackingEngine engine = null;
        this.bundleContext = (BundleContext) options.get(BundleContext.class.getName());
        EncryptionSupport encryptionSupport = new BasicEncryptionSupport(options);
        if (bundleContext != null) {
            ServiceReference ref = bundleContext.getServiceReference(FabricService.class.getName());
            service = (FabricServiceImpl) bundleContext.getService(ref);
            encryptionSupport = new EncryptionSupport(options);
        }
        String path = (String)options.get("path");
        if (path == null) {
            path = ZookeeperBackingEngine.USERS_NODE;
        }
        try {
            ZookeeperProperties users = new ZookeeperProperties(((FabricServiceImpl)service).getCurator(), path);
            users.load();
            engine = new ZookeeperBackingEngine(users, encryptionSupport);
        } catch (Exception e) {
            LOGGER.warn("Cannot initialize engine", e);
        } finally {
            return engine;
        }
    }

    public void setService(FabricService service) {
        this.service = service;

    }
}
