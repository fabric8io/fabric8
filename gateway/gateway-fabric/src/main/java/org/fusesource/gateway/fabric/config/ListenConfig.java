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
import org.fusesource.common.util.Strings;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.gateway.ServiceMap;
import org.fusesource.gateway.fabric.FabricGateway;
import org.fusesource.gateway.fabric.GatewayListener;
import org.fusesource.gateway.handlers.Gateway;
import org.fusesource.gateway.handlers.http.HttpGateway;
import org.fusesource.gateway.handlers.tcp.TcpGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;

/**
 * Represents a listener
 */
public class ListenConfig {
    private static final transient Logger LOG = LoggerFactory.getLogger(ListenConfig.class);

    private int port;
    private String host;
    private String protocol;

    @Override
    public String toString() {
        return "ListenConfig{" +
                "protocol='" + protocol + '\'' +
                ", port=" + port +
                ", host='" + host + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ListenConfig that = (ListenConfig) o;

        if (port != that.port) return false;
        if (host != null ? !host.equals(that.host) : that.host != null) return false;
        if (protocol != null ? !protocol.equals(that.protocol) : that.protocol != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = port;
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + (protocol != null ? protocol.hashCode() : 0);
        return result;
    }

    public boolean isWebProtocol() {
        return protocol == null || protocol.equals("http") || protocol.equals("https");
    }


    /**
     * Factory method to create a new gateway for this configuration
     */
    public Gateway createGateway(FabricGateway owner, ServiceMap serviceMap) {
        Vertx vertx = owner.getVertx();
        Gateway answer;
        if (isWebProtocol()) {
            answer = new HttpGateway(vertx, serviceMap, port);
        } else {
            answer = new TcpGateway(vertx, serviceMap, port, protocol);
        }
        if (Strings.isNotBlank(host)) {
            answer.setHost(host);
        }
        return answer;
    }

    // Properties
    //-------------------------------------------------------------------------

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
