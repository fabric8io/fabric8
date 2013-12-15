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

package io.fabric8.virt.commands;

import java.util.LinkedList;
import java.util.List;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;

public abstract class LibvirtCommandSupport extends OsgiCommandSupport {

    private List<Connect> connections = new LinkedList<Connect>();

    @Option(name = "--type", multiValued = false, required = false)
    private String type;

    @Option(name = "--url", multiValued = false, required = false)
    private String url;

    @Override
    protected abstract Object doExecute() throws Exception;


    protected Connect getConnection() throws LibvirtException {
        if (url != null) {
            Connect connect = null;
            for (Connect conn : connections) {
                if (type.equals(conn.getURI().startsWith(url))) {
                    connect = conn;
                    break;
                }
            }
            if (connect == null) {
                throw new IllegalArgumentException("Connection with url " + url + " not found");
            }
            return connect;
        } else if (type != null) {
            Connect connect = null;
            for (Connect conn : connections) {
                if (type.equals(connect.getType())) {
                    connect = conn;
                    break;
                }
            }
            if (connect == null) {
                throw new IllegalArgumentException("Connection type " + type + " not found");
            }
            return connect;
        } else {
            if (connections.size() != 1) {
                StringBuilder sb = new StringBuilder();
                for (Connect conn : connections) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(conn.getURI());
                }
                throw new IllegalArgumentException("Multiple connections are present, please select one using the --type /--url argument of the following connections: " + sb.toString());
            }
            return connections.get(0);
        }
    }

    public List<Connect> getConnections() {
        return connections;
    }

    public void setConnections(List<Connect> connections) {
        this.connections = connections;
    }
}
