/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.cxf;

import io.fabric8.common.util.PublicPortMapper;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerLifeCycleListener;
import io.fabric8.groups.Group;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class FabricServerListener implements ServerLifeCycleListener {
    private static final transient Log LOG = LogFactory.getLog(FabricServerListener.class);
    private final Group<CxfNodeState> group;
    private ServerAddressResolver addressResolver;
    private final CuratorFramework curator;
    private final List<String> services = new ArrayList<String>();

    public FabricServerListener(Group<CxfNodeState> group, ServerAddressResolver addressResolver, CuratorFramework curator) {
        this.group = group;
        this.addressResolver = addressResolver;
        this.curator = curator;
    }

    public FabricServerListener(Group<CxfNodeState> group) {
        this(group, null, null);
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
        String answer;
        if (addressResolver != null) {
            answer = addressResolver.getFullAddress(address);
        } else {
            answer = address;
        }
        if (isFullAddress(address)) {
            answer = toPublicAddress(address);
        }

        return answer;
    }

    protected boolean isFullAddress(String address) {
        return address.startsWith("http:") || address.startsWith("https:") || address.contains("://");
    }

    /**
     * Uses a port mapper to correctly convert to public address:port (e.g. in OpenShift environment)
     *
     * @param address
     * @return
     */
    private String toPublicAddress(String address) {
        try {
            String containerId = System.getProperty("karaf.name");
            if (containerId == null || containerId.trim().equals("")) {
                return address;
            }
            URI uri = new URI(address);
            int port = PublicPortMapper.getPublicPort(uri.getPort());
            String answer;
            String path = uri.getPath();
            while (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (curator != null) {
                String hostname = "${zk:" + containerId + "/ip}";
                answer =  uri.getScheme() + "://" + hostname + ":" + port + "/" + path;
                curator.getZookeeperClient().blockUntilConnectedOrTimedOut();
                answer = ZooKeeperUtils.getSubstitutedData(curator, answer);
            } else {
                answer =  uri.getScheme() + "://" + uri.getHost() + ":" + port + "/" + path;
            }
            return answer;
        } catch (InterruptedException e) {
            LOG.warn("Could not connect to Zookeeper to get public container address");
            return address;
        } catch (URISyntaxException e) {
            LOG.warn("Could not map URL to a public address: " + address);
            return address;
        }
    }

}
