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

package org.elasticsearch.pojo;

import java.io.UnsupportedEncodingException;
import java.util.*;

import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.http.HttpServer;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalNode;
import org.fusesource.fabric.groups.ChangeListener;
import org.fusesource.fabric.groups.Group;
import org.fusesource.fabric.groups.ZooKeeperGroupFactory;
import org.linkedin.zookeeper.client.IZKClient;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

public class NodeFactory implements ManagedServiceFactory {

    private BundleContext bundleContext;
    private Map<String,String> settings;
    private Map<String, Node> nodes = new HashMap<String, Node>();
    private Map<String, ServiceRegistration> services = new HashMap<String, ServiceRegistration>();
    private boolean destroyed = false;
    private IZKClient zookeeper;
    private List<ACL> acl = ZooDefs.Ids.OPEN_ACL_UNSAFE;
    private Group group;
    private String eid;


    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public IZKClient getZookeeper() {
        return zookeeper;
    }

    public void setZookeeper(IZKClient zookeeper) {
        this.zookeeper = zookeeper;
    }

    public List<ACL> getAcl() {
        return acl;
    }

    public void setAcl(List<ACL> acl) {
        this.acl = acl;
    }

    public String getName() {
        return "ElasticSearch Node factory";
    }

    public synchronized void updated(String pid, Dictionary properties) throws ConfigurationException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(InternalNode.class.getClassLoader());
            deleted(pid);
            if (properties != null && !destroyed) {
                ImmutableSettings.Builder builder = ImmutableSettings.settingsBuilder();
                builder.put(settings);
                if (properties != null) {
                    for (Enumeration e = properties.keys(); e.hasMoreElements();) {
                        String key = e.nextElement().toString();
                        Object oval = properties.get(key);
                        String val = oval != null ? oval.toString() : null;
                        builder.put(key, val);
                    }
                }
                Node node = new InternalNode(builder.build(), false);
                node.start();
                
                node.client().admin().cluster().prepareHealth().setWaitForGreenStatus().execute().actionGet();
                this.services.put(pid, this.bundleContext.registerService(Node.class.getName(), node, null));
                this.nodes.put(pid, node);

                //
                // If the node opened an HTTP url, then register it in Fabric so that other
                // fabric nodes can use the HTTP service.
                //
                String fabricUrl = null;
                if( node instanceof InternalNode ) {
                    InternalNode in = (InternalNode)node;
                    try {
                        HttpServerTransport transport = in.injector().getInstance(HttpServerTransport.class);
                        TransportAddress published = transport.boundAddress().publishAddress();
                        if(published instanceof InetSocketTransportAddress) {
                            InetSocketTransportAddress address = (InetSocketTransportAddress)published;
                            fabricUrl = "http://"+address.address().getAddress().getHostAddress()+":"+address.address().getPort();
                        }
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
                if( fabricUrl!=null ) {
                    if( group==null ) {
                        group = ZooKeeperGroupFactory.create(zookeeper, "/fabric/registry/clusters/elastic-search", acl);
                    }
                    try {
                        eid = group.join(fabricUrl.getBytes("UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }

            }
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    public synchronized void deleted(String pid) {
        Node node = nodes.remove(pid);
        if (node != null) {
            if( group!=null ) {
                group.leave(eid);
            }

            ServiceRegistration reg = this.services.remove(pid);
            if (reg != null) {
                reg.unregister();
            }
            node.close();
        }
    }

    public synchronized void destroy() {
        destroyed = true;
        while (!nodes.isEmpty()) {
            String pid = nodes.keySet().iterator().next();
            deleted(pid);
        }
        if( group!=null ) {
            group.close();
        }
    }

}
