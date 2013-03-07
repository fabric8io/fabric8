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
package org.elasticsearch.discovery.fabric;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.fusesource.fabric.groups.ClusteredSupport$;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NodeJsonTest {

    @Test
    public void testJson() throws Exception {
        Map<String, String> attr = new HashMap<String, String>();
        attr.put("key", "value");
        DiscoveryNode n = new DiscoveryNode("thename", "theid", new InetSocketTransportAddress("thehost", 3234), attr);
        FabricDiscovery.ESNode node = new FabricDiscovery.ESNode("thecluster", n, false);

        byte[] data = ClusteredSupport$.MODULE$.encode(node, ClusteredSupport$.MODULE$.DEFAULT_MAPPER());
        System.err.println(new String(data));
        FabricDiscovery.ESNode newNode = ClusteredSupport$.MODULE$.decode(FabricDiscovery.ESNode.class, data, ClusteredSupport$.MODULE$.DEFAULT_MAPPER());

        assertEquals(node.id(), newNode.id());
        assertEquals(node.node().id(), newNode.node().id());
        assertEquals(node.node().name(), newNode.node().name());
        assertEquals(node.node().address(), newNode.node().address());
        assertEquals(node.node().attributes(), newNode.node().attributes());
        assertEquals(node.node().version().toString(), newNode.node().version().toString());
    }

}
