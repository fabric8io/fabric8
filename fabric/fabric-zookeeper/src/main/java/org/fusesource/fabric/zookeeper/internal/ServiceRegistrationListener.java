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
package org.fusesource.fabric.zookeeper.internal;

import java.util.Properties;

import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.url.URLStreamHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceRegistrationListener implements LifecycleListener, ZooKeeperAware {

    private transient Logger logger = LoggerFactory.getLogger(ServiceRegistrationListener.class);

    private IZKClient zooKeeper;
    private String zooKeeperUrl;
    private BundleContext bundleContext;
    private ServiceRegistration clientRegistration;
    private ServiceRegistration handlerRegistration;

    public IZKClient getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    public String getZooKeeperUrl() {
        return zooKeeperUrl;
    }

    public void setZooKeeperUrl(String zooKeeperUrl) {
        this.zooKeeperUrl = zooKeeperUrl;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public synchronized void onConnected() {
        Properties props = new Properties();
        props.put("url", zooKeeperUrl);
        clientRegistration = bundleContext.registerService(IZKClient.class.getName(), zooKeeper, props);
        props = new Properties();
        props.put("url", zooKeeperUrl);
        props.put("url.handler.protocol", "zk");
        handlerRegistration = bundleContext.registerService(URLStreamHandlerService.class.getName(),
                new ZkUrlHandler(zooKeeper), props);
    }

    public synchronized void onDisconnected() {
        if (clientRegistration != null) {
            try {
                clientRegistration.unregister();
            } catch (Exception e) {
                logger.warn("An error occured during service unregistration", e);
            } finally {
                clientRegistration = null;
            }
        }
        if (handlerRegistration != null) {
            try {
                handlerRegistration.unregister();
            } catch (Exception e) {
                logger.warn("An error occured during service unregistration", e);
            } finally {
                handlerRegistration = null;
            }
        }
    }
}
