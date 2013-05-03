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
package org.fusesource.fabric.web;

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperRetriableUtils;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class FabricWebRegistrationHandler implements WebListener, LifecycleListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricWebRegistrationHandler.class);

    private final Map<Bundle, WebEvent> bundleEvents = new HashMap<Bundle, WebEvent>();
    private FabricService fabricService;
    private IZKClient zooKeeper;

    @Override
    public void webEvent(WebEvent webEvent) {
        bundleEvents.put(webEvent.getBundle(), webEvent);
        switch (webEvent.getType()) {
            case WebEvent.DEPLOYING:
                break;
            case WebEvent.DEPLOYED:
                registerWebapp(fabricService.getCurrentContainer(), webEvent);
                break;
            default:
                unRegisterWebapp(fabricService.getCurrentContainer(), webEvent);
        }
    }

    /**
     * Registers a webapp to the registry.
     * @param container
     * @param webEvent
     */
    void registerWebapp(Container container, WebEvent webEvent) {
        String id = container.getId();
        String url = container.getHttpUrl() + webEvent.getContextPath();

        if (!url.startsWith("http")) {
            url = "http://" + url;
        }

        String name = webEvent.getBundle().getSymbolicName();

        if (name.equals("org.jolokia")) {
            container.setJolokiaUrl(url);
            System.setProperty("jolokia.agent", url);
        }

        String json = "{\"id\":\"" + id + "\", \"services\":[\"" + url + "\"],\"container\":\"" + id + "\"}";
        try {
            ZooKeeperRetriableUtils.set(zooKeeper, ZkPath.WEBAPPS_CONTAINER.getPath(name,
                                        webEvent.getBundle().getVersion().toString(), id), json);
        } catch (Exception e) {
            LOGGER.error("Failed to register webapp {}.", webEvent.getContextPath(), e);
        }
    }


    /**
     * Unregister a webapp from the registry.
     * @param container
     * @param webEvent
     */
    void unRegisterWebapp(Container container, WebEvent webEvent) {
        try {
            zooKeeper.delete(ZkPath.WEBAPPS_CONTAINER.getPath(webEvent.getBundle().getSymbolicName(),
                             webEvent.getBundle().getVersion().toString(), container.getId()));
        } catch (Exception e) {
            LOGGER.error("Failed to unregister webapp {}.", webEvent.getContextPath(), e);
        }
    }

    @Override
    public void onConnected() {
        for (Map.Entry<Bundle, WebEvent> entry : bundleEvents.entrySet()) {
            webEvent(entry.getValue());
        }
    }

    @Override
    public void onDisconnected() {
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public IZKClient getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
    }
}
