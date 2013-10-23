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
package org.fusesource.gateway.fabric.config;

import org.apache.curator.framework.CuratorFramework;
import org.fusesource.gateway.ServiceMap;
import org.fusesource.gateway.fabric.FabricGateway;
import org.fusesource.gateway.fabric.GatewayListener;
import org.fusesource.gateway.handlers.Gateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The Gateway configuration
 */
public class GatewayConfig {
    private static final transient Logger LOG = LoggerFactory.getLogger(GatewayConfig.class);

    private String zooKeeperPath;
    private List<ListenConfig> listeners = new ArrayList<ListenConfig>();

    @Override
    public String toString() {
        return "GatewayConfig{" +
                "zooKeeperPath='" + zooKeeperPath + '\'' +
                ", listeners=" + listeners +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GatewayConfig that = (GatewayConfig) o;

        if (listeners != null ? !listeners.equals(that.listeners) : that.listeners != null) return false;
        if (zooKeeperPath != null ? !zooKeeperPath.equals(that.zooKeeperPath) : that.zooKeeperPath != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = zooKeeperPath != null ? zooKeeperPath.hashCode() : 0;
        result = 31 * result + (listeners != null ? listeners.hashCode() : 0);
        return result;
    }


    /**
     * Factory method to create a {@link org.fusesource.gateway.fabric.GatewayListener} from
     * the configuration
     */
    public GatewayListener createListener(FabricGateway owner) {
        String zkPath = getZooKeeperPath();
        if (zkPath == null) {
            LOG.warn("No ZooKeeperPath for listener so ignoring " + this);
            return null;
        }
        ServiceMap serviceMap = new ServiceMap();
        
        List<Gateway> gateways = new ArrayList<Gateway>();
        addGateways(gateways, owner, serviceMap);
        if (gateways.isEmpty()) {
            return null;
        }
        CuratorFramework curator = owner.getCurator();
        return new GatewayListener(curator, zkPath, serviceMap, gateways);
    }

    protected void addGateways(List<Gateway> gateways, FabricGateway owner, ServiceMap serviceMap) {
        List<ListenConfig> list = getListeners();
        for (ListenConfig listenConfig : list) {
            Gateway gateway = listenConfig.createGateway(owner, serviceMap);
            if (gateway != null) {
                gateways.add(gateway);
            }
        }
    }


    // Properties
    //-------------------------------------------------------------------------

    public String getZooKeeperPath() {
        return zooKeeperPath;
    }

    public void setZooKeeperPath(String zooKeeperPath) {
        this.zooKeeperPath = zooKeeperPath;
    }

    public List<ListenConfig> getListeners() {
        return listeners;
    }

    public void setListeners(List<ListenConfig> listeners) {
        this.listeners = listeners;
    }
}
