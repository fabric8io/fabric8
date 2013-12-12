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
package io.fabric8.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.codehaus.jackson.map.ObjectMapper;
import io.fabric8.boot.commands.support.FabricCommand;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.exists;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getAllChildren;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getChildren;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getSubstitutedData;

@Command(name = "cluster-list", scope = "fabric", description = "Lists all ActiveMQ message brokers in the fabric, enabling you to see which brokers are grouped into clusters.")
public class ClusterList extends FabricCommand {

    protected static String CLUSTER_PREFIX = "/fabric/registry/clusters";

    @Argument(required = false, description = "Path of the fabric registry node (Zookeeper registry node) to list. Relative paths are evaluated relative to the base node, /fabric/registry/clusters. If not specified, all clusters are listed.")
    String path = "";

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();

        String realPath = path;
        if (!realPath.startsWith("/")) {
            realPath = CLUSTER_PREFIX;
            if (path.length() > 0) {
                realPath += "/" + path;
            }
        }
        printCluster(realPath, System.out);
        return null;
    }

    protected void printCluster(String dir, PrintStream out) throws Exception {
        // do we have any clusters at all?
        if (exists(getCurator(), dir) == null) {
            return;
        }
        List<String> children = getAllChildren(getCurator(), dir);
        Map<String, Map<String,ClusterNode>> clusters = new TreeMap<String, Map<String,ClusterNode>>();
        for (String child : children) {
            byte[] data = getCurator().getData().forPath(child);
            if (data != null && data.length > 0) {
                String text = new String(data).trim();
                if (!text.isEmpty()) {
                    String clusterName = getClusterName(dir, child);
                    Map<String, ClusterNode> cluster = clusters.get(clusterName);
                    if (cluster == null) {
                        cluster = new TreeMap<String, ClusterNode>();
                        clusters.put(clusterName, cluster);
                    }

                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> map = mapper.readValue(data, HashMap.class);

                    ClusterNode node = null;

                    Object id = value(map, "id", "container");
                    if (id != null) {
                        Object agent = value(map, "container", "agent");
                        List services = (List) value(map, "services");

                        node = cluster.get(id);
                        if (node == null) {
                            node = new ClusterNode();
                            cluster.put(id.toString(), node);
                        }

                        if (services != null) {
                            if (!services.isEmpty()) {
                                for (Object service : services) {
                                    node.services.add(getSubstitutedData(getCurator(), service.toString()));
                                }

                                node.masters.add(agent);
                            } else {
                                node.slaves.add(agent);
                            }
                        } else {
                            node.slaves.add(agent);
                        }
                    }
                }
            }
        }

        out.println(String.format("%-30s %-30s %-30s %s", "[cluster]", "[masters]", "[slaves]", "[services]"));

        for (String clusterName : clusters.keySet()) {
            Map<String, ClusterNode> nodes = clusters.get(clusterName);
            out.println(String.format("%-30s %-30s %-30s %s", clusterName, "", "", "", ""));
            for (String nodeName : nodes.keySet()) {
                ClusterNode node = nodes.get(nodeName);
                out.println(String.format("%-30s %-30s %-30s %s",
                            "   "  + nodeName,
                            printList(node.masters),
                            printList(node.slaves),
                            printList(node.services)));
            }
        }
    }

    protected String printList(List list) {
        if (list.isEmpty()) {
            return "-";
        }
        String text = list.toString();
        return text.substring(1, text.length() - 1);
    }

    protected Object value(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    protected String getClusterName(String rootDir, String dir) {
        String clusterName = dir;
        clusterName = clusterName.substring(0, clusterName.lastIndexOf("/"));
        if (clusterName.startsWith(rootDir)) {
            clusterName = clusterName.substring(rootDir.length());
        }
        if (clusterName.startsWith("/")) {
            clusterName = clusterName.substring(1);
        }
        if (clusterName.length() == 0) {
            clusterName = ".";
        }
        return clusterName;
    }

    protected class ClusterNode {
        public List masters = new ArrayList();
        public List services = new ArrayList();
        public List slaves = new ArrayList();

        @Override
        public String toString() {
            return masters + " " + services + " " + slaves;
        }
    }

}
