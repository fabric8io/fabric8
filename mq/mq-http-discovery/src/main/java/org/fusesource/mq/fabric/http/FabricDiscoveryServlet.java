/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.mq.fabric.http;

import org.apache.curator.framework.CuratorFramework;
import org.codehaus.jackson.annotate.JsonProperty;
import io.fabric8.groups.NodeState;
import io.fabric8.groups.internal.ZooKeeperGroup;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FabricDiscoveryServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(FabricDiscoveryServlet.class);

    volatile  CuratorFramework curator = null;
    long cacheTimeout = 1000;
    ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<String, CacheEntry>();

    static class CacheEntry {
        long timestamp;
        String result;

        CacheEntry(String result, long timestamp) {
            this.result = result;
            this.timestamp = timestamp;
        }
    }

    static class ActiveMQNode extends NodeState {
        @JsonProperty
        String[] services;
    }

    public FabricDiscoveryServlet() {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        CuratorFramework  curator = this.curator;
        if( curator==null ) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Not attached to Fabric");
            return;
        }

        try {
            String groupName = req.getPathInfo();
            if( groupName==null ) {
                groupName = "";
            }
            if( groupName.startsWith("/") ) {
                groupName = groupName.substring(1);
            }

            LOG.debug("discovery request for group name="+groupName);

            // To avoid hammering ZooKeeper if we get to many HTTP requests back to back,
            // lets cache results.
            CacheEntry cacheEntry = cache.get(groupName);
            long now = System.currentTimeMillis();
            if( cacheEntry==null || cacheEntry.timestamp+cacheTimeout < now ) {

                try {
                    Map<String, ActiveMQNode> members = ZooKeeperGroup.members(curator, "/fabric/registry/clusters/fusemq/" + groupName, ActiveMQNode.class);
                    HashSet<String> masters = new HashSet<String>();
                    StringBuilder buff = new StringBuilder();

                    for (ActiveMQNode node : members.values()) {
                        if( !masters.contains(node.getId()) ) {
                            for (int i = 0; i < node.services.length; i++) {
                                String url = node.services[i];
                                url = ZooKeeperUtils.getSubstitutedData(curator, url);
                                buff.append(url);
                                buff.append('\n');
                            }
                            masters.add(node.getId());
                        }
                    }

                    cacheEntry = new CacheEntry(buff.toString(), now);
                } catch (Exception e) {
                    cacheEntry = new CacheEntry(null, now);
                }

                cache.put(groupName, cacheEntry);
            }

            if( cacheEntry.result !=null ) {
                resp.getWriter().print(cacheEntry.result.toString());
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Group not found");
            }

        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error occurred: "+e);
        }
    }

    public long getCacheTimeout() {
        return cacheTimeout;
    }

    public void setCacheTimeout(long cacheTimeout) {
        this.cacheTimeout = cacheTimeout;
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

}
