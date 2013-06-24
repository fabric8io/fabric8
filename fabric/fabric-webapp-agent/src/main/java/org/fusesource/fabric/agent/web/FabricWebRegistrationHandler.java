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
package org.fusesource.fabric.agent.web;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.fusesource.fabric.api.FabricService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabricWebRegistrationHandler implements ConnectionStateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricWebRegistrationHandler.class);

    private FabricService fabricService;
    private CuratorFramework curator;


    public void init() {
        LOGGER.info("Initialising " + this + " with fabricService: " + fabricService + " and curator: " + curator);
    }

    public void destroy() {
    }

    /**
     * Registers a webapp to the registry.
     */
    void registerWebapp() {
        String id = "";
        String url = "";


        // Catalina:type=Connector,port=8080

        // TODO lets query the HTTP port...



/*
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
*/

/*
        String json = "{\"id\":\"" + id + "\", \"services\":[\"" + url + "\"],\"container\":\"" + id + "\"}";
        try {
            setData(curator, ZkPath.WEBAPPS_CONTAINER.getPath(name,
                    webEvent.getBundle().getVersion().toString(), id), json, CreateMode.EPHEMERAL);
        } catch (Exception e) {
            LOGGER.error("Failed to register webapp {}.", webEvent.getContextPath(), e);
        }
*/
    }


    /**
     * Unregister a webapp from the registry.
     */
    void unRegisterWebapp() {
/*
        try {
            String name = webEvent.getBundle().getSymbolicName();
            if (name.equals("org.jolokia")) {
                container.setJolokiaUrl(null);
                System.clearProperty("jolokia.agent");
            }

            delete(curator, ZkPath.WEBAPPS_CONTAINER.getPath(name,
                    webEvent.getBundle().getVersion().toString(), container.getId()));
        } catch (KeeperException.NoNodeException e) {
            // If the node does not exists, ignore the exception
        } catch (Exception e) {
            LOGGER.error("Failed to unregister webapp {}.", webEvent.getContextPath(), e);
        }
*/
    }

    @Override
    public void stateChanged(CuratorFramework client, ConnectionState newState) {
        switch (newState) {
            case CONNECTED:
            case RECONNECTED:
                this.curator = client;
                onConnected();
                break;
            default:
                onDisconnected();
                this.curator = null;
                break;
        }
    }

    public void onConnected() {
/*
        for (WebEvent event : webEvents.values()) {
            webEvent(event);
        }
        for (Map<String, ServletEvent> map : servletEvents.values()) {
            for (ServletEvent event : map.values()) {
                servletEvent(event);
            }
        }
*/
    }

    public void onDisconnected() {
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }
}
