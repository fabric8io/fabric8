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
package io.fabric8.jaas;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.BackingEngineFactory;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ThreadSafe
@Component(name = "io.fabric8.jaas.zookeeper.backingengine", label = "Fabric8 Jaas Backing Engine Factory", metatype = false)
@Service(BackingEngineFactory.class)
public final class ZookeeperBackingEngineFactory extends AbstractComponent implements BackingEngineFactory {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ZookeeperBackingEngineFactory.class);

    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public String getModuleClass() {
        return ZookeeperLoginModule.class.getName();
    }

    @Override
    public BackingEngine build(Map options) {
        assertValid();
        ZookeeperBackingEngine engine = null;
        EncryptionSupport encryptionSupport = new BasicEncryptionSupport(options);
        String path = (String) options.get("path");
        if (path == null) {
            path = ZookeeperBackingEngine.USERS_NODE;
        }
        try {
            ZookeeperProperties users = new ZookeeperProperties(curator.get(), path);
            users.load();
            engine = new ZookeeperBackingEngine(users, encryptionSupport);
        } catch (Exception e) {
            LOGGER.warn("Cannot initialize engine", e);
        }
        return engine;
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }
}
