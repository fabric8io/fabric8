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
package org.fusesource.fabric.cxf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerLifeCycleListener;
import org.fusesource.fabric.groups.Group;

public class FabricServerListener implements ServerLifeCycleListener {
    private static final transient Log LOG = LogFactory.getLog(FabricServerListener.class);
    private final Group group;
    private String eid;
    private ServerAddressResolver addressResolver;

    public FabricServerListener(Group group, ServerAddressResolver addressResolver) {
        this.group = group;
        this.addressResolver = addressResolver;
    }

    public FabricServerListener(Group group) {
        this(group, null);
    }

    public void startServer(Server server) {
        // get the server address
        String address = getFullAddress(server.getEndpoint().getEndpointInfo().getAddress());
        if (LOG.isDebugEnabled()) {
            LOG.debug("The CXF server is start with address " + address);
        }
        try {
            eid = group.join(address.getBytes("UTF-8"));
        } catch (Exception ex) {
            LOG.warn("Cannot bind the address " + address + " to the group, due to ", ex);
        }
    }

    public void stopServer(Server server) {
        // get the server address
        String address = getFullAddress(server.getEndpoint().getEndpointInfo().getAddress());
        if (LOG.isDebugEnabled()) {
            LOG.debug("The CXF server is stopped with address " + address);
        }
        group.leave(eid);
    }

    public String getFullAddress(String address) {
        // Current CXF only supports these two schema
        if (!(address.startsWith("http") || address.startsWith("jms"))) {
            // we need to update the address with the prefixAddress as the Service is published from Servlet
            if (addressResolver == null) {
                LOG.warn("Cannot find a full address for the CXF service of " + address + " , due to lack of the configuration of ServerAddressResolver");
                // TODO do we need to throw exception here
                return address;
            }
            return addressResolver.getFullAddress(address);
        } else {
            return address;
        }
    }
}
