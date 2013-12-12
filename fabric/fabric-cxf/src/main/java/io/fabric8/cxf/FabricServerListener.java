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
package io.fabric8.cxf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerLifeCycleListener;
import io.fabric8.groups.Group;

import java.util.ArrayList;
import java.util.List;

public class FabricServerListener implements ServerLifeCycleListener {
    private static final transient Log LOG = LogFactory.getLog(FabricServerListener.class);
    private final Group<CxfNodeState> group;
    private ServerAddressResolver addressResolver;
    private final List<String> services = new ArrayList<String>();

    public FabricServerListener(Group<CxfNodeState> group, ServerAddressResolver addressResolver) {
        this.group = group;
        this.addressResolver = addressResolver;
    }

    public FabricServerListener(Group<CxfNodeState> group) {
        this(group, null);
    }

    public void startServer(Server server) {
        // get the server address
        String address = getFullAddress(server.getEndpoint().getEndpointInfo().getAddress());
        if (LOG.isDebugEnabled()) {
            LOG.debug("The CXF server is start with address " + address);
        }
        services.add(address);
        group.update(createState());
    }

    public void stopServer(Server server) {
        // get the server address
        String address = getFullAddress(server.getEndpoint().getEndpointInfo().getAddress());
        if (LOG.isDebugEnabled()) {
            LOG.debug("The CXF server is stopped with address " + address);
        }
        services.remove(address);
        group.update(createState());
    }

    private CxfNodeState createState() {
        CxfNodeState state = new CxfNodeState("cxf");
        state.services = services.toArray(new String[services.size()]);
        return state;
    }

    public String getFullAddress(String address) {
        if (addressResolver != null) {
            return addressResolver.getFullAddress(address);
        } else {
            return address;
        }
    }
}
