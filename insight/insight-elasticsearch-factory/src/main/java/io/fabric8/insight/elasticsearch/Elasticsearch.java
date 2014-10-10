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
package io.fabric8.insight.elasticsearch;

import io.fabric8.common.util.JMXUtils;
import org.apache.felix.scr.annotations.*;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsResponse;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.node.Node;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component(immediate = true)
@Service({ElasticsearchMBean.class})
public class Elasticsearch implements ElasticsearchMBean {

    @Reference
    private MBeanServer mbeanServer;

    @Reference(name = "node", referenceInterface = org.elasticsearch.node.Node.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
    private final Map<String, Set<Node>> nodesClusterMap = new ConcurrentHashMap<String, Set<Node>>();

    @Activate
    void activate(Map<String, ?> configuration) throws Exception {
        JMXUtils.registerMBean(this, mbeanServer, new ObjectName("io.fabric8.insight:type=Elasticsearch"));
    }

    @Deactivate
    void deactivate(Map<String, ?> configuration) throws Exception {
        JMXUtils.unregisterMBean(mbeanServer, new ObjectName("io.fabric8.insight:type=Elasticsearch"));
    }

    @Override
    public NodeInfo[] getNodeInfo(String clusterName) {
        Set<Node> nodeSet = nodesClusterMap.get(clusterName);
        if (nodeSet != null) {
            for (Node node : nodeSet) {
                ClusterAdminClient client = node.client().admin().cluster();
                NodesInfoResponse response = client.prepareNodesInfo().all().execute().actionGet();
                return response.getNodes();
            }
        }
        return null;
    }

    @Override
    public String getRestUrl(String clusterName) {
        NodeInfo[] nodes = getNodeInfo(clusterName);
        if (nodes != null && nodes.length > 0) {
            String publishAddress = nodes[0].getHttp().address().publishAddress().toString().substring(0);
            return publishAddress.substring(0, publishAddress.lastIndexOf(']')).replaceFirst("inet\\[.*\\/", "http://");
        }
        return null;
    }

    @Override
    public ClusterHealthResponse getClusterHealth(String clusterName) {
        Set<Node> nodeSet = nodesClusterMap.get(clusterName);
        if (nodeSet != null) {
            for (Node node : nodeSet) {
                ClusterAdminClient client = node.client().admin().cluster();
                ClusterHealthResponse response = client.prepareHealth().execute().actionGet();
                return response;
            }
        }
        return null;
    }

    public void bindNode(Node node) {
        String clusterName = node.settings().get("cluster.name");
        Set<Node> nodeSet = nodesClusterMap.get(clusterName);
        if (nodeSet == null) {
            nodeSet = new HashSet<Node>();
            nodesClusterMap.put(clusterName, nodeSet);
        }
        nodeSet.add(node);
    }

    public void unbindNode(Node node) {
        String clusterName = node.settings().get("cluster.name");
        Set<Node> nodeSet = nodesClusterMap.get(clusterName);
        if (nodeSet != null) {
            nodeSet.remove(node);
            if (nodeSet.isEmpty()) {
                nodesClusterMap.remove(clusterName);
            }
        }
    }
}