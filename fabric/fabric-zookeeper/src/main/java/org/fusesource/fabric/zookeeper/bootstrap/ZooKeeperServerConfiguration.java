package org.fusesource.fabric.zookeeper.bootstrap;

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

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.api.scr.ValidatingReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
@Component(name = "org.fusesource.fabric.zookeeper.server.config", configurationPid="org.fusesource.fabric.zookeeper.server", policy = ConfigurationPolicy.REQUIRE, immediate = true)
@Service(ZooKeeperServerConfiguration.class)
public class ZooKeeperServerConfiguration extends AbstractComponent {

    final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Reference(referenceInterface = BootstrapConfiguration.class)
    private final ValidatingReference<BootstrapConfiguration> bootstrapConfiguration = new ValidatingReference<BootstrapConfiguration>();

    private Map<String, ?> configuration;
    private QuorumPeerConfig peerConfig;
    private ServerConfig serverConfig;

    @Activate
    void activate(Map<String, ?> config) throws IOException {

        configuration = Collections.unmodifiableMap(config);
        Properties props = new Properties();
        for (Entry<String, ?> entry : config.entrySet()) {
            props.put(entry.getKey(), entry.getValue());
        }

        try {
            peerConfig = new QuorumPeerConfig();
            peerConfig.parseProperties(props);
            LOGGER.info("Created zookeeper peer configuration: {}", peerConfig);
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot create peer config from: " + props);
        }

        try {
            serverConfig = new ServerConfig();
            serverConfig.readFrom(peerConfig);
            LOGGER.info("Created zookeeper server configuration: {}", serverConfig);
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot create server config from: " + props);
        }

        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    public Map<String, ?> getConfiguration() {
        assertValid();
        return configuration;
    }

    public QuorumPeerConfig getPeerConfig() {
        assertValid();
        return peerConfig;
    }

    public ServerConfig getServerConfig() {
        assertValid();
        return serverConfig;
    }

    void bindBootstrapConfiguration(BootstrapConfiguration service) {
        this.bootstrapConfiguration.bind(service);
    }

    void unbindBootstrapConfiguration(BootstrapConfiguration service) {
        this.bootstrapConfiguration.unbind(service);
    }
}
