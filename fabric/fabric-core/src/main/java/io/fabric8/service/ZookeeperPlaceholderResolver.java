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
package io.fabric8.service;

import io.fabric8.api.FabricException;
import io.fabric8.api.FabricService;
import io.fabric8.api.PlaceholderResolver;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.zookeeper.ZkPath;

import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
@Component(name = "io.fabric8.placholder.resolver.zookeeper", label = "Fabric8 ZooKeeper Placeholder Resolver", metatype = false)
@Service({ PlaceholderResolver.class, ZookeeperPlaceholderResolver.class })
@Properties({ @Property(name = "scheme", value = ZookeeperPlaceholderResolver.RESOLVER_SCHEME) })
public final class ZookeeperPlaceholderResolver extends AbstractComponent implements PlaceholderResolver {

    public static final String RESOLVER_SCHEME = "zk";

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperPlaceholderResolver.class);

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public String getScheme() {
        return RESOLVER_SCHEME;
    }

    @Override
    public String resolve(FabricService fabricService, Map<String, Map<String, String>> configs, String pid, String key, String value) {
        try {
            return new String(ZkPath.loadURL(fabricService.adapt(CuratorFramework.class), value), "UTF-8");
        } catch (KeeperException.NoNodeException e) {
            LOGGER.warn("Could not load property value: {}. Ignoring.", value, e);
            return "";
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }
}
